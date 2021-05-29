package github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail;

import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import lombok.Value;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class FinanceValueViewModel {
    // 流動資産合計
    private final Long totalCurrentAssets;
    // 投資その他の資産合計
    private final Long totalInvestmentsAndOtherAssets;
    // 流動負債合計
    private final Long totalCurrentLiabilities;
    // 固定負債合計
    private final Long totalFixedLiabilities;
    // 営業利益
    private final Long operatingProfit;
    // 株式総数
    private final Long numberOfShares;

    public static FinanceValueViewModel of(final FinanceValue financeValue) {
        return new FinanceValueViewModel(
                financeValue.getTotalCurrentAssets().orElse(null),
                financeValue.getTotalInvestmentsAndOtherAssets().orElse(null),
                financeValue.getTotalCurrentLiabilities().orElse(null),
                financeValue.getTotalFixedLiabilities().orElse(null),
                financeValue.getOperatingProfit().orElse(null),
                financeValue.getNumberOfShares().orElse(null)
        );
    }
}
