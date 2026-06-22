package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SourceOfStockPrice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockSourceStalenessSpecificationTest {

    private static final LocalDate FIXED_NOW = LocalDate.of(2026, 6, 22);

    @Mock
    private StockPriceDao stockPriceDao;

    @Nested
    @DisplayName("findStaleSources")
    class FindStaleSources {

        @Test
        @DisplayName("有効ソースが閾値内なら空リストを返す")
        void returnsEmptyWhenAllEnabledSourcesAreFresh() {
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.NIKKEI.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW.minusDays(7)));
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.KABUOJI3.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW.minusDays(1)));
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.YAHOO_FINANCE.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW));
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.MINKABU.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW.minusDays(3)));

            StockSourceStalenessSpecification specification = newFixedNowSpecification(true, true, true, true, 7);

            List<SourceOfStockPrice> actual = specification.findStaleSources();

            assertTrue(actual.isEmpty());
        }

        @Test
        @DisplayName("有効ソースの最新日が古いならそのソースを返す")
        void returnsSourceWhenEnabledSourceIsStale() {
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.NIKKEI.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW.minusDays(8)));
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.KABUOJI3.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW));
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.YAHOO_FINANCE.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW));
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.MINKABU.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW));

            StockSourceStalenessSpecification specification = newFixedNowSpecification(true, true, true, true, 7);

            List<SourceOfStockPrice> actual = specification.findStaleSources();

            assertEquals(Collections.singletonList(SourceOfStockPrice.NIKKEI), actual);
        }

        @Test
        @DisplayName("有効ソースの最新日が空ならそのソースを返す")
        void returnsSourceWhenEnabledSourceHasNoData() {
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.NIKKEI.toValue()))
                    .thenReturn(Optional.empty());
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.KABUOJI3.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW));
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.YAHOO_FINANCE.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW));
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.MINKABU.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW));

            StockSourceStalenessSpecification specification = newFixedNowSpecification(true, true, true, true, 7);

            List<SourceOfStockPrice> actual = specification.findStaleSources();

            assertEquals(Collections.singletonList(SourceOfStockPrice.NIKKEI), actual);
        }

        @Test
        @DisplayName("無効ソースは最新日が古くても対象外")
        void skipsDisabledSourceEvenWhenStale() {
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.NIKKEI.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW));
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.KABUOJI3.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW));
            when(stockPriceDao.selectMaxTargetDateBySource(SourceOfStockPrice.MINKABU.toValue()))
                    .thenReturn(Optional.of(FIXED_NOW));

            StockSourceStalenessSpecification specification = newFixedNowSpecification(true, true, false, true, 7);

            List<SourceOfStockPrice> actual = specification.findStaleSources();

            assertTrue(actual.isEmpty());
        }
    }

    private StockSourceStalenessSpecification newFixedNowSpecification(
            boolean nikkeiEnabled,
            boolean kabuoji3Enabled,
            boolean yahooFinanceEnabled,
            boolean minkabuEnabled,
            int stalenessAlertDays) {
        return new StockSourceStalenessSpecification(
                stockPriceDao,
                nikkeiEnabled,
                kabuoji3Enabled,
                minkabuEnabled,
                yahooFinanceEnabled,
                stalenessAlertDays) {
            @Override
            LocalDate nowLocalDate() {
                return FIXED_NOW;
            }
        };
    }
}
