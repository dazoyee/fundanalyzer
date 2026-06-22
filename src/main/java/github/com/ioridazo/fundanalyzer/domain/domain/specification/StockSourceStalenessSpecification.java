package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SourceOfStockPrice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class StockSourceStalenessSpecification {

    private final StockPriceDao stockPriceDao;
    private final boolean nikkeiEnabled;
    private final boolean kabuoji3Enabled;
    private final boolean minkabuEnabled;
    private final boolean yahooFinanceEnabled;
    private final int stalenessAlertDays;

    public StockSourceStalenessSpecification(
            final StockPriceDao stockPriceDao,
            @Value("${app.config.stock.nikkei.enabled}") final boolean nikkeiEnabled,
            @Value("${app.config.stock.kabuoji3.enabled}") final boolean kabuoji3Enabled,
            @Value("${app.config.stock.minkabu.enabled}") final boolean minkabuEnabled,
            @Value("${app.config.stock.yahoo-finance.enabled}") final boolean yahooFinanceEnabled,
            @Value("${app.config.stock.staleness-alert-days}") final int stalenessAlertDays) {
        this.stockPriceDao = stockPriceDao;
        this.nikkeiEnabled = nikkeiEnabled;
        this.kabuoji3Enabled = kabuoji3Enabled;
        this.minkabuEnabled = minkabuEnabled;
        this.yahooFinanceEnabled = yahooFinanceEnabled;
        this.stalenessAlertDays = stalenessAlertDays;
    }

    LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    /**
     * 有効な株価ソースのうち、最新取得日が存在しない、または指定日数より古いソースを返します。
     *
     * @return 停滞している株価ソースの一覧
     */
    public List<SourceOfStockPrice> findStaleSources() {
        final LocalDate thresholdDate = nowLocalDate().minusDays(stalenessAlertDays);
        return Arrays.stream(SourceOfStockPrice.values())
                .filter(this::isEnabled)
                .filter(source -> isStale(source, thresholdDate))
                .toList();
    }

    private boolean isEnabled(final SourceOfStockPrice sourceOfStockPrice) {
        return switch (sourceOfStockPrice) {
            case NIKKEI -> nikkeiEnabled;
            case KABUOJI3 -> kabuoji3Enabled;
            case YAHOO_FINANCE -> yahooFinanceEnabled;
            case MINKABU -> minkabuEnabled;
        };
    }

    private boolean isStale(final SourceOfStockPrice sourceOfStockPrice, final LocalDate thresholdDate) {
        final Optional<LocalDate> maxTargetDate =
                stockPriceDao.selectMaxTargetDateBySource(sourceOfStockPrice.toValue());
        return maxTargetDate.isEmpty() || maxTargetDate.get().isBefore(thresholdDate);
    }
}
