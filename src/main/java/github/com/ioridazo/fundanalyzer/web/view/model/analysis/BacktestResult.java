package github.com.ioridazo.fundanalyzer.web.view.model.analysis;

import github.com.ioridazo.fundanalyzer.domain.value.Horizon;

import java.util.List;

/**
 * バックテスト集計結果の全体。
 *
 * @param horizons 期間別集計結果
 */
public record BacktestResult(List<HorizonResult> horizons) {

    /**
     * 1期間分の集計結果。
     *
     * @param horizon 評価期間
     * @param episodeCount 採用観測数
     * @param excludedDelisted 上場廃止除外数
     * @param excludedMissing 欠損除外数
     * @param buckets バケット別集計
     * @param correlation 割安度とリターン率の相関係数
     * @param industries 業種別集計
     * @param scatter 散布図点群
     */
    public record HorizonResult(
            Horizon horizon,
            long episodeCount,
            long excludedDelisted,
            long excludedMissing,
            List<BacktestBucketRow> buckets,
            Double correlation,
            List<BacktestIndustryRow> industries,
            List<BacktestScatterPoint> scatter
    ) {
    }

    /**
     * 1バケット分の集計行。
     *
     * @param label バケット表示名
     * @param count 件数
     * @param avgReturn 平均リターン率
     * @param medianReturn リターン率中央値
     * @param hitRate 勝率
     * @param avgConvergence 平均収束度
     */
    public record BacktestBucketRow(
            String label,
            long count,
            Double avgReturn,
            Double medianReturn,
            Double hitRate,
            Double avgConvergence
    ) {
    }

    /**
     * 1業種分の集計行。
     *
     * @param industryName 業種名
     * @param avgDiscountRate 平均割安度
     * @param avgReturn 平均リターン率
     * @param hitRate 勝率
     * @param count 件数
     */
    public record BacktestIndustryRow(
            String industryName,
            Double avgDiscountRate,
            Double avgReturn,
            Double hitRate,
            long count
    ) {
    }

    /**
     * 散布図の1点。
     *
     * @param discountRate 割安度
     * @param returnRate リターン率
     */
    public record BacktestScatterPoint(double discountRate, double returnRate) {
    }
}
