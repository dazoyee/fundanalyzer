package github.com.ioridazo.fundanalyzer.domain.value;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.MinkabuEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPriceEntity;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class Stock {

    private final Company company;

    // 提出日株価平均
    private final BigDecimal averageStockPrice;

    // 株価取得日
    private final LocalDate importDate;

    // 最新株価
    private final BigDecimal latestStockPrice;

    // 最新予想株価
    private final BigDecimal latestForecastStock;

    private final List<StockPriceEntity> stockPriceEntityList;

    private final List<MinkabuEntity> minkabuEntityList;

    public Optional<BigDecimal> getAverageStockPrice() {
        return Optional.ofNullable(averageStockPrice);
    }

    public Optional<LocalDate> getImportDate() {
        return Optional.ofNullable(importDate);
    }

    public Optional<BigDecimal> getLatestStockPrice() {
        return Optional.ofNullable(latestStockPrice);
    }

    public Optional<BigDecimal> getLatestForecastStock() {
        return Optional.ofNullable(latestForecastStock);
    }
}
