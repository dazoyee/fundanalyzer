package github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.BsSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.PlSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.value.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import github.com.ioridazo.fundanalyzer.domain.value.PlSubject;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * @param totalCurrentAssets             流動資産合計
 * @param totalInvestmentsAndOtherAssets 投資その他の資産合計
 * @param totalAssets                    総資産
 * @param totalCurrentLiabilities        流動負債合計
 * @param totalFixedLiabilities          固定負債合計
 * @param subscriptionWarrant            新株予約権
 * @param netAssets                      純資産
 * @param operatingProfit                営業利益
 * @param netIncome                      当期純利益
 * @param numberOfShares                 株式総数
 */
public record FinanceValueViewModel(
        IdValue totalCurrentAssets,
        IdValue totalInvestmentsAndOtherAssets,
        IdValue totalAssets,
        IdValue totalCurrentLiabilities,
        IdValue totalFixedLiabilities,
        IdValue subscriptionWarrant,
        IdValue netAssets,
        IdValue operatingProfit,
        IdValue netIncome,
        IdValue numberOfShares
) {

    public static FinanceValueViewModel of(
            final List<BsSubjectEntity> bsSubjectEntityList,
            final List<PlSubjectEntity> plSubjectEntityList,
            final FinanceValue financeValue) {
        try {
            return new FinanceValueViewModel(
                    new IdValue(
                            FinancialStatementEnum.BALANCE_SHEET.getId(),
                            bsSubjectEntityList.stream()
                                    .filter(bs -> bs.getName().equals(BsSubject.BsEnum.TOTAL_CURRENT_ASSETS.getSubject()))
                                    .findFirst().orElseThrow()
                                    .getId(),
                            BsSubject.BsEnum.TOTAL_CURRENT_ASSETS.getSubject(),
                            financeValue.getTotalCurrentAssets().orElse(null)
                    ),
                    new IdValue(
                            FinancialStatementEnum.BALANCE_SHEET.getId(),
                            bsSubjectEntityList.stream()
                                    .filter(bs -> bs.getName().equals(BsSubject.BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS.getSubject()))
                                    .findFirst().orElseThrow()
                                    .getId(),
                            BsSubject.BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS.getSubject(),
                            financeValue.getTotalInvestmentsAndOtherAssets().orElse(null)
                    ),
                    new IdValue(
                            FinancialStatementEnum.BALANCE_SHEET.getId(),
                            bsSubjectEntityList.stream()
                                    .filter(bs -> bs.getName().equals(BsSubject.BsEnum.TOTAL_ASSETS.getSubject()))
                                    .findFirst().orElseThrow()
                                    .getId(),
                            BsSubject.BsEnum.TOTAL_ASSETS.getSubject(),
                            financeValue.getTotalAssets().orElse(null)
                    ),
                    new IdValue(
                            FinancialStatementEnum.BALANCE_SHEET.getId(),
                            bsSubjectEntityList.stream()
                                    .filter(bs -> bs.getName().equals(BsSubject.BsEnum.TOTAL_CURRENT_LIABILITIES.getSubject()))
                                    .findFirst().orElseThrow()
                                    .getId(),
                            BsSubject.BsEnum.TOTAL_CURRENT_LIABILITIES.getSubject(),
                            financeValue.getTotalCurrentLiabilities().orElse(null)
                    ),
                    new IdValue(
                            FinancialStatementEnum.BALANCE_SHEET.getId(),
                            bsSubjectEntityList.stream()
                                    .filter(bs -> bs.getName().equals(BsSubject.BsEnum.TOTAL_FIXED_LIABILITIES.getSubject()))
                                    .findFirst().orElseThrow()
                                    .getId(),
                            BsSubject.BsEnum.TOTAL_FIXED_LIABILITIES.getSubject(),
                            financeValue.getTotalFixedLiabilities().orElse(null)
                    ),
                    new IdValue(
                            FinancialStatementEnum.BALANCE_SHEET.getId(),
                            bsSubjectEntityList.stream()
                                    .filter(bs -> bs.getName().equals(BsSubject.BsEnum.SUBSCRIPTION_WARRANT.getSubject()))
                                    .findFirst().orElseThrow()
                                    .getId(),
                            BsSubject.BsEnum.SUBSCRIPTION_WARRANT.getSubject(),
                            financeValue.getSubscriptionWarrant().orElse(null)
                    ),
                    new IdValue(
                            FinancialStatementEnum.BALANCE_SHEET.getId(),
                            bsSubjectEntityList.stream()
                                    .filter(bs -> bs.getName().equals(BsSubject.BsEnum.TOTAL_NET_ASSETS.getSubject()))
                                    .findFirst().orElseThrow()
                                    .getId(),
                            BsSubject.BsEnum.TOTAL_NET_ASSETS.getSubject(),
                            financeValue.getNetAssets().orElse(null)
                    ),
                    new IdValue(
                            FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.getId(),
                            plSubjectEntityList.stream()
                                    .filter(pl -> pl.name().equals(PlSubject.PlEnum.OPERATING_PROFIT.getSubject()))
                                    .findFirst().orElseThrow()
                                    .id(),
                            PlSubject.PlEnum.OPERATING_PROFIT.getSubject(),
                            financeValue.getOperatingProfit().orElse(null)
                    ),
                    new IdValue(
                            FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.getId(),
                            plSubjectEntityList.stream()
                                    .filter(pl -> pl.name().equals(PlSubject.PlEnum.NET_INCOME.getSubject()))
                                    .findFirst().orElseThrow()
                                    .id(),
                            PlSubject.PlEnum.NET_INCOME.getSubject(),
                            financeValue.getNetIncome().orElse(null)
                    ),
                    new IdValue(
                            FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.getId(),
                            "0",
                            FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.getName(),
                            financeValue.getNumberOfShares().orElse(null)
                    )
            );
        } catch (final NoSuchElementException e) {
            throw new FundanalyzerRuntimeException("財務諸表の科目が存在していません。", e);
        }
    }

    public record IdValue(
            String financialStatementId,
            String subjectId,
            String name,
            Long value
    ) {
    }
}
