package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.bean.CompanyViewBean;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BalanceSheetEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.ProfitAndLossStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCalculateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AnalysisService {

    private final CompanyDao companyDao;
    private final FinancialStatementDao financialStatementDao;
    private final AnalysisResultDao analysisResultDao;

    public AnalysisService(
            CompanyDao companyDao,
            FinancialStatementDao financialStatementDao,
            AnalysisResultDao analysisResultDao) {
        this.companyDao = companyDao;
        this.financialStatementDao = financialStatementDao;
        this.analysisResultDao = analysisResultDao;
    }

    private static LocalDate generatePeriod(final String year) {
        return LocalDate.of(Integer.parseInt(year), 1, 1);
    }

    public List<CompanyViewBean> viewCompany(final String year) {
        final var resultList = analysisResultDao.selectByPeriod(generatePeriod(year));
        final var companyList = companyDao.selectAll().stream()
                .filter(company -> company.getCode().isPresent())
                .collect(Collectors.toList());
        var viewBeanList = new ArrayList<CompanyViewBean>();

        resultList.forEach(analysisResult -> viewBeanList.add(new CompanyViewBean(
                analysisResult.getCompanyCode(),
                companyList.stream()
                        .filter(company -> analysisResult.getCompanyCode().equals(company.getCode().orElseThrow()))
                        .findAny()
                        .orElseThrow()
                        .getCompanyName(),
                analysisResult.getCorporateValue(),
                analysisResult.getPeriod().getYear()
        )));

        return viewBeanList;
    }

    public List<CompanyViewBean> analyze(final String year) {
        final var companyCodeList = companyDao.selectAll().stream()
                .map(Company::getCode)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        final var resultListAlready = analysisResultDao.selectByPeriod(generatePeriod(year));
        companyCodeList.forEach(companyCode -> {
            //noinspection StatementWithEmptyBody
            if (isAnalyzed(resultListAlready, companyCode)) {
                // 分析済
            } else {
                try {
                    analysisResultDao.insert(new AnalysisResult(
                                    null,
                                    companyCode,
                                    calculate(companyCode, year),
                                    LocalDate.of(Integer.parseInt(year), 1, 1),
                                    LocalDateTime.now()
                            )
                    );
                } catch (FundanalyzerCalculateException ignored) {
                    log.info("エラー発生により、企業価値を算出できませんでした。\t証券コード:{}", companyCode);
                }
            }
        });

        return viewCompany(year);
    }

    private BigDecimal calculate(final String companyCode, final String year) throws FundanalyzerCalculateException {
        try {
            final var company = companyDao.selectByCode(companyCode).orElseThrow();
            // 流動資産合計
            final var totalCurrentAssets = financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    BalanceSheetEnum.TOTAL_CURRENT_ASSETS.toValue(),
                    year).getValue().orElseThrow();
            System.out.println("流動資産合計 : " + totalCurrentAssets);

            // 投資その他の資産合計
            final var totalInvestmentsAndOtherAssets = financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    BalanceSheetEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS.toValue(),
                    year).getValue().orElseThrow();
            System.out.println("投資その他の資産合計 : " + totalInvestmentsAndOtherAssets);

            // 流動負債合計
            final var totalCurrentLiabilities = financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    BalanceSheetEnum.TOTAL_CURRENT_LIABILITIES.toValue(),
                    year).getValue().orElseThrow();
            System.out.println("流動負債合計 : " + totalCurrentLiabilities);

            // 固定負債合計
            final var totalFixedLiabilities = financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    BalanceSheetEnum.TOTAL_FIXED_LIABILITIES.toValue(),
                    year).getValue().orElseThrow();
            System.out.println("固定負債合計 : " + totalFixedLiabilities);

            // 営業利益
            final var operatingProfit = financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.toValue(),
                    ProfitAndLossStatementEnum.OPERATING_PROFIT.toValue(),
                    year).getValue().orElseThrow();
            System.out.println("営業利益 : " + operatingProfit);

            final var v =
                    (
                            operatingProfit * 10
                                    + totalCurrentAssets - (totalCurrentLiabilities * 1.2) + totalInvestmentsAndOtherAssets
                                    - totalFixedLiabilities
                    )
                            / 10000000;

            return BigDecimal.valueOf(v);

        } catch (NoSuchElementException | EmptyResultDataAccessException e) {
            log.error("データベースに値が存在しないか、またはNULLで登録されています。次のエラーメッセージを確認してください。" +
                    "\tmessage:{}", e.getMessage());
            throw new FundanalyzerCalculateException(e);
        }
    }

    private boolean isAnalyzed(final List<AnalysisResult> resultList, final String companyCode) {
        return resultList.stream()
                .anyMatch(analysisResult -> companyCode.equals(analysisResult.getCompanyCode()));
    }
}
