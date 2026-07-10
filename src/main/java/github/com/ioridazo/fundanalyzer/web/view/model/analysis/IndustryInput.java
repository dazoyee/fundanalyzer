package github.com.ioridazo.fundanalyzer.web.view.model.analysis;

import java.util.List;

/**
 * 業種ごとの生値入力を表す。
 *
 * @param industryName 業種名
 * @param discountRates 割安度倍率
 * @param grahamIndexes グレアム指数
 */
public record IndustryInput(
        String industryName,
        List<Double> discountRates,
        List<Double> grahamIndexes
) {
}
