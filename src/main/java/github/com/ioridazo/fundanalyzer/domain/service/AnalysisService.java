package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BsEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.PlEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.PlSubject;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatement;
import github.com.ioridazo.fundanalyzer.domain.util.Converter;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCalculateException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Log4j2
@Service
public class AnalysisService {

    private final IndustryDao industryDao;
    private final CompanyDao companyDao;
    private final BsSubjectDao bsSubjectDao;
    private final PlSubjectDao plSubjectDao;
    private final DocumentDao documentDao;
    private final FinancialStatementDao financialStatementDao;
    private final AnalysisResultDao analysisResultDao;

    public AnalysisService(
            IndustryDao industryDao,
            CompanyDao companyDao,
            BsSubjectDao bsSubjectDao,
            PlSubjectDao plSubjectDao,
            DocumentDao documentDao,
            FinancialStatementDao financialStatementDao,
            AnalysisResultDao analysisResultDao) {
        this.industryDao = industryDao;
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
    @Transactional
    public void analyze(final String documentId) {
        final var document = documentDao.selectByDocumentId(documentId);
        final var companyCode = Converter.toCompanyCode(document.getEdinetCode(), companyDao.selectAll()).orElseThrow();
        try {
            analysisResultDao.insert(new AnalysisResult(
                            null,
                            companyCode,
                            document.getPeriod(),
                            calculate(companyCode, document.getPeriod()),
                            nowLocalDateTime()
                    )
            );
        } catch (FundanalyzerCalculateException ignored) {
            log.info("エラー発生により、企業価値を算出できませんでした。\t証券コード:{}", companyCode);
        }
    }

    /**
     * 対象書類の分析結果をデータベースに登録する
     *
     * @param submitDate 提出日
     */
    public CompletableFuture<Void> analyze(final LocalDate submitDate) {
        final var companyAll = companyDao.selectAll();
        final var bank = industryDao.selectByName("銀行業");
        final var insurance = industryDao.selectByName("保険業");

        documentDao.selectByTypeAndSubmitDate("120", submitDate).stream()
                // target company code
                .filter(document -> companyAll.stream()
                        .filter(company -> document.getEdinetCode().equals(company.getEdinetCode()))
                        .filter(company -> company.getCode().isPresent())
                        // 銀行業、保険業は対象外とする
                        .filter(company -> !bank.getId().equals(company.getIndustryId()))
                        .anyMatch(company -> !insurance.getId().equals(company.getIndustryId()))
                )
                // only not analyze
                .filter(document -> analysisResultDao.selectByUniqueKey(
                        Converter.toCompanyCode(document.getEdinetCode(), companyAll).orElseThrow(), document.getPeriod()
                        ).isEmpty()
                )
                .forEach(document -> analyze(document.getDocumentId()));

        log.info("すべての企業分析が正常に終了しました。\t対象提出日:{}", submitDate);
        return null;
    }

    /**
     * 企業価値を算出する
     *
     * @param companyCode 企業コード
     * @param period      対象年
     * @return 企業価値
     * @throws FundanalyzerCalculateException 算出に失敗したとき
     */
    BigDecimal calculate(final String companyCode, final LocalDate period) {
        final var company = companyDao.selectByCode(companyCode).orElseThrow();

        // 流動資産合計
        final var totalCurrentAssets = bsValues(company, BsEnum.TOTAL_CURRENT_ASSETS, period);
        // 投資その他の資産合計
        final var totalInvestmentsAndOtherAssets = bsValues(company, BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS, period);
        // 流動負債合計
        final var totalCurrentLiabilities = bsValues(company, BsEnum.TOTAL_CURRENT_LIABILITIES, period);
        // 固定負債合計
        final var totalFixedLiabilities = bsValues(company, BsEnum.TOTAL_FIXED_LIABILITIES, period);
        // 営業利益
        final var operatingProfit = plValues(company, PlEnum.OPERATING_PROFIT, period);
        // 株式総数
        final var numberOfShares = nsValue(company, period);

        return BigDecimal.valueOf(
                (
                        operatingProfit * 10
                                + totalCurrentAssets - (totalCurrentLiabilities * 1.2) + totalInvestmentsAndOtherAssets
                                - totalFixedLiabilities
                )
                        / numberOfShares
        );
    }

    /**
     * 貸借対照表の値を取得する
     *
     * @param company 会社情報
     * @param bsEnum  貸借対照表の対象科目
     * @param period  対象年
     * @return 科目の値
     */
    public Long bsValues(final Company company, final BsEnum bsEnum, final LocalDate period) {
        return bsSubjectDao.selectByOutlineSubjectId(bsEnum.getOutlineSubjectId()).stream()
                .sorted(Comparator.comparing(BsSubject::getDetailSubjectId))
                .map(bsSubject -> financialStatementDao.selectByUniqueKey(
                        company.getEdinetCode(),
                        FinancialStatementEnum.BALANCE_SHEET.toValue(),
                        bsSubject.getId(),
                        String.valueOf(period.getYear())
                        ).flatMap(FinancialStatement::getValue)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny()
                .orElseThrow(() -> {
                    final var docId = documentDao.selectDocumentIdBy(
                            Converter.toEdinetCode(company.getCode().orElseThrow(), companyDao.selectAll()).orElseThrow(),
                            "120",
                            String.valueOf(period.getYear())
                    ).getDocumentId();
                    documentDao.update(Document.builder()
                            .documentId(docId)
                            .scrapedBs(DocumentStatus.HALF_WAY.toValue())
                            .updatedAt(nowLocalDateTime())
                            .build()
                    );
                    log.warn("貸借対照表の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                                    "\t会社コード:{}\t科目名:{}\t対象年:{}\n書類パス:{}",
                            company.getCode().orElseThrow(),
                            bsEnum.getSubject(),
                            period,
                            documentDao.selectByDocumentId(docId).getBsDocumentPath()
                    );
                    throw new FundanalyzerCalculateException();
                });
    }

    /**
     * 損益計算書の値を取得する
     *
     * @param company 会社情報
     * @param plEnum  損益計算書の対象科目
     * @param period  対象年
     * @return 科目の値
     */
    public Long plValues(final Company company, final PlEnum plEnum, final LocalDate period) {
        return plSubjectDao.selectByOutlineSubjectId(plEnum.getOutlineSubjectId()).stream()
                .sorted(Comparator.comparing(PlSubject::getDetailSubjectId))
                .map(plSubject -> financialStatementDao.selectByUniqueKey(
                        company.getEdinetCode(),
                        FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.toValue(),
                        plSubject.getId(),
                        String.valueOf(period.getYear())
                        ).flatMap(FinancialStatement::getValue)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny()
                .orElseThrow(() -> {
                    final var docId = documentDao.selectDocumentIdBy(
                            Converter.toEdinetCode(company.getCode().orElseThrow(), companyDao.selectAll()).orElseThrow(),
                            "120",
                            String.valueOf(period.getYear())
                    ).getDocumentId();
                    documentDao.update(Document.builder()
                            .documentId(docId)
                            .scrapedPl(DocumentStatus.HALF_WAY.toValue())
                            .updatedAt(nowLocalDateTime())
                            .build()
                    );
                    log.warn("損益計算書の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                                    "\t会社コード:{}\t科目名:{}\t対象年:{}\n書類パス:{}",
                            company.getCode().orElseThrow(),
                            plEnum.getSubject(),
                            period,
                            documentDao.selectByDocumentId(docId).getPlDocumentPath()
                    );
                    throw new FundanalyzerCalculateException();
                });
    }

    /**
     * 株式総数の値を取得する
     *
     * @param company 会社情報
     * @param period  対象年
     * @return 株式総数の値
     */
    public Long nsValue(final Company company, final LocalDate period) {
        return financialStatementDao.selectByUniqueKey(
                company.getEdinetCode(),
                FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.toValue(),
                "0",
                String.valueOf(period.getYear())
        ).flatMap(FinancialStatement::getValue)
                .orElseThrow(() -> {
                    final var docId = documentDao.selectDocumentIdBy(
                            Converter.toEdinetCode(company.getCode().orElseThrow(), companyDao.selectAll()).orElseThrow(),
                            "120",
                            String.valueOf(period.getYear())
                    ).getDocumentId();
                    documentDao.update(Document.builder()
                            .documentId(docId)
                            .scrapedNumberOfShares(DocumentStatus.HALF_WAY.toValue())
                            .updatedAt(nowLocalDateTime())
                            .build()
                    );
                    log.warn("  株式総数の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                                    "\t会社コード:{}\t科目名:{}\t対象年:{}\n書類パス:{}",
                            company.getCode().orElseThrow(),
                            "株式総数",
                            period,
                            documentDao.selectByDocumentId(docId).getNumberOfSharesDocumentPath()
                    );
                    throw new FundanalyzerCalculateException();
                });
    }
}
