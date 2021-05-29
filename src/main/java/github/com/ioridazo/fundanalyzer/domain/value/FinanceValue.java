package github.com.ioridazo.fundanalyzer.domain.value;

import lombok.Value;

import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class FinanceValue {

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

    public Optional<Long> getTotalCurrentAssets() {
        return Optional.ofNullable(totalCurrentAssets);
    }

    public Optional<Long> getTotalInvestmentsAndOtherAssets() {
        return Optional.ofNullable(totalInvestmentsAndOtherAssets);
    }

    public Optional<Long> getTotalCurrentLiabilities() {
        return Optional.ofNullable(totalCurrentLiabilities);
    }

    public Optional<Long> getTotalFixedLiabilities() {
        return Optional.ofNullable(totalFixedLiabilities);
    }

    public Optional<Long> getOperatingProfit() {
        return Optional.ofNullable(operatingProfit);
    }

    public Optional<Long> getNumberOfShares() {
        return Optional.ofNullable(numberOfShares);
    }
}
