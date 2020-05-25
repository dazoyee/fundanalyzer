package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BalanceSheetEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.ProfitAndLossStatementEnum;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AnalysisService {

    private final FinancialStatementDao financialStatementDao;

    public AnalysisService(
            FinancialStatementDao financialStatementDao) {
        this.financialStatementDao = financialStatementDao;
    }

    public String analyze(final String companyCode, final String year) {
        try {
            // 流動資産合計
            final var totalCurrentAssets = financialStatementDao.selectByUniqueKey(
                    companyCode,
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    BalanceSheetEnum.TOTAL_CURRENT_ASSETS.toValue(),
                    year).getValue().orElseThrow();

            // 投資その他の資産合計
            final var totalInvestmentsAndOtherAssets = financialStatementDao.selectByUniqueKey(
                    companyCode,
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    BalanceSheetEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS.toValue(),
                    year).getValue().orElseThrow();

            // 流動負債合計
            final var totalCurrentLiabilities = financialStatementDao.selectByUniqueKey(
                    companyCode,
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    BalanceSheetEnum.TOTAL_CURRENT_LIABILITIES.toValue(),
                    year).getValue().orElseThrow();

            // 固定負債合計
            final var totalFixedLiabilities = financialStatementDao.selectByUniqueKey(
                    companyCode,
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    BalanceSheetEnum.TOTAL_CURRENT_LIABILITIES.toValue(),
                    year).getValue().orElseThrow();

            // 営業利益
            final var operatingProfit = financialStatementDao.selectByUniqueKey(
                    companyCode,
                    FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.toValue(),
                    ProfitAndLossStatementEnum.OPERATING_PROFIT.toValue(),
                    year).getValue().orElseThrow();

            final var v = (operatingProfit * 10 + totalCurrentAssets - (totalCurrentLiabilities * 1.2) + totalInvestmentsAndOtherAssets)
                    - (totalFixedLiabilities)
                    / 100;

            return String.valueOf(v);
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new FundanalyzerRuntimeException("値なし");
        }
    }
}
