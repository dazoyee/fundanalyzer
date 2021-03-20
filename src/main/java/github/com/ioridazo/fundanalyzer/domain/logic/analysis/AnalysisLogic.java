package github.com.ioridazo.fundanalyzer.domain.logic.analysis;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BsEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.DocTypeCode;
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
                    document.getPeriod(),
                    calculate(companyCode, document.getPeriod(), DocTypeCode.fromValue(document.getDocumentTypeCode())),
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
                        document.getPeriod()
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
     * @param period      対象年
     * @return 企業価値
     * @throws FundanalyzerCalculateException 算出に失敗したとき
     */
    BigDecimal calculate(final String companyCode, final LocalDate period, final DocTypeCode docTypeCode) {
        final var company = companyDao.selectByCode(companyCode).orElseThrow();
        final FsValueParameter parameter = FsValueParameter.of(company, period, docTypeCode);

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
                        parameter.getDocTypeCode().toValue()
                        ).flatMap(FinancialStatement::getValue)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny()
                .orElseThrow(() -> {
                    final var docId = documentDao.selectDocumentIdBy(
                            Converter.toEdinetCode(parameter.getCompany().getCode().orElseThrow(), companyDao.selectAll()).orElseThrow(),
                            "120",
                            String.valueOf(parameter.getPeriod().getYear())
                    ).getDocumentId();

                    documentDao.update(Document.ofUpdateBsToHalfWay(docId, nowLocalDateTime()));

                    log.warn("貸借対照表の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                                    "\t会社コード:{}\t科目名:{}\t対象年:{}\n書類パス:{}",
                            parameter.getCompany().getCode().orElseThrow(),
                            bsEnum.getSubject(),
                            parameter.getPeriod(),
                            documentDao.selectByDocumentId(docId).getBsDocumentPath()
                    );
                    throw new FundanalyzerCalculateException();
                });
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
                        parameter.getDocTypeCode().toValue()
                        ).flatMap(FinancialStatement::getValue)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny()
                .orElseThrow(() -> {
                    final var docId = documentDao.selectDocumentIdBy(
                            Converter.toEdinetCode(parameter.getCompany().getCode().orElseThrow(), companyDao.selectAll()).orElseThrow(),
                            "120",
                            String.valueOf(parameter.getPeriod().getYear())
                    ).getDocumentId();

                    documentDao.update(Document.ofUpdatePlToHalfWay(docId, nowLocalDateTime()));

                    log.warn("損益計算書の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                                    "\t会社コード:{}\t科目名:{}\t対象年:{}\n書類パス:{}",
                            parameter.getCompany().getCode().orElseThrow(),
                            plEnum.getSubject(),
                            parameter.getPeriod(),
                            documentDao.selectByDocumentId(docId).getPlDocumentPath()
                    );
                    throw new FundanalyzerCalculateException();
                });
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
                parameter.getDocTypeCode().toValue()
        ).flatMap(FinancialStatement::getValue)
                .orElseThrow(() -> {
                    final var docId = documentDao.selectDocumentIdBy(
                            Converter.toEdinetCode(parameter.getCompany().getCode().orElseThrow(), companyDao.selectAll()).orElseThrow(),
                            "120",
                            String.valueOf(parameter.getPeriod().getYear())
                    ).getDocumentId();

                    documentDao.update(Document.ofUpdateNumberOfSharesToHalfWay(docId, nowLocalDateTime()));

                    log.warn("  株式総数の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                                    "\t会社コード:{}\t科目名:{}\t対象年:{}\n書類パス:{}",
                            parameter.getCompany().getCode().orElseThrow(),
                            "株式総数",
                            parameter.getPeriod(),
                            documentDao.selectByDocumentId(docId).getNumberOfSharesDocumentPath()
                    );
                    throw new FundanalyzerCalculateException();
                });
    }

    @SuppressWarnings("RedundantModifiersValueLombok")
    @Value(staticConstructor = "of")
    public static class FsValueParameter {
        private final Company company;
        private final LocalDate period;
        private final DocTypeCode docTypeCode;
    }
}
