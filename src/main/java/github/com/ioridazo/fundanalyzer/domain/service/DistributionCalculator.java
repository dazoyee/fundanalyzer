package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.web.view.model.analysis.DistributionResult;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.DistributionResult.HistogramBin;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.DistributionResult.IndustryStatRow;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.IndustryInput;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 分布表示用の純粋集計関数群。
 */
public final class DistributionCalculator {

    private DistributionCalculator() {
    }

    /**
     * 入力値を分布表示用の集計結果へ変換する。
     *
     * @param discountRatesPercent 割安度百分率一覧
     * @param grahamIndexes グレアム指数一覧
     * @param industries 業種別入力一覧
     * @param discountBinsPercent 割安度ヒストグラム境界
     * @param grahamBins グレアム指数ヒストグラム境界
     * @param minIndustrySize 業種別表示の最小件数
     * @return 集計結果
     */
    public static DistributionResult aggregate(
            final List<Double> discountRatesPercent,
            final List<Double> grahamIndexes,
            final List<IndustryInput> industries,
            final List<BigDecimal> discountBinsPercent,
            final List<BigDecimal> grahamBins,
            final int minIndustrySize) {
        final List<Double> nonNullGrahamIndexes = nonNullValues(grahamIndexes);
        final List<IndustryStatRow> industryRows = buildIndustryRows(industries, minIndustrySize);
        return new DistributionResult(
                histogram(discountRatesPercent, discountBinsPercent),
                histogram(nonNullGrahamIndexes, grahamBins),
                median(discountRatesPercent),
                mean(discountRatesPercent),
                median(nonNullGrahamIndexes),
                industryRows
        );
    }

    /**
     * 値列を境界に従ったヒストグラムへ変換する。
     *
     * @param values 値列
     * @param boundaries 境界値一覧
     * @return ヒストグラム
     */
    private static List<HistogramBin> histogram(final List<Double> values, final List<BigDecimal> boundaries) {
        if (boundaries == null || boundaries.isEmpty()) {
            return List.of();
        }

        final List<String> labels = histogramLabels(boundaries);
        final long[] counts = new long[labels.size()];
        for (final Double value : nonNullValues(values)) {
            counts[resolveBinIndex(value, boundaries)]++;
        }

        final List<HistogramBin> bins = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            bins.add(new HistogramBin(labels.get(i), counts[i]));
        }
        return bins;
    }

    /**
     * 値列の中央値を返す。
     *
     * @param values 値列
     * @return 中央値
     */
    private static Double median(final List<Double> values) {
        final List<Double> filtered = nonNullValues(values);
        if (filtered.isEmpty()) {
            return null;
        }

        final List<Double> sorted = filtered.stream()
                .sorted()
                .toList();
        final int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    /**
     * 値列の平均値を返す。
     *
     * @param values 値列
     * @return 平均値
     */
    private static Double mean(final List<Double> values) {
        final List<Double> filtered = nonNullValues(values);
        if (filtered.isEmpty()) {
            return null;
        }

        double sum = 0.0;
        for (final Double value : filtered) {
            sum += value;
        }
        return sum / filtered.size();
    }

    private static List<IndustryStatRow> buildIndustryRows(
            final List<IndustryInput> industries, final int minIndustrySize) {
        final List<IndustryInput> safeIndustries = industries == null ? List.of() : industries;
        return safeIndustries.stream()
                .map(industry -> new IndustryStatRow(
                        industry.industryName(),
                        median(industry.discountRates()),
                        mean(industry.discountRates()),
                        median(industry.grahamIndexes()),
                        count(industry.discountRates())))
                .filter(row -> row.count() >= minIndustrySize)
                .sorted(Comparator.comparing(
                        IndustryStatRow::discountMedian,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private static long count(final List<Double> values) {
        return values == null ? 0L : values.size();
    }

    private static int resolveBinIndex(final Double value, final List<BigDecimal> boundaries) {
        final BigDecimal decimalValue = BigDecimal.valueOf(value);
        if (decimalValue.compareTo(boundaries.get(0)) < 0) {
            return 0;
        }
        for (int i = 0; i < boundaries.size() - 1; i++) {
            final BigDecimal lower = boundaries.get(i);
            final BigDecimal upper = boundaries.get(i + 1);
            if (decimalValue.compareTo(lower) >= 0 && decimalValue.compareTo(upper) < 0) {
                return i + 1;
            }
        }
        return boundaries.size();
    }

    private static List<String> histogramLabels(final List<BigDecimal> boundaries) {
        final List<String> labels = new ArrayList<>();
        labels.add("<" + formatBoundary(boundaries.get(0)));
        for (int i = 0; i < boundaries.size() - 1; i++) {
            labels.add(formatBoundary(boundaries.get(i)) + "〜" + formatBoundary(boundaries.get(i + 1)));
        }
        labels.add(formatBoundary(boundaries.get(boundaries.size() - 1)) + "+");
        return labels;
    }

    private static String formatBoundary(final BigDecimal boundary) {
        final BigDecimal stripped = boundary.stripTrailingZeros();
        if (stripped.scale() <= 0) {
            return stripped.toPlainString();
        }
        if (stripped.scale() == 1) {
            return stripped.toPlainString();
        }
        return boundary.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static List<Double> nonNullValues(final List<Double> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null)
                .toList();
    }
}
