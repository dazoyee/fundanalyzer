package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.AnalyzeUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.CorporateValue;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import github.com.ioridazo.fundanalyzer.domain.value.PlSubject;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

@Component
public class AnalyzeInteractor implements AnalyzeUseCase {

    private static final Logger log = LogManager.getLogger(AnalyzeInteractor.class);
    private static final int WEIGHTING_BUSINESS_VALUE = 10;
    private static final double AVERAGE_CURRENT_RATIO = 1.2;
    private static final int WEIGHTING_QUARTER_VALUE = 4;

    private final CompanySpecification companySpecification;
    private final DocumentSpecification documentSpecification;
    private final FinancialStatementSpecification financialStatementSpecification;
    private final AnalysisResultSpecification analysisResultSpecification;

    public AnalyzeInteractor(
            final CompanySpecification companySpecification,
            final DocumentSpecification documentSpecification,
            final FinancialStatementSpecification financialStatementSpecification,
            final AnalysisResultSpecification analysisResultSpecification) {
        this.companySpecification = companySpecification;
        this.documentSpecification = documentSpecification;
        this.financialStatementSpecification = financialStatementSpecification;
        this.analysisResultSpecification = analysisResultSpecification;
    }

    /**
     * 企業価値を算出する
     *
     * @param inputData 書類ID
     */
    @Override
    public void analyze(final IdInputData inputData) {
        final long startTime = System.currentTimeMillis();
        this.analyze(documentSpecification.findDocument(inputData));

        log.info(FundanalyzerLogClient.toInteractorLogObject(
                MessageFormat.format("書類ID[{0}]の分析が正常に終了しました。", inputData.getId()),
                Category.ANALYSIS,
                Process.ANALYSIS,
                System.currentTimeMillis() - startTime
        ));
    }

    /**
     * 企業価値を算出する
     *
     * @param inputData 提出日
     */
    @Override
    public void analyze(DateInputData inputData) {
        final long startTime = System.currentTimeMillis();
        final List<Document> targetList = documentSpecification.analysisTargetList(inputData);

        if (targetList.isEmpty()) {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "次の提出日に関する書類は分析済みかまたはデータベースに存在しませんでした。\t対象提出日:{0}",
                            inputData.getDate()
                    ),
                    Category.ANALYSIS,
                    Process.ANALYSIS,
                    System.currentTimeMillis() - startTime
            ));
        } else {
            targetList.forEach(this::analyze);

            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "次の提出日に関する書類に対して分析を正常に終了しました。\t対象提出日:{0}",
                            inputData.getDate()
                    ),
                    Category.ANALYSIS,
                    Process.ANALYSIS,
                    System.currentTimeMillis() - startTime
            ));
        }
    }

    /**
     * 企業価値を算出する
     *
     * @param document ドキュメント
     */
    void analyze(final Document document) {
        final String companyCode = companySpecification.findCompanyByEdinetCode(document.getEdinetCode())
                .flatMap(Company::getCode)
                .orElseThrow(FundanalyzerNotExistException::new);

        try {
            analysisResultSpecification.insert(document, calculateFsValue(document));
        } catch (FundanalyzerNotExistException ignored) {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "エラー発生により、企業価値を算出できませんでした。\t証券コード:{0}\t書類ID:{1}",
                            companyCode,
                            document.getDocumentId()
                    ),
                    Category.ANALYSIS,
                    Process.ANALYSIS
            ));
        }
    }

    /**
     * 企業価値情報を取得する
     *
     * @param company 企業情報
     * @return 企業価値
     */
    @Override
    public CorporateValue calculateCorporateValue(final Company company) {
        final CorporateValue corporateValue = CorporateValue.of();

        // 最新企業価値
        final Optional<BigDecimal> latestCorporateValue = analysisResultSpecification.latestCorporateValue(company);
        if (latestCorporateValue.isEmpty()) {
            return corporateValue;
        } else {
            corporateValue.setLatestCorporateValue(latestCorporateValue.get());
        }
        // 平均企業価値
        final Optional<BigDecimal> averageCorporateValue = analysisResultSpecification.averageCorporateValue(company);
        if (averageCorporateValue.isEmpty()) {
            return corporateValue;
        } else {
            corporateValue.setAverageCorporateValue(averageCorporateValue.get());
        }
        // 標準偏差
        final Optional<BigDecimal> standardDeviation = analysisResultSpecification.standardDeviation(company, averageCorporateValue.get());
        if (standardDeviation.isEmpty()) {
            return corporateValue;
        } else {
            corporateValue.setStandardDeviation(standardDeviation.get());
        }
        // 変動係数
        final Optional<BigDecimal> coefficientOfVariation = analysisResultSpecification.coefficientOfVariation(standardDeviation.get(), averageCorporateValue.get());
        if (coefficientOfVariation.isEmpty()) {
            return corporateValue;
        } else {
            corporateValue.setCoefficientOfVariation(coefficientOfVariation.get());
        }
        // 対象年カウント
        final BigDecimal countYear = analysisResultSpecification.countYear(company);
        corporateValue.setCountYear(countYear);

        return corporateValue;
    }

    /**
     * 企業価値の算出する
     *
     * @param document ドキュメント
     * @return 企業価値
     * @throws FundanalyzerNotExistException 値が存在しないとき
     */
    BigDecimal calculateFsValue(final Document document) throws FundanalyzerNotExistException {
        final FinanceValue financeValue = financialStatementSpecification.getFinanceValue(document);
        // 流動資産合計
        final Long totalCurrentAssets = financeValue.getTotalCurrentAssets().orElseThrow(() -> fsValueThrow(
                FinancialStatementEnum.BALANCE_SHEET,
                BsSubject.BsEnum.TOTAL_CURRENT_ASSETS.getSubject(),
                document
        ));
        // 投資その他の資産合計
        final Long totalInvestmentsAndOtherAssets = financeValue.getTotalInvestmentsAndOtherAssets().orElseThrow(() -> fsValueThrow(
                FinancialStatementEnum.BALANCE_SHEET,
                BsSubject.BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS.getSubject(),
                document
        ));
        // 流動負債合計
        final Long totalCurrentLiabilities = financeValue.getTotalCurrentLiabilities().orElseThrow(() -> fsValueThrow(
                FinancialStatementEnum.BALANCE_SHEET,
                BsSubject.BsEnum.TOTAL_CURRENT_LIABILITIES.getSubject(),
                document
        ));
        // 固定負債合計
        final Long totalFixedLiabilities = financeValue.getTotalFixedLiabilities().orElseThrow(() -> fsValueThrow(
                FinancialStatementEnum.BALANCE_SHEET,
                BsSubject.BsEnum.TOTAL_FIXED_LIABILITIES.getSubject(),
                document
        ));
        // 営業利益
        final Long operatingProfit = financeValue.getOperatingProfit().orElseThrow(() -> fsValueThrow(
                FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                PlSubject.PlEnum.OPERATING_PROFIT.getSubject(),
                document
        ));
        // 四半期種別の重みづけ
        final Integer weightingQuarterType = Optional.of(document)
                .map(Document::getQuarterType)
                .map(QuarterType::getWeight)
                .orElse(WEIGHTING_QUARTER_VALUE);
        // 株式総数
        final Long numberOfShares = financeValue.getNumberOfShares()
                .orElseThrow(() -> fsValueThrow(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES, "株式総数", document));

        return BigDecimal.valueOf(
                (
                        (
                                (
                                        operatingProfit * WEIGHTING_BUSINESS_VALUE
                                                + totalCurrentAssets - (totalCurrentLiabilities * AVERAGE_CURRENT_RATIO) + totalInvestmentsAndOtherAssets
                                                - totalFixedLiabilities
                                )
                                        / weightingQuarterType
                        ) * WEIGHTING_QUARTER_VALUE
                )
                        / numberOfShares
        );
    }

    /**
     * 処理状況を処理途中に更新する
     *
     * @param fs          財務諸表種別
     * @param subjectName 科目名
     * @param document    ドキュメント
     * @return FundanalyzerNotExistException
     */
    private FundanalyzerNotExistException fsValueThrow(
            final FinancialStatementEnum fs, final String subjectName, final Document document) {
        log.warn(FundanalyzerLogClient.toInteractorLogObject(
                MessageFormat.format(
                        "{0}の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                                "\t会社コード:{1}\t書類ID:{2}\t科目名:{3}\t対象年:{4}\n書類パス:{5}",
                        fs.getName(),
                        companySpecification.findCompanyByEdinetCode(document.getEdinetCode()).flatMap(Company::getCode).orElse("null"),
                        document.getDocumentId(),
                        subjectName,
                        document.getDocumentPeriod().map(String::valueOf).orElse("null"),
                        document.getFsDocumentPath(fs).orElse("null")
                ),
                Category.ANALYSIS,
                Process.of(fs)
        ));

        // ステータスをHALF_WAY（途中）に更新する
        documentSpecification.updateFsToHalfWay(document, fs);

        throw new FundanalyzerNotExistException(fs.getName(), subjectName);
    }
}
