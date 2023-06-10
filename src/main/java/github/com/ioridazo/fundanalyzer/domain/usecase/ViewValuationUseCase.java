package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationViewModel;
import io.micrometer.observation.annotation.Observed;

import java.util.List;

public interface ViewValuationUseCase {

    /**
     * メインビュー
     *
     * @return 会社評価ビュー
     */
    @Observed
    List<CompanyValuationViewModel> viewValuation();

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
     * お気に入りビュー
     *
     * @return 会社評価ビュー
     */
    @Observed
    List<CompanyValuationViewModel> viewFavoriteValuation();

    /**
     * 業種ビュー
     *
     * @return 会社評価ビュー
     */
    @Observed
    List<IndustryValuationViewModel> viewIndustryValuation();

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
