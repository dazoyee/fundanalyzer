package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.view.model.analysis.DistributionResult;
import io.micrometer.observation.annotation.Observed;

/**
 * 分布集計結果の参照ユースケース。
 */
public interface DistributionUseCase {

    /**
     * 分布集計結果を返す。
     *
     * @return 分布集計結果
     */
    @Observed
    DistributionResult distribution();
}
