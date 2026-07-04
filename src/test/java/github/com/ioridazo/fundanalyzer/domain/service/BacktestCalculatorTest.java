package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.value.Horizon;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.BacktestResult;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.EpisodeOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BacktestCalculator} のテスト。
 */
@DisplayName("BacktestCalculatorのテスト")
class BacktestCalculatorTest {

    private static final double DELTA = 1.0e-9;

    @Nested
    @DisplayName("medianのテスト")
    class MedianTest {

        @ParameterizedTest
        @CsvSource({
                "'1.0', 1.0",
                "'3.0,1.0,2.0', 2.0",
                "'4.0,1.0,3.0,2.0', 2.5"
        })
        @DisplayName("入力値ごとの中央値")
        void median(final String csvValues, final double expected) {
            final double[] values = parse(csvValues);

            final double actual = BacktestCalculator.median(values);

            assertEquals(expected, actual, DELTA);
        }

        @Test
        @DisplayName("空配列→IllegalArgumentException")
        void emptyArray() {
            assertThrows(IllegalArgumentException.class, () -> BacktestCalculator.median(new double[0]));
        }
    }

    @Nested
    @DisplayName("pearsonのテスト")
    class PearsonTest {

        @Test
        @DisplayName("正の線形関係→1に近い値")
        void positiveCorrelation() {
            final Double actual = BacktestCalculator.pearson(
                    new double[]{1.0, 2.0, 3.0},
                    new double[]{2.0, 4.0, 6.0});

            assertEquals(1.0, actual, DELTA);
        }

        @Test
        @DisplayName("相関なしの系列→0に近い値")
        void noCorrelation() {
            final Double actual = BacktestCalculator.pearson(
                    new double[]{1.0, 2.0, 3.0},
                    new double[]{1.0, 0.0, 1.0});

            assertEquals(0.0, actual, DELTA);
        }

        @Test
        @DisplayName("要素数1→null")
        void tooShort() {
            final Double actual = BacktestCalculator.pearson(
                    new double[]{1.0},
                    new double[]{2.0});

            assertNull(actual);
        }

        @Test
        @DisplayName("分散ゼロの系列→null")
        void zeroVariance() {
            final Double actual = BacktestCalculator.pearson(
                    new double[]{1.0, 1.0, 1.0},
                    new double[]{2.0, 3.0, 4.0});

            assertNull(actual);
        }
    }

    @Nested
    @DisplayName("bucketLabelOfのテスト")
    class BucketLabelOfTest {

        private final List<BigDecimal> boundaries = List.of(
                BigDecimal.valueOf(1.0),
                BigDecimal.valueOf(1.2),
                BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(2.0)
        );

        @ParameterizedTest
        @CsvSource({
                "0.9, <100%",
                "1.0, 100〜120%",
                "1.1, 100〜120%",
                "1.2, 120〜150%",
                "1.4, 120〜150%",
                "1.5, 150〜200%",
                "2.0, 200%+",
                "2.3, 200%+"
        })
        @DisplayName("境界値と区間内の値→対応するラベル")
        void label(final double rate, final String expected) {
            final String actual = BacktestCalculator.bucketLabelOf(BigDecimal.valueOf(rate), boundaries);

            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("aggregateのテスト")
    class AggregateTest {

        private final List<BigDecimal> boundaries = List.of(
                BigDecimal.valueOf(1.0),
                BigDecimal.valueOf(1.2),
                BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(2.0)
        );

        @Test
        @DisplayName("複数期間の入力→期間別集計を返す")
        void multipleHorizons() {
            final List<EpisodeOutcome> outcomes = List.of(
                    new EpisodeOutcome(Horizon.M3, BigDecimal.valueOf(0.9), "Tech", 0.10, 0.50),
                    new EpisodeOutcome(Horizon.M3, BigDecimal.valueOf(1.1), "Tech", 0.20, null),
                    new EpisodeOutcome(Horizon.M3, BigDecimal.valueOf(1.4), "Retail", -0.10, 0.20),
                    new EpisodeOutcome(Horizon.M3, BigDecimal.valueOf(2.0), "Retail", 0.40, 0.80),
                    new EpisodeOutcome(Horizon.M6, BigDecimal.valueOf(1.0), "Health", 0.10, 0.10),
                    new EpisodeOutcome(Horizon.M6, BigDecimal.valueOf(1.5), "Finance", 0.20, 0.20),
                    new EpisodeOutcome(Horizon.M6, BigDecimal.valueOf(2.0), "Finance", 0.30, null)
            );
            final Map<Horizon, long[]> exclusions = Map.of(
                    Horizon.M3, new long[]{2L, 1L},
                    Horizon.M12, new long[]{1L, 0L}
            );

            final BacktestResult actual = BacktestCalculator.aggregate(outcomes, exclusions, boundaries);

            assertEquals(3, actual.horizons().size());

            final BacktestResult.HorizonResult m3 = actual.horizons().get(0);
            final BacktestResult.HorizonResult m6 = actual.horizons().get(1);
            final BacktestResult.HorizonResult m12 = actual.horizons().get(2);

            assertAll(
                    () -> assertEquals(Horizon.M3, m3.horizon()),
                    () -> assertEquals(4L, m3.episodeCount()),
                    () -> assertEquals(2L, m3.excludedDelisted()),
                    () -> assertEquals(1L, m3.excludedMissing()),
                    () -> assertEquals(4, m3.buckets().size()),
                    () -> assertEquals("<100%", m3.buckets().get(0).label()),
                    () -> assertEquals(1L, m3.buckets().get(0).count()),
                    () -> assertEquals(0.10, m3.buckets().get(0).avgReturn(), DELTA),
                    () -> assertEquals(0.10, m3.buckets().get(0).medianReturn(), DELTA),
                    () -> assertEquals(1.0, m3.buckets().get(0).hitRate(), DELTA),
                    () -> assertEquals(0.50, m3.buckets().get(0).avgConvergence(), DELTA),
                    () -> assertEquals("100〜120%", m3.buckets().get(1).label()),
                    () -> assertEquals(1L, m3.buckets().get(1).count()),
                    () -> assertEquals("120〜150%", m3.buckets().get(2).label()),
                    () -> assertEquals(1L, m3.buckets().get(2).count()),
                    () -> assertEquals(-0.10, m3.buckets().get(2).avgReturn(), DELTA),
                    () -> assertEquals(0.0, m3.buckets().get(2).hitRate(), DELTA),
                    () -> assertEquals("200%+", m3.buckets().get(3).label()),
                    () -> assertEquals(1L, m3.buckets().get(3).count()),
                    () -> assertEquals(0.40, m3.buckets().get(3).avgReturn(), DELTA),
                    () -> assertEquals(2, m3.industries().size()),
                    () -> assertEquals("Tech", m3.industries().get(0).industryName()),
                    () -> assertEquals(1.0, m3.industries().get(0).hitRate(), DELTA),
                    () -> assertEquals("Retail", m3.industries().get(1).industryName()),
                    () -> assertEquals(0.5, m3.industries().get(1).hitRate(), DELTA),
                    () -> assertEquals(4, m3.scatter().size()),
                    () -> assertEquals(0.9, m3.scatter().get(0).discountRate(), DELTA),
                    () -> assertEquals(0.10, m3.scatter().get(0).returnRate(), DELTA)
            );

            assertAll(
                    () -> assertEquals(Horizon.M6, m6.horizon()),
                    () -> assertEquals(3L, m6.episodeCount()),
                    () -> assertEquals(0L, m6.excludedDelisted()),
                    () -> assertEquals(0L, m6.excludedMissing()),
                    () -> assertEquals(3, m6.buckets().size()),
                    () -> assertEquals(1.0, m6.correlation(), DELTA),
                    () -> assertEquals(1.0, m6.industries().get(0).hitRate(), DELTA),
                    () -> assertEquals(1.0, m6.industries().get(1).hitRate(), DELTA),
                    () -> assertTrue(m6.industries().get(0).hitRate() >= m6.industries().get(1).hitRate())
            );

            assertAll(
                    () -> assertEquals(Horizon.M12, m12.horizon()),
                    () -> assertEquals(0L, m12.episodeCount()),
                    () -> assertEquals(1L, m12.excludedDelisted()),
                    () -> assertEquals(0L, m12.excludedMissing()),
                    () -> assertTrue(m12.buckets().isEmpty()),
                    () -> assertNull(m12.correlation()),
                    () -> assertTrue(m12.industries().isEmpty()),
                    () -> assertTrue(m12.scatter().isEmpty())
            );
        }

        @Test
        @DisplayName("outcomes空かつ除外のみ→除外がある期間だけ返す")
        void emptyOutcomesWithExclusions() {
            final BacktestResult actual = BacktestCalculator.aggregate(
                    List.of(),
                    Map.of(Horizon.M6, new long[]{3L, 4L}),
                    boundaries);

            assertEquals(1, actual.horizons().size());
            assertAll(
                    () -> assertEquals(Horizon.M6, actual.horizons().get(0).horizon()),
                    () -> assertEquals(0L, actual.horizons().get(0).episodeCount()),
                    () -> assertEquals(3L, actual.horizons().get(0).excludedDelisted()),
                    () -> assertEquals(4L, actual.horizons().get(0).excludedMissing())
            );
        }
    }

    private static double[] parse(final String csvValues) {
        return List.of(csvValues.split(",")).stream()
                .map(String::trim)
                .mapToDouble(Double::parseDouble)
                .toArray();
    }
}
