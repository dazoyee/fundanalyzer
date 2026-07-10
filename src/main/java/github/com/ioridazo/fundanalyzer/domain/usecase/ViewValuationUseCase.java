package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import io.micrometer.observation.annotation.Observed;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ViewValuationUseCase {

    /**
     * 企業ごとのビュー
     *
     * @return 会社評価ビュー
     */
    @Observed
    List<CompanyValuationViewModel> viewValuation(CodeInputData inputData);

    /**
     * オールビュー
     *
     * @return 会社評価ビュー
     */
    @Observed
    List<CompanyValuationViewModel> viewAllValuation();

    /**
     * グレアム指数の業種内zスコアを企業コード（4桁）別に算出する。
     *
     * <p>全対象企業を業種でグルーピングし、業種ごとにグレアム指数の平均・標準偏差を求め、
     * 各社の z=(値−業種平均)/業種σ を返す。業種内の社数が少ない（n&lt;3）・標準偏差が 0 ・
     * グレアム指数が存在しない社は算出対象外（マップに含めない）。
     *
     * @return 企業コード（4桁）→ 業種内zスコア
     */
    @Observed
    Map<String, BigDecimal> findGrahamIndustryZScore();

    /**
     * ビュー更新
     */
    @Observed
    void updateView();

    /**
     * ビュー更新
     *
     * @param inputData 提出日
     */
    @Observed
    void updateView(CodeInputData inputData);
}
