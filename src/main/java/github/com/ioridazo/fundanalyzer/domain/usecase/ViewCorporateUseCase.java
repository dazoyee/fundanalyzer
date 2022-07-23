package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.ValuationViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import org.springframework.cloud.sleuth.annotation.NewSpan;

import java.util.List;

public interface ViewCorporateUseCase {

    /**
     * メインビュー
     *
     * @return 企業情報ビュー
     */
    @NewSpan
    List<CorporateViewModel> viewMain();

    /**
     * オールビュー
     *
     * @return 企業情報ビュー
     */
    @NewSpan
    List<CorporateViewModel> viewAll();

    /**
     * お気に入りビュー
     *
     * @return 企業情報ビュー
     */
    @NewSpan
    List<CorporateViewModel> viewFavorite();

    /**
     * 割安度ソート
     *
     * @return 企業情報ビュー
     */
    @NewSpan
    List<CorporateViewModel> sortByDiscountRate();

    /**
     * 企業情報詳細ビュー
     *
     * @param inputData 企業コード
     * @return 企業情報詳細ビュー
     */
    @NewSpan
    CorporateDetailViewModel viewCorporateDetail(CodeInputData inputData);

    /**
     * ビュー更新
     */
    @NewSpan
    void updateView();

    /**
     * ビュー更新
     *
     * @param inputData 企業コード
     */
    @NewSpan
    void updateView(DateInputData inputData);

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
}
