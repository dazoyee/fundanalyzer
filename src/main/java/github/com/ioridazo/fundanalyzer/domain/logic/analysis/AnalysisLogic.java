package github.com.ioridazo.fundanalyzer.domain.logic.analysis;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BsEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.PlEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.PlSubject;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatement;
import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.util.Converter;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCalculateException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.seasar.doma.jdbc.NonUniqueResultException;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;

@Log4j2
@Component
public class AnalysisLogic {

    private static final int WEIGHTING_BUSINESS_VALUE = 10;
    private static final double AVERAGE_CURRENT_RATIO = 1.2;

    private final CompanyDao companyDao;
    private final BsSubjectDao bsSubjectDao;
    private final PlSubjectDao plSubjectDao;
    private final DocumentDao documentDao;
    private final FinancialStatementDao financialStatementDao;
    private final AnalysisResultDao analysisResultDao;

    public AnalysisLogic(
            final CompanyDao companyDao,
            final BsSubjectDao bsSubjectDao,
            final PlSubjectDao plSubjectDao,
            final DocumentDao documentDao,
            final FinancialStatementDao financialStatementDao,
            final AnalysisResultDao analysisResultDao) {
        this.companyDao = companyDao;
        this.bsSubjectDao = bsSubjectDao;
        this.plSubjectDao = plSubjectDao;
        this.documentDao = documentDao;
        this.financialStatementDao = financialStatementDao;
        this.analysisResultDao = analysisResultDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 対象書類の分析結果をデータベースに登録する
     *
     * @param documentId 書類ID
     */
    @NewSpan("AnalysisLogic.analyze")
    @Transactional
    public void analyze(final String documentId) {
        final var document = documentDao.selectByDocumentId(documentId);
        final var companyCode = Converter.toCompanyCode(document.getEdinetCode(), companyDao.selectAll()).orElseThrow();
        try {
            analysisResultDao.insert(AnalysisResult.of(
                    companyCode,
                    document.getDocumentPeriod(),
                    calculate(companyCode, document),
                    DocumentTypeCode.fromValue(document.getDocumentTypeCode()),
                    document.getSubmitDate(),
                    documentId,
                    nowLocalDateTime()
            ));
        } catch (FundanalyzerCalculateException ignored) {
            FundanalyzerLogClient.logLogic(
                    MessageFormat.format("エラー発生により、企業価値を算出できませんでした。\t証券コード:{0}", companyCode),
                    Category.DOCUMENT,
                    Process.ANALYSIS
            );
        } catch (NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.debug("一意制約違反のため、データベースへの登録をスキップします。\tテーブル名:{}\t会社コード:{}\t期間:{}",
                        "analysis_result",
                        companyCode,
                        document.getDocumentPeriod()
                );
            } else {
                throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
            }
        }
    }

    /**
     * 企業価値を算出する
     *
     * @param companyCode 企業コード
     * @param document    ドキュメント
     * @return 企業価値
     */
    BigDecimal calculate(final String companyCode, final Document document) {
        final var company = companyDao.selectByCode(companyCode).orElseThrow();
        final FsValueParameter parameter = FsValueParameter.of(
                company, document.getDocumentPeriod(), DocumentTypeCode.fromValue(document.getDocumentTypeCode()), document.getSubmitDate());

        // 流動資産合計
        final long totalCurrentAssets = bsValue(BsEnum.TOTAL_CURRENT_ASSETS, parameter);
        // 投資その他の資産合計
        final long totalInvestmentsAndOtherAssets = bsValue(BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS, parameter);
        // 流動負債合計
        final long totalCurrentLiabilities = bsValue(BsEnum.TOTAL_CURRENT_LIABILITIES, parameter);
        // 固定負債合計
        final long totalFixedLiabilities = bsValue(BsEnum.TOTAL_FIXED_LIABILITIES, parameter);
        // 営業利益
        final long operatingProfit = plValue(PlEnum.OPERATING_PROFIT, parameter);
        // 株式総数
        final long numberOfShares = nsValue(parameter);

        return BigDecimal.valueOf(
                (
                        operatingProfit * WEIGHTING_BUSINESS_VALUE
                                + totalCurrentAssets - (totalCurrentLiabilities * AVERAGE_CURRENT_RATIO) + totalInvestmentsAndOtherAssets
                                - totalFixedLiabilities
                )
                        / numberOfShares
        );
    }

    /**
     * 貸借対照表の値を取得する
     *
     * @param bsEnum    貸借対照表の対象科目
     * @param parameter FsValueParameter.class
     * @return 科目の値
     */
    @NewSpan("AnalysisLogic.bsValue")
    public Long bsValue(final BsEnum bsEnum, final FsValueParameter parameter) {
        return bsSubjectDao.selectByOutlineSubjectId(bsEnum.getOutlineSubjectId()).stream()
                .sorted(Comparator.comparing(BsSubject::getDetailSubjectId))
                .map(bsSubject -> financialStatementDao.selectByUniqueKey(
                        parameter.getCompany().getEdinetCode(),
                        FinancialStatementEnum.BALANCE_SHEET.toValue(),
                        bsSubject.getId(),
                        String.valueOf(parameter.getPeriod().getYear()),
                        parameter.getDocumentTypeCode().toValue(),
                        parameter.getSubmitDate()
                        ).flatMap(FinancialStatement::getValue)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny()
                .orElseThrow(() -> fsValueThrow(
                        "貸借対照表",
                        parameter,
                        bsEnum.getSubject(),
                        document -> {
                            if (DocumentStatus.DONE.equals(DocumentStatus.fromValue(document.getScrapedBs()))) {
                                documentDao.update(Document.ofUpdateBsToHalfWay(document.getDocumentId(), nowLocalDateTime()));
                            }
                        }));
    }

    /**
     * 損益計算書の値を取得する
     *
     * @param plEnum    損益計算書の対象科目
     * @param parameter FsValueParameter.class
     * @ 科目の値
     */
    @NewSpan("AnalysisLogic.plValue")
    public Long plValue(final PlEnum plEnum, final FsValueParameter parameter) {
        return plSubjectDao.selectByOutlineSubjectId(plEnum.getOutlineSubjectId()).stream()
                .sorted(Comparator.comparing(PlSubject::getDetailSubjectId))
                .map(plSubject -> financialStatementDao.selectByUniqueKey(
                        parameter.getCompany().getEdinetCode(),
                        FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.toValue(),
                        plSubject.getId(),
                        String.valueOf(parameter.getPeriod().getYear()),
                        parameter.getDocumentTypeCode().toValue(),
                        parameter.getSubmitDate()
                        ).flatMap(FinancialStatement::getValue)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny()
                .orElseThrow(() -> fsValueThrow(
                        "損益計算書",
                        parameter,
                        plEnum.getSubject(),
                        document -> {
                            if (DocumentStatus.DONE.equals(DocumentStatus.fromValue(document.getScrapedPl()))) {
                                documentDao.update(Document.ofUpdatePlToHalfWay(document.getDocumentId(), nowLocalDateTime()));
                            }
                        }));
    }

    /**
     * 株式総数の値を取得する
     *
     * @param parameter FsValueParameter.class
     * @return 株式総数の値
     */
    @NewSpan("AnalysisLogic.nsValue")
    public Long nsValue(final FsValueParameter parameter) {
        return financialStatementDao.selectByUniqueKey(
                parameter.getCompany().getEdinetCode(),
                FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.toValue(),
                "0",
                String.valueOf(parameter.getPeriod().getYear()),
                parameter.getDocumentTypeCode().toValue(),
                parameter.getSubmitDate()
        )
                .flatMap(FinancialStatement::getValue)
                .orElseThrow(() -> fsValueThrow(
                        "  株式総数",
                        parameter,
                        "株式総数",
                        document -> {
                            if (DocumentStatus.DONE.equals(DocumentStatus.fromValue(document.getScrapedNumberOfShares()))) {
                                documentDao.update(Document.ofUpdateNumberOfSharesToHalfWay(document.getDocumentId(), nowLocalDateTime()));
                            }
                        }));
    }

    /**
     * 財務諸表の値が取得できないときに、ステータスを更新する
     *
     * @param fsName         財務諸表の名前
     * @param parameter      FsValueParameter.class
     * @param subjectName    財務諸表の科目名
     * @param updateDocument ドキュメントステータス更新処理
     * @return FundanalyzerCalculateException
     */
    private FundanalyzerCalculateException fsValueThrow(
            final String fsName,
            final FsValueParameter parameter,
            final String subjectName,
            final Consumer<Document> updateDocument) {
        try {
            final Document document = documentDao.selectDocumentBy(
                    Converter.toEdinetCode(parameter.getCompany().getCode().orElseThrow(), companyDao.selectAll()).orElseThrow(),
                    parameter.getDocumentTypeCode().toValue(),
                    parameter.getSubmitDate(),
                    String.valueOf(parameter.getPeriod().getYear())
            );

            // ステータスをHALF_WAY（途中）に更新する
            updateDocument.accept(document);

            log.warn(
                    "{}の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                            "\t会社コード:{}\t科目名:{}\t対象年:{}\n書類パス:{}",
                    fsName,
                    parameter.getCompany().getCode().orElseThrow(),
                    subjectName,
                    parameter.getPeriod(),
                    document.getPlDocumentPath()
            );

        } catch (NestedRuntimeException e) {
            log.warn(
                    "{}の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                            "\t会社コード:{}\t科目名:{}\t対象年:{}",
                    fsName,
                    parameter.getCompany().getCode().orElseThrow(),
                    subjectName,
                    parameter.getPeriod()
            );

            if (e.contains(NonUniqueResultException.class)) {
                log.warn(
                        "期待値1件に対し、複数のドキュメントが見つかりました。次の項目を確認してください。" +
                                "\t会社コード:{}\t書類種別コード:{}\t提出日:{}\t対象年:{}",
                        parameter.getCompany().getCode().orElseThrow(),
                        parameter.getDocumentTypeCode().toValue(),
                        parameter.getSubmitDate(),
                        String.valueOf(parameter.getPeriod().getYear())

                );
            } else {
                log.error("想定外のエラーが発生しました。", e);
            }
        }

        throw new FundanalyzerCalculateException("財務諸表の値を取得することができませんでした。");
    }

    @SuppressWarnings("RedundantModifiersValueLombok")
    @Value(staticConstructor = "of")
    public static class FsValueParameter {
        private final Company company;
        private final LocalDate period;
        private final DocumentTypeCode documentTypeCode;
        private final LocalDate submitDate;
    }
}
