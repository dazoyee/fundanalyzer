package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.value.Horizon;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.BacktestResult;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.EpisodeOutcome;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * バックテスト用の純粋集計関数群。
 */
public final class BacktestCalculator {

    private BacktestCalculator() {
    }

    /**
     * 観測値を期間別の表示用集計結果へ変換する。
     *
     * <p>結果には、採用観測が1件以上ある期間、または除外件数が1件以上ある期間のみ含める。</p>
     *
     * @param outcomes 採用観測一覧
     * @param exclusions 期間別除外件数
     * @param bucketBoundaries バケット境界
     * @return 集計結果
     */
    public static BacktestResult aggregate(
            final List<EpisodeOutcome> outcomes,
            final Map<Horizon, long[]> exclusions,
            final List<BigDecimal> bucketBoundaries) {
        final List<BacktestResult.HorizonResult> horizonResults = new ArrayList<>();
        for (final Horizon horizon : Horizon.values()) {
            final List<EpisodeOutcome> filtered = outcomes.stream()
                    .filter(outcome -> outcome.horizon() == horizon)
                    .toList();
            final long[] counts = exclusions.getOrDefault(horizon, new long[]{0L, 0L});
            final long excludedDelisted = counts.length > 0 ? counts[0] : 0L;
            final long excludedMissing = counts.length > 1 ? counts[1] : 0L;
            if (filtered.isEmpty() && excludedDelisted == 0L && excludedMissing == 0L) {
                continue;
            }

            final List<BacktestResult.BacktestBucketRow> buckets = buildBuckets(filtered, bucketBoundaries);
            final Double correlation = buildCorrelation(filtered);
            final List<BacktestResult.BacktestIndustryRow> industries = buildIndustries(filtered);
            final List<BacktestResult.BacktestScatterPoint> scatter = filtered.stream()
                    .map(outcome -> new BacktestResult.BacktestScatterPoint(
                            outcome.discountRate().doubleValue(),
                            outcome.returnRate()))
                    .toList();

            horizonResults.add(new BacktestResult.HorizonResult(
                    horizon,
                    filtered.size(),
                    excludedDelisted,
                    excludedMissing,
                    buckets,
                    correlation,
                    industries,
                    scatter));
        }
        return new BacktestResult(horizonResults);
    }

    /**
     * 値列の中央値を返す。
     *
     * @param values 値列
     * @return 中央値
     */
    static double median(final double[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }
        final double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        final int middle = sorted.length / 2;
        if (sorted.length % 2 == 1) {
            return sorted[middle];
        }
        return (sorted[middle - 1] + sorted[middle]) / 2.0;
    }

    /**
     * 2系列のピアソン相関係数を返す。
     *
     * @param x 第1系列
     * @param y 第2系列
     * @return 相関係数
     */
    static Double pearson(final double[] x, final double[] y) {
        if (x.length != y.length || x.length < 2) {
            return null;
        }

        double sumX = 0;
        double sumY = 0;
        for (int i = 0; i < x.length; i++) {
            sumX += x[i];
            sumY += y[i];
        }
        final double meanX = sumX / x.length;
        final double meanY = sumY / y.length;

        double numerator = 0;
        double sumSquareX = 0;
        double sumSquareY = 0;
        for (int i = 0; i < x.length; i++) {
            final double dx = x[i] - meanX;
            final double dy = y[i] - meanY;
            numerator += dx * dy;
            sumSquareX += dx * dx;
            sumSquareY += dy * dy;
        }

        if (sumSquareX == 0 || sumSquareY == 0) {
            return null;
        }
        return numerator / Math.sqrt(sumSquareX * sumSquareY);
    }

    /**
     * 割安度からバケット表示名を返す。
     *
     * @param rate 割安度
     * @param boundaries バケット境界
     * @return バケット表示名
     */
    static String bucketLabelOf(final BigDecimal rate, final List<BigDecimal> boundaries) {
        final int firstPercent = toPercent(boundaries.get(0));
        if (rate.compareTo(boundaries.get(0)) < 0) {
            return "<" + firstPercent + "%";
        }
        for (int i = 0; i < boundaries.size() - 1; i++) {
            final BigDecimal lower = boundaries.get(i);
            final BigDecimal upper = boundaries.get(i + 1);
            if (rate.compareTo(lower) >= 0 && rate.compareTo(upper) < 0) {
                return toPercent(lower) + "〜" + toPercent(upper) + "%";
            }
        }
        return toPercent(boundaries.get(boundaries.size() - 1)) + "%+";
    }

    private static List<BacktestResult.BacktestBucketRow> buildBuckets(
            final List<EpisodeOutcome> outcomes, final List<BigDecimal> bucketBoundaries) {
        final List<String> orderedLabels = bucketLabels(bucketBoundaries);
        final Map<String, List<EpisodeOutcome>> byBucket = new LinkedHashMap<>();
        for (final String label : orderedLabels) {
            byBucket.put(label, new ArrayList<>());
        }
        for (final EpisodeOutcome outcome : outcomes) {
            byBucket.get(bucketLabelOf(outcome.discountRate(), bucketBoundaries)).add(outcome);
        }

        final List<BacktestResult.BacktestBucketRow> buckets = new ArrayList<>();
        for (final String label : orderedLabels) {
            final List<EpisodeOutcome> bucketOutcomes = byBucket.get(label);
            if (bucketOutcomes.isEmpty()) {
                continue;
            }
            final double[] returns = bucketOutcomes.stream()
                    .mapToDouble(EpisodeOutcome::returnRate)
                    .toArray();
            final Double avgConvergence = averageNullable(bucketOutcomes.stream()
                    .map(EpisodeOutcome::convergence)
                    .filter(value -> value != null)
                    .mapToDouble(Double::doubleValue)
                    .toArray());
            buckets.add(new BacktestResult.BacktestBucketRow(
                    label,
                    bucketOutcomes.size(),
                    average(returns),
                    median(returns),
                    hitRate(bucketOutcomes),
                    avgConvergence));
        }
        return buckets;
    }

    private static Double buildCorrelation(final List<EpisodeOutcome> outcomes) {
        final double[] x = outcomes.stream()
                .map(EpisodeOutcome::discountRate)
                .mapToDouble(BigDecimal::doubleValue)
                .toArray();
        final double[] y = outcomes.stream()
                .mapToDouble(EpisodeOutcome::returnRate)
                .toArray();
        return pearson(x, y);
    }

    private static List<BacktestResult.BacktestIndustryRow> buildIndustries(final List<EpisodeOutcome> outcomes) {
        final Map<String, List<EpisodeOutcome>> byIndustry = new LinkedHashMap<>();
        for (final EpisodeOutcome outcome : outcomes) {
            byIndustry.computeIfAbsent(outcome.industryName(), key -> new ArrayList<>()).add(outcome);
        }

        return byIndustry.entrySet().stream()
                .map(entry -> {
                    final List<EpisodeOutcome> industryOutcomes = entry.getValue();
                    final double[] discountRates = industryOutcomes.stream()
                            .map(EpisodeOutcome::discountRate)
                            .mapToDouble(BigDecimal::doubleValue)
                            .toArray();
                    final double[] returns = industryOutcomes.stream()
                            .mapToDouble(EpisodeOutcome::returnRate)
                            .toArray();
                    return new BacktestResult.BacktestIndustryRow(
                            entry.getKey(),
                            average(discountRates),
                            average(returns),
                            hitRate(industryOutcomes),
                            industryOutcomes.size());
                })
                .sorted(Comparator.comparing(BacktestResult.BacktestIndustryRow::hitRate).reversed())
                .toList();
    }

    private static List<String> bucketLabels(final List<BigDecimal> boundaries) {
        final List<String> labels = new ArrayList<>();
        labels.add("<" + toPercent(boundaries.get(0)) + "%");
        for (int i = 0; i < boundaries.size() - 1; i++) {
            labels.add(toPercent(boundaries.get(i)) + "〜" + toPercent(boundaries.get(i + 1)) + "%");
        }
        labels.add(toPercent(boundaries.get(boundaries.size() - 1)) + "%+");
        return labels;
    }

    private static double average(final double[] values) {
        return Arrays.stream(values)
                .average()
                .orElse(0);
    }

    private static Double averageNullable(final double[] values) {
        if (values.length == 0) {
            return null;
        }
        return average(values);
    }

    private static double hitRate(final List<EpisodeOutcome> outcomes) {
        final long positiveCount = outcomes.stream()
                .filter(outcome -> outcome.returnRate() > 0)
                .count();
        return (double) positiveCount / outcomes.size();
    }

    private static int toPercent(final BigDecimal value) {
        return value.multiply(BigDecimal.valueOf(100)).intValue();
    }
}
