package github.com.ioridazo.fundanalyzer.web.view.model.valuation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 業種別評価ビュー
 *
 * @param name                     業種名
 * @param differenceFromSubmitDate 提出日との差
 * @param submitDateRatio          提出日比率
 * @param grahamIndex              グレアム指数
 * @param count                    件数
 */
public record IndustryValuationViewModel(
        String name,
        BigDecimal differenceFromSubmitDate,
        BigDecimal submitDateRatio,
        BigDecimal grahamIndex,
        Integer count) {

    /**
     * double 値を四捨五入で BigDecimal 化したうえでビューを生成する静的ファクトリ
     *
     * @param industryName             業種名
     * @param differenceFromSubmitDate 提出日との差
     * @param submitDateRatio          提出日比率
     * @param grahamIndex              グレアム指数
     * @param count                    件数
     * @return IndustryValuationViewModel
     */
    public static IndustryValuationViewModel of(
            final String industryName,
            final double differenceFromSubmitDate,
            final double submitDateRatio,
            final double grahamIndex,
            final int count) {
        return new IndustryValuationViewModel(
                industryName,
                BigDecimal.valueOf(differenceFromSubmitDate).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(submitDateRatio).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(grahamIndex).setScale(2, RoundingMode.HALF_UP),
                count
        );
    }

    public String getName() {
        return name;
    }

    public BigDecimal getDifferenceFromSubmitDate() {
        return differenceFromSubmitDate;
    }

    public BigDecimal getSubmitDateRatio() {
        return submitDateRatio;
    }

    public BigDecimal getGrahamIndex() {
        return grahamIndex;
    }

    public Integer getCount() {
        return count;
    }
}
