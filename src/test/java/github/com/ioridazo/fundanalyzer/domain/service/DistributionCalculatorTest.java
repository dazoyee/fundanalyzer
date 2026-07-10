package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.web.view.model.analysis.DistributionResult;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.DistributionResult.HistogramBin;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.IndustryInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DistributionCalculator} のテスト。
 */
@DisplayName("DistributionCalculatorのテスト")
class DistributionCalculatorTest {

    private static final double DELTA = 1.0e-9;

    @Nested
    @DisplayName("histogramのテスト")
    class HistogramTest {

        private final List<BigDecimal> grahamBoundaries = List.of(
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(22.5),
                BigDecimal.valueOf(70)
        );

        @ParameterizedTest
        @CsvSource({
                "9.9, 0, <10",
                "10.0, 1, 10〜15",
                "14.9, 1, 10〜15",
                "15.0, 2, 15〜22.5",
                "22.4, 2, 15〜22.5",
                "22.5, 3, 22.5〜70",
                "69.9, 3, 22.5〜70",
                "70.0, 4, 70+",
                "88.8, 4, 70+"
        })
        @DisplayName("境界値ごとの入力→対応するビンへ集計")
        void boundaryCases(final double value, final int expectedIndex, final String expectedLabel) {
            final List<HistogramBin> actual = invokeHistogram(List.of(value), grahamBoundaries);

            assertAll(
                    () -> assertEquals(5, actual.size()),
                    () -> assertEquals(expectedLabel, actual.get(expectedIndex).label()),
                    () -> assertEquals(1L, actual.get(expectedIndex).count()),
                    () -> assertEquals(0L, sumOtherBins(actual, expectedIndex))
            );
        }

        @Test
        @DisplayName("空入力→全ビン0件")
        void emptyValues() {
            final List<HistogramBin> actual = invokeHistogram(List.of(), grahamBoundaries);

            assertAll(
                    () -> assertEquals(List.of("<10", "10〜15", "15〜22.5", "22.5〜70", "70+"),
                            actual.stream().map(HistogramBin::label).toList()),
                    () -> assertTrue(actual.stream().allMatch(bin -> bin.count() == 0L))
            );
        }

        @Test
        @DisplayName("null要素を含む入力→nullを無視して集計")
        void nullElements() {
            final List<HistogramBin> actual = invokeHistogram(Arrays.asList(null, 10.0, null, 70.0), grahamBoundaries);

            assertAll(
                    () -> assertEquals(1L, actual.get(1).count()),
                    () -> assertEquals(1L, actual.get(4).count())
            );
        }
    }

    @Nested
    @DisplayName("medianのテスト")
    class MedianTest {

        @Test
        @DisplayName("奇数件→中央の値")
        void oddValues() {
            final Double actual = invokeMedian(List.of(3.0, 1.0, 2.0));

            assertEquals(2.0, actual, DELTA);
        }

        @Test
        @DisplayName("偶数件→中央2値の平均")
        void evenValues() {
            final Double actual = invokeMedian(List.of(4.0, 1.0, 3.0, 2.0));

            assertEquals(2.5, actual, DELTA);
        }

        @Test
        @DisplayName("空入力→null")
        void emptyValues() {
            final Double actual = invokeMedian(List.of());

            assertNull(actual);
        }

        @Test
        @DisplayName("nullのみの入力→null")
        void onlyNulls() {
            final Double actual = invokeMedian(Arrays.asList(null, null));

            assertNull(actual);
        }
    }

    @Nested
    @DisplayName("meanのテスト")
    class MeanTest {

        @ParameterizedTest
        @ValueSource(strings = {"1.0,2.0,3.0", "1.5,2.5"})
        @DisplayName("通常入力→平均値")
        void normalValues(final String csvValues) {
            final List<Double> values = parse(csvValues);

            final Double actual = invokeMean(values);

            assertEquals(values.stream().mapToDouble(Double::doubleValue).average().orElseThrow(), actual, DELTA);
        }

        @Test
        @DisplayName("空入力→null")
        void emptyValues() {
            final Double actual = invokeMean(List.of());

            assertNull(actual);
        }
    }

    @Nested
    @DisplayName("aggregateのテスト")
    class AggregateTest {

        private final List<BigDecimal> discountBoundaries = List.of(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(150),
                BigDecimal.valueOf(300)
        );
        private final List<BigDecimal> grahamBoundaries = List.of(
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(22.5),
                BigDecimal.valueOf(70)
        );

        @Test
        @DisplayName("業種入力あり→集計結果を返す")
        void aggregateValues() {
            final DistributionResult actual = DistributionCalculator.aggregate(
                    List.of(90.0, 100.0, 120.0, 151.0, 300.0),
                    Arrays.asList(null, 9.0, 10.0, 22.5, 70.0),
                    List.of(
                            new IndustryInput("Retail", List.of(1.2, 1.4), Arrays.asList(20.0, null)),
                            new IndustryInput("Tech", List.of(2.0, 2.4, 2.2), Arrays.asList(30.0, null, 10.0)),
                            new IndustryInput("Small", List.of(0.8), List.of(5.0))
                    ),
                    discountBoundaries,
                    grahamBoundaries,
                    2
            );

            assertAll(
                    () -> assertEquals(List.of("<100", "100〜120", "120〜150", "150〜300", "300+"),
                            actual.discountHistogram().stream().map(HistogramBin::label).toList()),
                    () -> assertEquals(List.of(1L, 1L, 1L, 1L, 1L),
                            actual.discountHistogram().stream().map(HistogramBin::count).toList()),
                    () -> assertEquals(List.of("<10", "10〜15", "15〜22.5", "22.5〜70", "70+"),
                            actual.grahamHistogram().stream().map(HistogramBin::label).toList()),
                    () -> assertEquals(List.of(1L, 1L, 0L, 1L, 1L),
                            actual.grahamHistogram().stream().map(HistogramBin::count).toList()),
                    () -> assertEquals(120.0, actual.discountMedian(), DELTA),
                    () -> assertEquals(152.2, actual.discountMean(), DELTA),
                    () -> assertEquals(16.25, actual.grahamMedian(), DELTA),
                    () -> assertEquals(2, actual.industries().size()),
                    () -> assertEquals("Tech", actual.industries().get(0).industryName()),
                    () -> assertEquals(2.2, actual.industries().get(0).discountMedian(), DELTA),
                    () -> assertEquals(2.2, actual.industries().get(0).discountMean(), DELTA),
                    () -> assertEquals(20.0, actual.industries().get(0).grahamMedian(), DELTA),
                    () -> assertEquals(3L, actual.industries().get(0).count()),
                    () -> assertEquals("Retail", actual.industries().get(1).industryName()),
                    () -> assertEquals(1.3, actual.industries().get(1).discountMedian(), DELTA),
                    () -> assertEquals(1.3, actual.industries().get(1).discountMean(), DELTA),
                    () -> assertEquals(20.0, actual.industries().get(1).grahamMedian(), DELTA),
                    () -> assertEquals(2L, actual.industries().get(1).count())
            );
        }

        @Test
        @DisplayName("完全空入力→空集計を返す")
        void emptyInputs() {
            final DistributionResult actual = assertDoesNotThrow(() -> DistributionCalculator.aggregate(
                    List.of(),
                    List.of(),
                    List.of(),
                    discountBoundaries,
                    grahamBoundaries,
                    2
            ));

            assertAll(
                    () -> assertNull(actual.discountMedian()),
                    () -> assertNull(actual.discountMean()),
                    () -> assertNull(actual.grahamMedian()),
                    () -> assertEquals(List.of(0L, 0L, 0L, 0L, 0L),
                            actual.discountHistogram().stream().map(HistogramBin::count).toList()),
                    () -> assertEquals(List.of(0L, 0L, 0L, 0L, 0L),
                            actual.grahamHistogram().stream().map(HistogramBin::count).toList()),
                    () -> assertTrue(actual.industries().isEmpty())
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static List<HistogramBin> invokeHistogram(final List<Double> values, final List<BigDecimal> boundaries) {
        return (List<HistogramBin>) invokePrivateStaticMethod("histogram",
                new Class<?>[]{List.class, List.class},
                values,
                boundaries);
    }

    private static Double invokeMedian(final List<Double> values) {
        return (Double) invokePrivateStaticMethod("median", new Class<?>[]{List.class}, values);
    }

    private static Double invokeMean(final List<Double> values) {
        return (Double) invokePrivateStaticMethod("mean", new Class<?>[]{List.class}, values);
    }

    private static Object invokePrivateStaticMethod(
            final String methodName,
            final Class<?>[] parameterTypes,
            final Object... args) {
        try {
            final Method method = DistributionCalculator.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (final NoSuchMethodException | IllegalAccessException exception) {
            throw new AssertionError(exception);
        } catch (final InvocationTargetException exception) {
            throw new AssertionError(exception.getCause());
        }
    }

    private static long sumOtherBins(final List<HistogramBin> bins, final int expectedIndex) {
        long sum = 0L;
        for (int i = 0; i < bins.size(); i++) {
            if (i != expectedIndex) {
                sum += bins.get(i).count();
            }
        }
        return sum;
    }

    private static List<Double> parse(final String csvValues) {
        return List.of(csvValues.split(",")).stream()
                .map(String::trim)
                .map(Double::parseDouble)
                .toList();
    }
}
