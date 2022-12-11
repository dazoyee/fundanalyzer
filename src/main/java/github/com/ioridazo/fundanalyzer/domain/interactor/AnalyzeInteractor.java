package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.InvestmentIndicatorSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.AnalyzeUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.value.AverageInfo;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.CorporateValue;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class AnalyzeInteractor implements AnalyzeUseCase {

    private static final Logger log = LogManager.getLogger(AnalyzeInteractor.class);

    private final CompanySpecification companySpecification;
    private final DocumentSpecification documentSpecification;
    private final FinancialStatementSpecification financialStatementSpecification;
    private final AnalysisResultSpecification analysisResultSpecification;
    private final StockSpecification stockSpecification;
    private final InvestmentIndicatorSpecification investmentIndicatorSpecification;

    @Value("${app.config.view.document-type-code}")
    List<String> targetTypeCodes;

    public AnalyzeInteractor(
            final CompanySpecification companySpecification,
            final DocumentSpecification documentSpecification,
            final FinancialStatementSpecification financialStatementSpecification,
            final AnalysisResultSpecification analysisResultSpecification,
            final StockSpecification stockSpecification,
            final InvestmentIndicatorSpecification investmentIndicatorSpecification) {
        this.companySpecification = companySpecification;
        this.documentSpecification = documentSpecification;
        this.financialStatementSpecification = financialStatementSpecification;
        this.analysisResultSpecification = analysisResultSpecification;
        this.stockSpecification = stockSpecification;
        this.investmentIndicatorSpecification = investmentIndicatorSpecification;
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
                inputData.getId(),
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
            if (targetList.size() > 10) {
                targetList.parallelStream().forEach(this::analyze);
            } else {
                targetList.forEach(this::analyze);
            }

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
        try {
            final FinanceValue financeValue = financialStatementSpecification.getFinanceValue(document);
            final AnalysisResult analysisResult = new AnalysisResult(financeValue, document);

            analysisResultSpecification.insert(document, analysisResult);
            analysisResultSpecification.findAnalysisResult(document.getDocumentId()).ifPresent(this::indicate);

        } catch (final FundanalyzerNotExistException e) {
            final FinancialStatementEnum fs = e.getFs().orElseThrow(FundanalyzerRuntimeException::new);
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "{0}の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                                    "\t会社コード:{1}\t書類ID:{2}\t科目名:{3}\t対象年:{4}\n書類パス:{5}",
                            fs.getName(),
                            companySpecification.findCompanyByEdinetCode(document.getEdinetCode()).map(Company::getCode).orElse("null"),
                            document.getDocumentId(),
                            e.getSubjectName().orElse("null"),
                            document.getDocumentPeriod().map(String::valueOf).orElse("null"),
                            document.getFsDocumentPath(fs).orElse("null")
                    ),
                    document,
                    Category.ANALYSIS,
                    Process.of(fs)
            ));

            // ステータスをHALF_WAY（途中）に更新する
            documentSpecification.updateFsToHalfWay(document, fs);
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
        final Optional<BigDecimal> latestCorporateValue =
                analysisResultSpecification.findLatestAnalysisResult(company.getCode())
                        .map(AnalysisResultEntity::getCorporateValue);
        if (latestCorporateValue.isEmpty()) {
            return corporateValue;
        } else {
            corporateValue.setLatestCorporateValue(latestCorporateValue.get());
        }

        // 平均企業価値
        final List<AverageInfo> averageInfoList = new ArrayList<>();
        List.of(
                AverageInfo.Year.THREE,
                AverageInfo.Year.FIVE,
                AverageInfo.Year.TEN,
                AverageInfo.Year.ALL
        ).forEach(year -> {
            final AverageInfo averageInfo = new AverageInfo();
            averageInfo.setYear(year);

            Optional.of(year)
                    .flatMap(y -> {
                        // 平均企業価値
                        if (AverageInfo.Year.ALL.equals(year)) {
                            return analysisResultSpecification.allYearAverageCorporateValue(company);
                        } else {
                            return analysisResultSpecification.yearAverageCorporateValue(company, AverageInfo.parseYear(y));
                        }
                    }).ifPresent(ave -> {
                        averageInfo.setAverageCorporateValue(ave);

                        // 標準偏差
                        final Optional<BigDecimal> sd = analysisResultSpecification.standardDeviation(company, ave);
                        if (sd.isPresent()) {
                            averageInfo.setStandardDeviation(sd.get());

                            // 変動係数
                            analysisResultSpecification.coefficientOfVariation(sd.get(), ave)
                                    .ifPresent(averageInfo::setCoefficientOfVariation);
                        }
                    });

            averageInfoList.add(averageInfo);
        });

        corporateValue.setAverageInfoList(averageInfoList);

        // 対象年カウント
        final BigDecimal countYear = analysisResultSpecification.countYear(company);
        corporateValue.setCountYear(countYear);

        return corporateValue;
    }

    /**
     * 投資指標を算出する
     *
     * @param inputData 企業コード
     */
    @Override
    public void indicate(final CodeInputData inputData) {
        analysisResultSpecification.findLatestAnalysisResult(inputData.getCode()).ifPresent(this::indicate);
    }

    /**
     * 投資指標を算出する
     *
     * @param analysisResult 分析結果
     */
    void indicate(final AnalysisResultEntity analysisResult) {
        if (targetTypeCodes.stream().noneMatch(target -> analysisResult.getDocumentTypeCode().equals(target))) {
            // 120,130 以外は処理対象外
            return;
        }

        final long startTime = System.currentTimeMillis();
        final List<IndicatorValue> indicatorValueList = investmentIndicatorSpecification.findIndicatorValueList(analysisResult.getId());
        final Optional<StockPriceEntity> latestStock = stockSpecification.findLatestStock(analysisResult.getCompanyCode());

        if (latestStock.isPresent()) {
            final LocalDate executedDate = indicatorValueList.stream()
                    // latest indicator
                    .max(Comparator.comparing(IndicatorValue::getTargetDate))
                    .map(IndicatorValue::getTargetDate)
                    // default
                    .orElse(analysisResult.getSubmitDate().minusDays(1));
            final LocalDate latestDate = latestStock.get().getTargetDate();

            // 提出日 <= 株価取得日 && 株価取得日 <= 提出日+1年
            if (
                    (latestDate.isEqual(analysisResult.getSubmitDate())
                            || latestDate.isAfter(analysisResult.getSubmitDate()))
                            &&
                            (latestDate.isEqual(analysisResult.getSubmitDate().plusYears(1))
                                    || (latestDate.isBefore(analysisResult.getSubmitDate().plusYears(1))))
            ) {
                // executedDate -> latestDate
                executedDate.plusDays(1).datesUntil(latestDate.plusDays(1)).forEach(date ->
                        // find stock
                        stockSpecification.findStock(analysisResult.getCompanyCode(), date)
                                .ifPresent(spe -> {
                                            if (spe.getStockPrice().isPresent()) {
                                                // indicate
                                                investmentIndicatorSpecification.insert(analysisResult, spe);
                                            }
                                        }
                                )
                );

                log.debug(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "投資指標を算出しました。\t企業コード:{0}\t処理対象日:{1} -> {2}",
                                analysisResult.getCompanyCode(),
                                executedDate.plusDays(1),
                                latestDate
                        ),
                        analysisResult.getDocumentId(),
                        Category.ANALYSIS,
                        Process.INDICATE,
                        System.currentTimeMillis() - startTime
                ));
            } else {
                log.warn(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "提出日または株価取得日が正しくないため、投資指標を算出しませんでした。" +
                                        "\t企業コード:{0}\t提出日:{1}\t株価取得日:{2}",
                                analysisResult.getCompanyCode(),
                                analysisResult.getSubmitDate(),
                                latestDate
                        ),
                        analysisResult.getDocumentId(),
                        Category.ANALYSIS,
                        Process.INDICATE,
                        System.currentTimeMillis() - startTime
                ));
            }
        } else {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "株価が存在しないため、投資指標を算出できませんでした。\t企業コード:{0}",
                            analysisResult.getCompanyCode()
                    ),
                    analysisResult.getDocumentId(),
                    Category.ANALYSIS,
                    Process.INDICATE,
                    System.currentTimeMillis() - startTime
            ));
        }
    }
}
