package github.com.ioridazo.fundanalyzer.web.view.model.analysis;

import github.com.ioridazo.fundanalyzer.domain.value.Horizon;

import java.math.BigDecimal;

/**
 * バックテスト集計に採用された1観測分の入力値。
 *
 * @param horizon 評価期間
 * @param discountRate 割安度
 * @param industryName 業種名
 * @param returnRate リターン率
 * @param convergence 企業価値収束度
 */
public record EpisodeOutcome(
        Horizon horizon,
        BigDecimal discountRate,
        String industryName,
        double returnRate,
        Double convergence
) {
}
