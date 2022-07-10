package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewEdinetUseCase;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.ValuationViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail.EdinetDetailViewModel;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ViewService {

    private final CompanyUseCase companyUseCase;
    private final DocumentUseCase documentUseCase;
    private final ViewCorporateUseCase viewCorporateUseCase;
    private final ViewEdinetUseCase viewEdinetUseCase;

    public ViewService(
            final CompanyUseCase companyUseCase,
            final DocumentUseCase documentUseCase,
            final ViewCorporateUseCase viewCorporateUseCase,
            final ViewEdinetUseCase viewEdinetUseCase) {
        this.companyUseCase = companyUseCase;
        this.documentUseCase = documentUseCase;
        this.viewCorporateUseCase = viewCorporateUseCase;
        this.viewEdinetUseCase = viewEdinetUseCase;
    }

    /**
     * 企業情報（メイン）
     *
     * @return 企業一覧
     */
    @NewSpan
    public List<CorporateViewModel> getCorporateView() {
        return viewCorporateUseCase.viewMain();
    }

    /**
     * 企業情報（すべて）
     *
     * @return 企業一覧
     */
    @NewSpan
    public List<CorporateViewModel> getAllCorporateView() {
        return viewCorporateUseCase.viewAll();
    }

    /**
     * 企業情報（お気に入り）
     *
     * @return 企業一覧
     */
    @NewSpan
    public List<CorporateViewModel> getFavoriteCorporateView() {
        return viewCorporateUseCase.viewFavorite();
    }

    /**
     * 企業情報（企業価値ソート）
     *
     * @return 企業一覧
     */
    @NewSpan
    public List<CorporateViewModel> getSortedCorporateView() {
        return viewCorporateUseCase.sortByDiscountRate();
    }

    /**
     * EDINETリスト（メイン）
     *
     * @return 書類状況リスト
     */
    @NewSpan
    public List<EdinetListViewModel> getEdinetListView() {
        return viewEdinetUseCase.viewMain();
    }

    /**
     * EDINETリスト（すべて）
     *
     * @return 書類状況リスト
     */
    @NewSpan
    public List<EdinetListViewModel> getAllEdinetListView() {
        return viewEdinetUseCase.viewAll();
    }

    /**
     * 企業情報更新日時
     *
     * @return 更新日時
     */
    @NewSpan
    public String getUpdateDate() {
        return companyUseCase.getUpdateDate();
    }

    /**
     * 企業詳細情報
     *
     * @return 企業詳細
     */
    @NewSpan
    public CorporateDetailViewModel getCorporateDetailView(final CodeInputData inputData) {
        return viewCorporateUseCase.viewCorporateDetail(inputData);
    }

    /**
     * EDINET詳細リスト
     *
     * @return 処理詳細情報
     */
    @NewSpan
    public EdinetDetailViewModel getEdinetDetailView(final DateInputData inputData) {
        return viewEdinetUseCase.viewEdinetDetail(inputData);
    }

    /**
     * 表示アップデート
     */
    @NewSpan
    @Async
    public void updateCorporateView() {
        // view corporate
        viewCorporateUseCase.updateView();
    }

    /**
     * 処理状況アップデート
     */
    @NewSpan
    @Async
    public void updateEdinetView() {
        // view edinet
        viewEdinetUseCase.updateView();
    }

    /**
     * EDINETリストアップデート
     *
     * @param inputData 提出日
     */
    @NewSpan
    public void updateEdinetListView(final DateInputData inputData) {
        // remove
        documentUseCase.removeDocument(inputData);
        // view edinet
        viewEdinetUseCase.updateView(inputData);
    }

    /**
     * 株価評価（メイン）
     *
     * @return 株価評価
     */
    @NewSpan
    public List<ValuationViewModel> getValuationView() {
        return viewCorporateUseCase.viewValuation();
    }

    /**
     * 株価評価（オール）
     *
     * @return 株価評価
     */
    @NewSpan
    public List<ValuationViewModel> getAllValuationView() {
        return viewCorporateUseCase.viewAllValuation();
    }

    /**
     * 株価評価（お気に入り）
     *
     * @return 株価評価
     */
    @NewSpan
    public List<ValuationViewModel> getFavoriteValuationView() {
        return viewCorporateUseCase.viewFavoriteValuation();
    }
}
