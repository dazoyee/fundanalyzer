package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
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
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocument;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatement;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCalculateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static github.com.ioridazo.fundanalyzer.domain.ViewService.mapToPeriod;

@Slf4j
@Service
public class AnalysisService {

    private final IndustryDao industryDao;
    private final CompanyDao companyDao;
    private final BsSubjectDao bsSubjectDao;
    private final PlSubjectDao plSubjectDao;
    private final EdinetDocumentDao edinetDocumentDao;
    private final DocumentDao documentDao;
    private final FinancialStatementDao financialStatementDao;
    private final AnalysisResultDao analysisResultDao;

    public AnalysisService(
            IndustryDao industryDao,
            CompanyDao companyDao,
            BsSubjectDao bsSubjectDao,
            PlSubjectDao plSubjectDao,
            EdinetDocumentDao edinetDocumentDao,
            DocumentDao documentDao,
            FinancialStatementDao financialStatementDao,
            AnalysisResultDao analysisResultDao) {
        this.industryDao = industryDao;
        this.companyDao = companyDao;
        this.bsSubjectDao = bsSubjectDao;
        this.plSubjectDao = plSubjectDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.documentDao = documentDao;
        this.financialStatementDao = financialStatementDao;
        this.analysisResultDao = analysisResultDao;
    }

    public void analyze(final String documentId) {
        edinetDocumentDao.selectByDocId(documentId).getPeriodEnd()
                .ifPresent(d -> analyze(Integer.parseInt(d.substring(0, 4))));
    }

    public void analyze(final int year) {
        final var bank = industryDao.selectByName("銀行業");
        final var insurance = industryDao.selectByName("保険業");
        final var companyAll = companyDao.selectAll();
        var presentCompanies = new ArrayList<Company>();

        edinetDocumentDao.selectByDocTypeCodeAndPeriodEnd("120", String.valueOf(year)).stream()
                .map(EdinetDocument::getEdinetCode)
                .map(Optional::get)
                .forEach(edinetCode -> companyAll.stream()
                        .filter(company -> edinetCode.equals(company.getEdinetCode()))
                        .filter(company -> company.getCode().isPresent())
                        // 銀行業、保険業は対象外とする
                        .filter(company -> !bank.getId().equals(company.getIndustryId()))
                        .filter(company -> !insurance.getId().equals(company.getIndustryId()))
                        .findAny()
                        .ifPresent(presentCompanies::add));

        final var resultListAlready = analysisResultDao.selectByPeriod(mapToPeriod(year));
        presentCompanies.forEach(company -> {
            final var companyCode = company.getCode().orElseThrow();
            //noinspection StatementWithEmptyBody
            if (isAnalyzed(resultListAlready, companyCode)) {
                // 分析済
            } else {
                try {
                    analysisResultDao.insert(new AnalysisResult(
                                    null,
                                    companyCode,
                                    LocalDate.of(year, 1, 1),
                                    calculate(companyCode, year),
                                    LocalDateTime.now()
                            )
                    );
                } catch (FundanalyzerCalculateException ignored) {
                    log.info("エラー発生により、企業価値を算出できませんでした。\t証券コード:{}", companyCode);
                }
            }
        });
        log.info("すべての企業分析が正常に終了しました。\t対象月:{}", year);
    }

    BigDecimal calculate(final String companyCode, final int year) throws FundanalyzerCalculateException {
        final var company = companyDao.selectByCode(companyCode).orElseThrow();

        // 流動資産合計
        final var totalCurrentAssets = bsValues(company, BsEnum.TOTAL_CURRENT_ASSETS, year);
        // 投資その他の資産合計
        final var totalInvestmentsAndOtherAssets = bsValues(company, BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS, year);
        // 流動負債合計
        final var totalCurrentLiabilities = bsValues(company, BsEnum.TOTAL_CURRENT_LIABILITIES, year);
        // 固定負債合計
        final var totalFixedLiabilities = bsValues(company, BsEnum.TOTAL_FIXED_LIABILITIES, year);
        // 営業利益
        final var operatingProfit = plValues(company, PlEnum.OPERATING_PROFIT, year);
        // 株式総数
        final var numberOfShares = numberOfSharesValue(company, year);

        return BigDecimal.valueOf(
                (
                        operatingProfit * 10
                                + totalCurrentAssets - (totalCurrentLiabilities * 1.2) + totalInvestmentsAndOtherAssets
                                - totalFixedLiabilities
                )
                        / numberOfShares
        );
    }

    private Long bsValues(final Company company, final BsEnum bsEnum, final int year) {
        return bsSubjectDao.selectByOutlineSubjectId(bsEnum.getOutlineSubjectId()).stream()
                .sorted(Comparator.comparing(BsSubject::getDetailSubjectId))
                .map(bsSubject -> financialStatementDao.selectByUniqueKey(
                        company.getEdinetCode(),
                        FinancialStatementEnum.BALANCE_SHEET.toValue(),
                        bsSubject.getId(),
                        String.valueOf(year)
                        ).flatMap(FinancialStatement::getValue)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny()
                .orElseThrow(() -> {
                    final var docId = edinetDocumentDao.selectDocIdBy(
                            codeConverter(company.getCode().orElseThrow(), companyDao.selectAll()),
                            "120",
                            String.valueOf(year)
                    ).getDocId();
                    documentDao.update(Document.builder()
                            .documentId(docId)
                            .scrapedBs(DocumentStatus.HALF_WAY.toValue())
                            .updatedAt(LocalDateTime.now())
                            .build()
                    );
                    log.warn("貸借対照表の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                                    "\t会社コード:{}\t科目名:{}\t対象年:{}\n書類パス:{}",
                            company.getCode().orElseThrow(),
                            bsEnum.getSubject(),
                            year,
                            documentDao.selectByDocumentId(docId).getBsDocumentPath()
                    );
                    throw new FundanalyzerCalculateException();
                });
    }

    private Long plValues(final Company company, final PlEnum plEnum, final int year) {
        return plSubjectDao.selectByOutlineSubjectId(plEnum.getOutlineSubjectId()).stream()
                .sorted(Comparator.comparing(PlSubject::getDetailSubjectId))
                .map(plSubject -> financialStatementDao.selectByUniqueKey(
                        company.getEdinetCode(),
                        FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.toValue(),
                        plSubject.getId(),
                        String.valueOf(year)
                        ).flatMap(FinancialStatement::getValue)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny()
                .orElseThrow(() -> {
                    final var docId = edinetDocumentDao.selectDocIdBy(
                            codeConverter(company.getCode().orElseThrow(), companyDao.selectAll()),
                            "120",
                            String.valueOf(year)
                    ).getDocId();
                    documentDao.update(Document.builder()
                            .documentId(docId)
                            .scrapedPl(DocumentStatus.HALF_WAY.toValue())
                            .updatedAt(LocalDateTime.now())
                            .build()
                    );
                    log.warn("損益計算書の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                                    "\t会社コード:{}\t科目名:{}\t対象年:{}\n書類パス:{}",
                            company.getCode().orElseThrow(),
                            plEnum.getSubject(),
                            year,
                            documentDao.selectByDocumentId(docId).getPlDocumentPath()
                    );
                    throw new FundanalyzerCalculateException();
                });
    }

    private Long numberOfSharesValue(final Company company, final int year) {
        return financialStatementDao.selectByUniqueKey(
                company.getEdinetCode(),
                FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.toValue(),
                "0",
                String.valueOf(year)
        ).flatMap(FinancialStatement::getValue)
                .orElseThrow(() -> {
                    final var docId = edinetDocumentDao.selectDocIdBy(
                            codeConverter(company.getCode().orElseThrow(), companyDao.selectAll()),
                            "120",
                            String.valueOf(year)
                    ).getDocId();
                    documentDao.update(Document.builder()
                            .documentId(docId)
                            .scrapedNumberOfShares(DocumentStatus.HALF_WAY.toValue())
                            .updatedAt(LocalDateTime.now())
                            .build()
                    );
                    log.warn("  株式総数の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                                    "\t会社コード:{}\t科目名:{}\t対象年:{}\n書類パス:{}",
                            company.getCode().orElseThrow(),
                            "株式総数",
                            year,
                            documentDao.selectByDocumentId(docId).getNumberOfSharesDocumentPath()
                    );
                    throw new FundanalyzerCalculateException();
                });
    }

    private boolean isAnalyzed(final List<AnalysisResult> resultList, final String companyCode) {
        return resultList.stream()
                .anyMatch(analysisResult -> companyCode.equals(analysisResult.getCompanyCode()));
    }

    private String codeConverter(final String companyCode, final List<Company> companyAll) {
        return companyAll.stream()
                .filter(company -> company.getCode().isPresent())
                .filter(company -> companyCode.equals(company.getCode().get()))
                .map(Company::getEdinetCode)
                .findAny()
                .orElseThrow();
    }
}
