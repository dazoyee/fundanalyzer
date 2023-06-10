package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail.EdinetDetailViewModel;
import io.micrometer.observation.annotation.Observed;

import java.util.List;

public interface ViewEdinetUseCase {

    /**
     * メインビュー
     *
     * @return EDINETリストビュー
     */
    @Observed
    List<EdinetListViewModel> viewMain();

    /**
     * オールビュー
     *
     * @return EDINETリストビュー
     */
    @Observed
    List<EdinetListViewModel> viewAll();

    /**
     * EDINETリスト詳細ビュー
     *
     * @param inputData 提出日
     * @return EDINETリスト詳細ビュー
     */
    @Observed
    EdinetDetailViewModel viewEdinetDetail(DateInputData inputData);

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
    void updateView(DateInputData inputData);
}
