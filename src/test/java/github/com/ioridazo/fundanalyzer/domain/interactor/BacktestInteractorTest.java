package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CorporateActionSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ValuationSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Horizon;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.BacktestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BacktestInteractor} のテスト。
 */
@DisplayName("BacktestInteractorのテスト")
class BacktestInteractorTest {

    private static final double DELTA = 1.0e-9;

    private CompanySpecification companySpecification;
    private ValuationSpecification valuationSpecification;
    private StockSpecification stockSpecification;
    private AnalysisResultSpecification analysisResultSpecification;
    private CorporateActionSpecification corporateActionSpecification;

    private BacktestInteractor interactor;

    @BeforeEach
    void setUp() {
        companySpecification = mock(CompanySpecification.class);
        valuationSpecification = mock(ValuationSpecification.class);
        stockSpecification = mock(StockSpecification.class);
        analysisResultSpecification = mock(AnalysisResultSpecification.class);
        corporateActionSpecification = mock(CorporateActionSpecification.class);

        interactor = spy(new BacktestInteractor(
                companySpecification,
                valuationSpecification,
                stockSpecification,
                analysisResultSpecification,
                corporateActionSpecification
        ));
        interactor.bucketBoundaries = List.of(
                BigDecimal.valueOf(1.0),
                BigDecimal.valueOf(1.2),
                BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(2.0)
        );
        interactor.matchToleranceDays = 7;

        when(corporateActionSpecification.findActions(any())).thenReturn(List.of());
        when(corporateActionSpecification.adjustToBasisWithActions(any(), any(), any(), any(), eq(true)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("backtestメソッドのテスト")
    class BacktestMethodTest {

        @Test
        @DisplayName("1社1エピソードで十分な株価時系列→3M6M12MのreturnRateを集計する")
        void singleEpisodeAcrossHorizons() {
            final Company company = company("10000", "Tech", true);
            final ValuationEntity valuation = valuation(
                    LocalDate.parse("2024-01-01"),
                    LocalDate.parse("2024-01-10"),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1.5),
                    0,
                    10
            );
            final List<StockPriceEntity> stocks = List.of(
                    stock(LocalDate.parse("2024-04-09"), 110.0),
                    stock(LocalDate.parse("2024-07-08"), 120.0),
                    stock(LocalDate.parse("2025-01-09"), 130.0)
            );

            stubCompanyScenario(company, List.of(valuation), stocks);
            when(analysisResultSpecification.findAnalysisResult(10)).thenReturn(Optional.empty());

            final BacktestResult actual = interactor.backtest();

            assertEquals(3, actual.horizons().size());
            assertAll(
                    () -> assertEquals(Horizon.M3, actual.horizons().get(0).horizon()),
                    () -> assertEquals(0.10, actual.horizons().get(0).scatter().get(0).returnRate(), DELTA),
                    () -> assertEquals(Horizon.M6, actual.horizons().get(1).horizon()),
                    () -> assertEquals(0.20, actual.horizons().get(1).scatter().get(0).returnRate(), DELTA),
                    () -> assertEquals(Horizon.M12, actual.horizons().get(2).horizon()),
                    () -> assertEquals(0.30, actual.horizons().get(2).scatter().get(0).returnRate(), DELTA)
            );
        }

        @Test
        @DisplayName("許容日数内に株価がないとき→生存企業はmissingで除外する")
        void missingForLivedCompany() {
            final Company company = company("10000", "Tech", true);
            final ValuationEntity valuation = valuation(
                    LocalDate.parse("2024-01-01"),
                    LocalDate.parse("2024-01-10"),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1.5),
                    0,
                    10
            );

            stubCompanyScenario(company, List.of(valuation), List.of());
            when(analysisResultSpecification.findAnalysisResult(10)).thenReturn(Optional.empty());

            final BacktestResult actual = interactor.backtest();

            assertEquals(3, actual.horizons().size());
            assertAll(
                    () -> assertEquals(1L, actual.horizons().get(0).excludedMissing()),
                    () -> assertEquals(0L, actual.horizons().get(0).excludedDelisted()),
                    () -> assertEquals(1L, actual.horizons().get(1).excludedMissing()),
                    () -> assertEquals(1L, actual.horizons().get(2).excludedMissing())
            );
        }

        @Test
        @DisplayName("許容日数内に株価がないとき→非生存企業はdelistedで除外する")
        void delistedForNonLivedCompany() {
            final Company company = company("10000", "Tech", false);
            final ValuationEntity valuation = valuation(
                    LocalDate.parse("2024-01-01"),
                    LocalDate.parse("2024-01-10"),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1.5),
                    0,
                    10
            );

            stubCompanyScenario(company, List.of(valuation), List.of());
            when(analysisResultSpecification.findAnalysisResult(10)).thenReturn(Optional.empty());

            final BacktestResult actual = interactor.backtest();

            assertEquals(3, actual.horizons().size());
            assertAll(
                    () -> assertEquals(1L, actual.horizons().get(0).excludedDelisted()),
                    () -> assertEquals(0L, actual.horizons().get(0).excludedMissing()),
                    () -> assertEquals(1L, actual.horizons().get(1).excludedDelisted()),
                    () -> assertEquals(1L, actual.horizons().get(2).excludedDelisted())
            );
        }

        @Test
        @DisplayName("企業価値が取得できるとき→convergenceを算出する")
        void computesConvergence() {
            final Company company = company("10000", "Tech", true);
            final ValuationEntity valuation = valuation(
                    LocalDate.parse("2024-01-01"),
                    LocalDate.parse("2024-01-10"),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1.5),
                    0,
                    10
            );
            final List<StockPriceEntity> stocks = List.of(
                    stock(LocalDate.parse("2024-04-09"), 125.0),
                    stock(LocalDate.parse("2024-07-08"), 125.0),
                    stock(LocalDate.parse("2025-01-09"), 125.0)
            );
            stubCompanyScenario(company, List.of(valuation), stocks);
            when(analysisResultSpecification.findAnalysisResult(10)).thenReturn(Optional.of(analysisResult(
                    10,
                    "10000",
                    BigDecimal.valueOf(150)
            )));

            final BacktestResult actual = interactor.backtest();

            assertEquals(3, actual.horizons().size());
            assertEquals(0.6, actual.horizons().get(0).buckets().get(0).avgConvergence(), DELTA);
            verify(analysisResultSpecification, times(1)).findAnalysisResult(10);
        }

        @Test
        @DisplayName("複数企業の入力→horizonとbucket構造を返す")
        void returnsExpectedHorizonAndBucketStructure() {
            final Company tech = company("10000", "Tech", true);
            final Company retail = company("20000", "Retail", true);
            final ValuationEntity techValuation = valuation(
                    LocalDate.parse("2024-01-01"),
                    LocalDate.parse("2024-01-10"),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(0.9),
                    0,
                    10
            );
            final ValuationEntity retailValuation = valuation(
                    LocalDate.parse("2024-02-01"),
                    LocalDate.parse("2024-02-10"),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1.4),
                    0,
                    20
            );

            when(companySpecification.inquiryAllTargetCompanies()).thenReturn(List.of(tech, retail));
            when(valuationSpecification.findAllValuationEntities("10000")).thenReturn(List.of(techValuation));
            when(valuationSpecification.findAllValuationEntities("20000")).thenReturn(List.of(retailValuation));
            when(stockSpecification.findEntityList("10000")).thenReturn(List.of(
                    stock(LocalDate.parse("2024-04-09"), 110.0),
                    stock(LocalDate.parse("2024-07-08"), 120.0),
                    stock(LocalDate.parse("2025-01-09"), 130.0)
            ));
            when(stockSpecification.findEntityList("20000")).thenReturn(List.of(
                    stock(LocalDate.parse("2024-05-10"), 90.0),
                    stock(LocalDate.parse("2024-08-08"), 80.0),
                    stock(LocalDate.parse("2025-02-10"), 70.0)
            ));
            when(corporateActionSpecification.findActions("10000")).thenReturn(List.of());
            when(corporateActionSpecification.findActions("20000")).thenReturn(List.of());
            when(analysisResultSpecification.findAnalysisResult(10)).thenReturn(Optional.empty());
            when(analysisResultSpecification.findAnalysisResult(20)).thenReturn(Optional.empty());

            final BacktestResult actual = interactor.backtest();

            assertEquals(3, actual.horizons().size());
            assertAll(
                    () -> assertEquals(Horizon.M3, actual.horizons().get(0).horizon()),
                    () -> assertEquals(2L, actual.horizons().get(0).episodeCount()),
                    () -> assertEquals(2, actual.horizons().get(0).buckets().size()),
                    () -> assertEquals("<100%", actual.horizons().get(0).buckets().get(0).label()),
                    () -> assertEquals("120〜150%", actual.horizons().get(0).buckets().get(1).label()),
                    () -> assertEquals(1.0, actual.horizons().get(0).industries().get(0).hitRate(), DELTA),
                    () -> assertEquals(0.0, actual.horizons().get(0).industries().get(1).hitRate(), DELTA)
            );
        }
    }

    private void stubCompanyScenario(
            final Company company,
            final List<ValuationEntity> valuations,
            final List<StockPriceEntity> stocks) {
        when(companySpecification.inquiryAllTargetCompanies()).thenReturn(List.of(company));
        when(valuationSpecification.findAllValuationEntities(company.code())).thenReturn(valuations);
        when(stockSpecification.findEntityList(company.code())).thenReturn(stocks);
        when(corporateActionSpecification.findActions(company.code())).thenReturn(List.of());
    }

    private static Company company(final String code, final String industryName, final boolean lived) {
        return new Company(code, code, 1, industryName, "E" + code, null, null, null, null, false, false, lived);
    }

    private static ValuationEntity valuation(
            final LocalDate submitDate,
            final LocalDate targetDate,
            final BigDecimal stockPrice,
            final BigDecimal discountRate,
            final long daySinceSubmitDate,
            final Integer analysisResultId) {
        return new ValuationEntity(
                1,
                "10000",
                submitDate,
                targetDate,
                null,
                stockPrice,
                null,
                null,
                daySinceSubmitDate,
                null,
                null,
                null,
                discountRate,
                analysisResultId,
                LocalDateTime.parse("2024-01-01T00:00:00")
        );
    }

    private static StockPriceEntity stock(final LocalDate targetDate, final Double stockPrice) {
        return new StockPriceEntity(
                1,
                "10000",
                targetDate,
                stockPrice,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "1",
                LocalDateTime.parse("2024-01-01T00:00:00"),
                LocalDateTime.parse("2024-01-01T00:00:00")
        );
    }

    private static AnalysisResultEntity analysisResult(
            final Integer id,
            final String companyCode,
            final BigDecimal corporateValue) {
        return new AnalysisResultEntity(
                id,
                companyCode,
                LocalDate.parse("2023-12-31"),
                corporateValue,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.parse("2024-01-01"),
                "DOC-1",
                LocalDateTime.parse("2024-01-01T00:00:00")
        );
    }
}
