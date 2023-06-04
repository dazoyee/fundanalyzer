package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.presenter.Target;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import io.micrometer.observation.annotation.Observed;

import java.util.List;

public interface ViewCorporateUseCase {

    /**
     * メインビュー
     *
     * @return 企業情報ビュー
     */
    @Observed
    List<CorporateViewModel> viewMain();

    /**
     * 四半期ビュー
     *
     * @return 企業情報ビュー
     */
    @Observed
    List<CorporateViewModel> viewQuart();

    /**
     * オールビュー
     *
     * @return 企業情報ビュー
     */
    @Observed
    List<CorporateViewModel> viewAll();

    /**
     * お気に入りビュー
     *
     * @return 企業情報ビュー
     */
    @Observed
    List<CorporateViewModel> viewFavorite();

    /**
     * 企業情報詳細ビュー
     *
     * @param inputData 企業コード
     * @return 企業情報詳細ビュー
     */
    @Observed
    CorporateDetailViewModel viewCorporateDetail(CodeInputData inputData);

    /**
     * 企業情報詳細ビュー
     *
     * @param inputData 企業コード
     * @param target    表示種別
     * @return 企業情報詳細ビュー
     */
    @Observed
    CorporateDetailViewModel viewCorporateDetail(CodeInputData inputData, Target target);

    /**
     * ビュー更新
     */
    @Observed
    void updateView();

    /**
     * ビュー更新
     *
     * @param inputData 企業コード
     */
    @Observed
    void updateView(DateInputData inputData);
}
