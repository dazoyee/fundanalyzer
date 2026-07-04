package github.com.ioridazo.fundanalyzer.web.view.model.analysis;

import java.util.List;

/**
 * 分布集計結果の全体を表す。
 *
 * @param discountHistogram 割安度ヒストグラム
 * @param grahamHistogram グレアム指数ヒストグラム
 * @param discountMedian 割安度中央値
 * @param discountMean 割安度平均値
 * @param grahamMedian グレアム指数中央値
 * @param industries 業種別集計
 */
public record DistributionResult(
        List<HistogramBin> discountHistogram,
        List<HistogramBin> grahamHistogram,
        Double discountMedian,
        Double discountMean,
        Double grahamMedian,
        List<IndustryStatRow> industries
) {

    /**
     * ヒストグラムの1区間を表す。
     *
     * @param label 区間表示名
     * @param count 件数
     */
    public record HistogramBin(String label, long count) {
    }

    /**
     * 業種別集計の1行を表す。
     *
     * @param industryName 業種名
     * @param discountMedian 割安度中央値
     * @param discountMean 割安度平均値
     * @param grahamMedian グレアム指数中央値
     * @param count 件数
     */
    public record IndustryStatRow(
            String industryName,
            Double discountMedian,
            Double discountMean,
            Double grahamMedian,
            long count
    ) {
    }
}
