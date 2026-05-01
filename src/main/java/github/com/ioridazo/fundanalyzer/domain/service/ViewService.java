package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewEdinetUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewValuationUseCase;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.presenter.Target;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail.EdinetDetailViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationViewModel;
import io.micrometer.observation.annotation.Observed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ViewService {

    private final CompanyUseCase companyUseCase;
    private final DocumentUseCase documentUseCase;
    private final ViewCorporateUseCase viewCorporateUseCase;
    private final ViewEdinetUseCase viewEdinetUseCase;
    private final ViewValuationUseCase viewValuationUseCase;

    public ViewService(
            final CompanyUseCase companyUseCase,
            final DocumentUseCase documentUseCase,
            final ViewCorporateUseCase viewCorporateUseCase,
            final ViewEdinetUseCase viewEdinetUseCase,
            final ViewValuationUseCase viewValuationUseCase) {
        this.companyUseCase = companyUseCase;
        this.documentUseCase = documentUseCase;
        this.viewValuationUseCase = viewValuationUseCase;
        this.viewCorporateUseCase = viewCorporateUseCase;
        this.viewEdinetUseCase = viewEdinetUseCase;
    }

    /**
     * 企業情報（メイン）
     *
     * @return 企業一覧
     */
    @Observed
    public List<CorporateViewModel> getCorporateView() {
        return viewCorporateUseCase.viewMain();
    }

    /**
     * 企業情報（四半期）
     *
     * @return 企業一覧
     */
    @Observed
    public List<CorporateViewModel> getQuartCorporateView() {
        return viewCorporateUseCase.viewQuart();
    }

    /**
     * 企業情報（すべて）
     *
     * @return 企業一覧
     */
    @Observed
    public List<CorporateViewModel> getAllCorporateView() {
        return viewCorporateUseCase.viewAll();
    }

    /**
     * 企業情報（お気に入り）
     *
     * @return 企業一覧
     */
    @Observed
    public List<CorporateViewModel> getFavoriteCorporateView() {
        return viewCorporateUseCase.viewFavorite();
    }

    /**
     * 会社一覧テーブルを target / keyword / pageable で絞り込んで返す。Phase 3 で導入したテーブル汎用パターンの初版。
     *
     * @param query 問い合わせ条件
     * @return 1 ページ分の会社リストとページング情報
     */
    @Observed
    public CompanyTablePage findCompanyTable(final CompanyTableQuery query) {
        final List<CorporateViewModel> all = switch (Optional.ofNullable(query.target()).orElse("")) {
            case "quart" -> getQuartCorporateView();
            case "all" -> getAllCorporateView();
            case "favorite" -> getFavoriteCorporateView();
            default -> getCorporateView();
        };

        final String keyword = Optional.ofNullable(query.keyword()).map(String::trim).orElse("");
        final List<CorporateViewModel> filtered = keyword.isEmpty()
                ? all
                : all.stream()
                        .filter(c -> containsIgnoreCase(c.getCode(), keyword)
                                || containsIgnoreCase(c.getName(), keyword))
                        .toList();

        final Pageable pageable = query.pageable();
        final List<CorporateViewModel> sorted = applySort(filtered, pageable.getSort());

        final int totalElements = sorted.size();
        final int pageSize = pageable.getPageSize();
        final int pageNumber = pageable.getPageNumber();
        final int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
        final List<CorporateViewModel> pageContent = sorted.stream()
                .skip((long) pageNumber * pageSize)
                .limit(pageSize)
                .toList();

        return new CompanyTablePage(pageContent, totalPages, totalElements, pageNumber, pageSize, pageable.getSort());
    }

    private static boolean containsIgnoreCase(final String value, final String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private static List<CorporateViewModel> applySort(final List<CorporateViewModel> source, final Sort sort) {
        if (sort.isUnsorted()) {
            return source;
        }
        Comparator<CorporateViewModel> comparator = null;
        for (final Sort.Order order : sort) {
            Comparator<CorporateViewModel> c = comparatorFor(order.getProperty());
            if (c == null) {
                continue;
            }
            if (order.isDescending()) {
                c = c.reversed();
            }
            comparator = (comparator == null) ? c : comparator.thenComparing(c);
        }
        if (comparator == null) {
            return source;
        }
        return source.stream().sorted(comparator).toList();
    }

    private static Comparator<CorporateViewModel> comparatorFor(final String property) {
        return switch (property) {
            case "code" -> Comparator.comparing(
                    CorporateViewModel::getCode, Comparator.nullsLast(Comparator.naturalOrder()));
            case "name" -> Comparator.comparing(
                    CorporateViewModel::getName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "submitDate" -> Comparator.comparing(
                    CorporateViewModel::getSubmitDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "latestCorporateValue" -> Comparator.comparing(
                    CorporateViewModel::getLatestCorporateValue, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
    }

    /**
     * EDINETリスト（メイン）
     *
     * @return 書類状況リスト
     */
    @Observed
    public List<EdinetListViewModel> getEdinetListView() {
        return viewEdinetUseCase.viewMain();
    }

    /**
     * EDINETリスト（すべて）
     *
     * @return 書類状況リスト
     */
    @Observed
    public List<EdinetListViewModel> getAllEdinetListView() {
        return viewEdinetUseCase.viewAll();
    }

    /**
     * 企業情報更新日時
     *
     * @return 更新日時
     */
    @Observed
    public String getUpdateDate() {
        return companyUseCase.getUpdateDate();
    }

    /**
     * 企業詳細情報
     *
     * @param inputData 企業コード
     * @return 企業詳細
     */
    @Observed
    public CorporateDetailViewModel getCorporateDetailView(final CodeInputData inputData) {
        return viewCorporateUseCase.viewCorporateDetail(inputData);
    }

    /**
     * 企業詳細情報
     *
     * @param inputData 企業コード
     * @param target    表示種別
     * @return 企業詳細
     */
    @Observed
    public CorporateDetailViewModel getCorporateDetailView(final CodeInputData inputData, final Target target) {
        return viewCorporateUseCase.viewCorporateDetail(inputData, target);
    }

    /**
     * EDINET詳細リスト
     *
     * @param inputData 提出日
     * @return 処理詳細情報
     */
    @Observed
    public EdinetDetailViewModel getEdinetDetailView(final DateInputData inputData) {
        return viewEdinetUseCase.viewEdinetDetail(inputData);
    }

    /**
     * 表示アップデート
     */
    @Observed
    @Async
    public void updateCorporateView() {
        // view corporate
        viewCorporateUseCase.updateView();
    }

    /**
     * 処理状況アップデート
     */
    @Observed
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
    @Observed
    public void updateEdinetListView(final DateInputData inputData) {
        // remove
        documentUseCase.removeDocument(inputData);
        // view edinet
        viewEdinetUseCase.updateView(inputData);
    }

    /**
     * 企業評価アップデート
     */
    @Observed
    @Async
    public void updateValuationView() {
        // view edinet
        viewValuationUseCase.updateView();
    }

    /**
     * 企業評価アップデート
     *
     * @param inputData 企業コード
     */
    @Observed
    @Async
    public void updateValuationView(final CodeInputData inputData) {
        // view valuation
        viewValuationUseCase.updateView(inputData);
    }

    /**
     * 株価評価（メイン）
     *
     * @return 株価評価
     */
    @Observed
    public List<CompanyValuationViewModel> getValuationView() {
        return viewValuationUseCase.viewValuation();
    }

    /**
     * 株価評価（企業ごと）
     *
     * @return 株価評価
     */
    @Observed
    public List<CompanyValuationViewModel> getValuationView(final CodeInputData inputData) {
        return viewValuationUseCase.viewValuation(inputData);
    }

    /**
     * 株価評価（オール）
     *
     * @return 株価評価
     */
    @Observed
    public List<CompanyValuationViewModel> getAllValuationView() {
        return viewValuationUseCase.viewAllValuation();
    }

    /**
     * 株価評価（お気に入り）
     *
     * @return 株価評価
     */
    @Observed
    public List<CompanyValuationViewModel> getFavoriteValuationView() {
        return viewValuationUseCase.viewFavoriteValuation();
    }

    /**
     * 株価評価（業種）
     *
     * @return 株価評価
     */
    @Observed
    public List<IndustryValuationViewModel> getIndustryValuationView() {
        return viewValuationUseCase.viewIndustryValuation();
    }
}
