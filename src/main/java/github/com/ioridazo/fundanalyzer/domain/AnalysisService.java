package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BalanceSheetEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.ProfitAndLossStatementEnum;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Slf4j
@Service
public class AnalysisService {

    private final CompanyDao companyDao;
    private final FinancialStatementDao financialStatementDao;

    public AnalysisService(
            CompanyDao companyDao,
            FinancialStatementDao financialStatementDao) {
        this.companyDao = companyDao;
        this.financialStatementDao = financialStatementDao;
    }

    public String analyze(final String companyCode, final String year) {
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

            final var v = (operatingProfit * 10 + totalCurrentAssets - (totalCurrentLiabilities * 1.2) + totalInvestmentsAndOtherAssets)
                    - (totalFixedLiabilities)
                    / 100;

            return String.valueOf(v);
        } catch (NoSuchElementException e) {
            log.error(e.getMessage());
            throw new FundanalyzerRuntimeException("データベースには値がNULLで登録されています。");
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new FundanalyzerRuntimeException("値なし");
        }
    }
}
