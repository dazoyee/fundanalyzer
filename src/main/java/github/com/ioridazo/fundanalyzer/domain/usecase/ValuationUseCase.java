package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.ValuationViewModel;
import org.springframework.cloud.sleuth.annotation.NewSpan;

import java.util.List;

public interface ValuationUseCase {

    /**
     * 株価評価
     */
    @NewSpan
    int evaluate();

    /**
     * 株価評価
     *
     * @param inputData 企業コード
     */
    @NewSpan
    boolean evaluate(CodeInputData inputData);

    /**
     * メインビュー
     *
     * @return 株価評価ビュー
     */
    @NewSpan
    List<ValuationViewModel> viewValuation();

    /**
     * 企業ごとのビュー
     *
     * @return 株価評価ビュー
     */
    @NewSpan
    List<ValuationViewModel> viewValuation(CodeInputData inputData);

    /**
     * オールビュー
     *
     * @return 株価評価ビュー
     */
    @NewSpan
    List<ValuationViewModel> viewAllValuation();

    /**
     * お気に入りビュー
     *
     * @return 株価評価ビュー
     */
    @NewSpan
    List<ValuationViewModel> viewFavoriteValuation();

    /**
     * 業種ビュー
     *
     * @return 株価評価ビュー
     */
    @NewSpan
    List<ValuationViewModel> viewIndustryValuation();
}
