package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationViewModel;
import org.springframework.cloud.sleuth.annotation.NewSpan;

import java.util.List;

public interface ViewValuationUseCase {

    /**
     * メインビュー
     *
     * @return 会社評価ビュー
     */
    @NewSpan
    List<CompanyValuationViewModel> viewValuation();

    /**
     * 企業ごとのビュー
     *
     * @return 会社評価ビュー
     */
    @NewSpan
    List<CompanyValuationViewModel> viewValuation(CodeInputData inputData);

    /**
     * オールビュー
     *
     * @return 会社評価ビュー
     */
    @NewSpan
    List<CompanyValuationViewModel> viewAllValuation();

    /**
     * お気に入りビュー
     *
     * @return 会社評価ビュー
     */
    @NewSpan
    List<CompanyValuationViewModel> viewFavoriteValuation();

    /**
     * 業種ビュー
     *
     * @return 会社評価ビュー
     */
    @NewSpan
    List<IndustryValuationViewModel> viewIndustryValuation();

    /**
     * ビュー更新
     */
    @NewSpan
    void updateView();

    /**
     * ビュー更新
     *
     * @param inputData 提出日
     */
    @NewSpan
    void updateView(CodeInputData inputData);
}
