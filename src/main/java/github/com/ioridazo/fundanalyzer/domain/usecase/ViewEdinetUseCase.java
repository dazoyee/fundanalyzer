package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail.EdinetDetailViewModel;
import org.springframework.cloud.sleuth.annotation.NewSpan;

import java.util.List;

public interface ViewEdinetUseCase {

    /**
     * メインビュー
     *
     * @return EDINETリストビュー
     */
    @NewSpan
    List<EdinetListViewModel> viewMain();

    /**
     * オールビュー
     *
     * @return EDINETリストビュー
     */
    @NewSpan
    List<EdinetListViewModel> viewAll();

    /**
     * EDINETリスト詳細ビュー
     *
     * @param inputData 提出日
     * @return EDINETリスト詳細ビュー
     */
    @NewSpan
    EdinetDetailViewModel viewEdinetDetail(DateInputData inputData);

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
    void updateView(DateInputData inputData);
}
