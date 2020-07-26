package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BsEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.PlEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.PlSubject;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocument;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCalculateException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNoSuchBsElementException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNoSuchNsElementException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNoSuchPlElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static github.com.ioridazo.fundanalyzer.domain.ViewService.mapToPeriod;

@Slf4j
@Service
public class AnalysisService {

    private final CompanyDao companyDao;
    private final BsSubjectDao bsSubjectDao;
    private final PlSubjectDao plSubjectDao;
    private final EdinetDocumentDao edinetDocumentDao;
    private final DocumentDao documentDao;
    private final FinancialStatementDao financialStatementDao;
    private final AnalysisResultDao analysisResultDao;

    public AnalysisService(
            CompanyDao companyDao,
            BsSubjectDao bsSubjectDao,
            PlSubjectDao plSubjectDao,
            EdinetDocumentDao edinetDocumentDao,
            DocumentDao documentDao,
            FinancialStatementDao financialStatementDao,
            AnalysisResultDao analysisResultDao) {
        this.companyDao = companyDao;
        this.bsSubjectDao = bsSubjectDao;
        this.plSubjectDao = plSubjectDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.documentDao = documentDao;
        this.financialStatementDao = financialStatementDao;
        this.analysisResultDao = analysisResultDao;
    }

    public void analyze(final int year) {
        final var companyAll = companyDao.selectAll();
        var presentCompanies = new ArrayList<Company>();

        edinetDocumentDao.selectByDocTypeCodeAndPeriodEnd("120", String.valueOf(year)).stream()
                .map(EdinetDocument::getEdinetCode)
                .map(Optional::get)
                .forEach(edinetCode -> companyAll.stream()
                        .filter(company -> edinetCode.equals(company.getEdinetCode()))
                        .filter(company -> company.getCode().isPresent())
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
        final var operatingProfit = operatingProfit(company, year);
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
        final var subjectId = bsSubjectDao.selectByUniqueKey(bsEnum.getOutlineSubjectId(), bsEnum.getDetailSubjectId()).getId();
        try {
            return financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    subjectId,
                    String.valueOf(year)
            ).map(fs -> fs.getValue().orElseThrow(FundanalyzerNoSuchBsElementException::new)
            ).orElseThrow(FundanalyzerNoSuchBsElementException::new);
        } catch (FundanalyzerNoSuchBsElementException e) {
            final var docId = edinetDocumentDao.selectDocIdBy(
                    codeConverter(company.getCode().orElseThrow(), companyDao.selectAll()),
                    "120",
                    String.valueOf(year)
            ).getDocId();
            documentDao.update(Document.builder().documentId(docId).scrapedBs(DocumentStatus.HALF_WAY.toValue()).build());
            log.warn("貸借対照表の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                    "\t会社コード:{}\t科目名:{}\t対象年:{}", company.getCode().orElseThrow(), bsEnum.getSubject(), year);
            throw new FundanalyzerCalculateException(e);
        }
    }

    private Long plValues(final Company company, final PlEnum plEnum, final int year) {
        final var subjectId = plSubjectDao.selectByUniqueKey(plEnum.getOutlineSubjectId(), plEnum.getDetailSubjectId()).getId();
        try {
            return financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.toValue(),
                    subjectId,
                    String.valueOf(year)
            ).map(fs -> fs.getValue().orElseThrow(FundanalyzerNoSuchPlElementException::new)
            ).orElseThrow(FundanalyzerNoSuchPlElementException::new);
        } catch (FundanalyzerNoSuchPlElementException e) {
            final var docId = edinetDocumentDao.selectDocIdBy(
                    codeConverter(company.getCode().orElseThrow(), companyDao.selectAll()),
                    "120",
                    String.valueOf(year)
            ).getEdinetCode().orElseThrow();
            documentDao.update(Document.builder().documentId(docId).scrapedPl(DocumentStatus.HALF_WAY.toValue()).build());
            log.warn("損益計算書の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                    "\t会社コード:{}\t科目名:{}\t対象年:{}", company.getCode().orElseThrow(), plEnum.getSubject(), year);
            throw new FundanalyzerCalculateException(e);
        }
    }

    private Long operatingProfit(final Company company, final int year) {
        final var plSubjectIdList = plSubjectDao.selectByOutlineSubjectId(PlEnum.OPERATING_PROFIT.getOutlineSubjectId()).stream()
                .map(PlSubject::getId)
                .collect(Collectors.toList());

        for (String subjectId : plSubjectIdList) {
            try {
                return financialStatementDao.selectByUniqueKey(
                        company.getEdinetCode(),
                        FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.toValue(),
                        subjectId,
                        String.valueOf(year)
                ).map(fs -> fs.getValue().orElseThrow(FundanalyzerNoSuchPlElementException::new)
                ).orElseThrow(FundanalyzerNoSuchPlElementException::new);
            } catch (FundanalyzerNoSuchPlElementException ignored) {
            }
        }
        final var docId = edinetDocumentDao.selectDocIdBy(
                codeConverter(company.getCode().orElseThrow(), companyDao.selectAll()),
                "120",
                String.valueOf(year)
        ).getEdinetCode().orElseThrow();
        documentDao.update(Document.builder().documentId(docId).scrapedPl(DocumentStatus.HALF_WAY.toValue()).build());
        log.warn("損益計算書の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                "\t会社コード:{}\t科目名:{}\t対象年:{}", company.getCode().orElseThrow(), "営業利益", year);
        throw new FundanalyzerCalculateException();
    }

    private Long numberOfSharesValue(final Company company, final int year) {
        try {
            return financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.toValue(),
                    "0",
                    String.valueOf(year)
            ).map(fs -> fs.getValue().orElseThrow(FundanalyzerNoSuchNsElementException::new)
            ).orElseThrow(FundanalyzerNoSuchNsElementException::new);
        } catch (FundanalyzerNoSuchNsElementException e) {
            final var docId = edinetDocumentDao.selectDocIdBy(
                    codeConverter(company.getCode().orElseThrow(), companyDao.selectAll()),
                    "120",
                    String.valueOf(year)
            ).getEdinetCode().orElseThrow();
            documentDao.update(Document.builder().documentId(docId).scrapedNumberOfShares(DocumentStatus.HALF_WAY.toValue()).build());
            log.warn("株式総数がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                    "\t会社コード:{}\t対象年:{}", company.getCode().orElseThrow(), year);
            throw new FundanalyzerCalculateException(e);
        }
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
