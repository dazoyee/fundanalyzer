package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.view.model.analysis.BacktestResult;
import io.micrometer.observation.annotation.Observed;

/**
 * バックテスト集計結果を取得する。
 */
public interface BacktestUseCase {

    /**
     * バックテスト集計結果。
     *
     * @return バックテスト集計結果
     */
    @Observed
    BacktestResult backtest();
}
