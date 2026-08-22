package github.com.ioridazo.fundanalyzer.web.presenter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.usecase.SystemEventUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewFilterSettingUseCase;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerBadDataException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.ViewFilterSettingInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.AnalysisResultViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.StockPriceViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.index.SystemEventSummaryViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
public class IndexPresenter {

    private static final Logger log = LoggerFactory.getLogger(IndexPresenter.class);

    private static final String INDEX_V2 = "index-v2";
    private static final String INDEX_TABLE_FRAGMENT = "fragments/index-table :: table";
    private static final String INDEX_SUMMARY_CHART_FRAGMENT = "fragments/index-summary-chart :: chart";
    private static final String INDEX_FAVORITE_BUTTON_FRAGMENT = "fragments/index-table :: favorite-button";
    private static final String INDEX_STAR_BUTTON_FRAGMENT = "fragments/index-table :: star-button";
    private static final String INDEX_FILTER_SETTING_FRAGMENT = "fragments/index-filter-setting :: panel";

    private static final String TARGET = "target";

    private static final int MAX_PAGE_SIZE = 100;
    private static final int RECENT_SYSTEM_EVENT_LIMIT = 20;
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final String DEFAULT_SORT = "submitDate,desc";
    private static final String SORT_FIELD_SUBMIT_DATE = "submitDate";
    private static final String SORT_FIELD_CODE = "code";
    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            SORT_FIELD_CODE, "name", SORT_FIELD_SUBMIT_DATE, "latestCorporateValue", "discountRate", "grahamIndex");

    @Value("${app.config.view.latest-document-type-code}")
    private List<String> targetTypeCodes;

    private final ViewService viewService;
    private final AnalysisService analysisService;
    private final SystemEventUseCase systemEventUseCase;
    private final ViewFilterSettingUseCase viewFilterSettingUseCase;
    private final ObjectMapper objectMapper;

    public IndexPresenter(
            final ViewService viewService,
            final AnalysisService analysisService,
            final SystemEventUseCase systemEventUseCase,
            final ViewFilterSettingUseCase viewFilterSettingUseCase,
            final ObjectMapper objectMapper) {
        this.viewService = viewService;
        this.analysisService = analysisService;
        this.systemEventUseCase = systemEventUseCase;
        this.viewFilterSettingUseCase = viewFilterSettingUseCase;
        this.objectMapper = objectMapper;
    }

    /**
     * 会社一覧 v3（Tailwind + htmx）。HTML 全体を返す。
     *
     * @param target    表示対象の種別。null（メイン）/ "quart" / "all" / "favorite"
     * @param keyword   証券コードまたは会社名の partial match キーワード
     * @param page      ページ番号（0 始まり）
     * @param size      1 ページあたりの件数
     * @param sortParam ソート条件（"field,asc" または "field,desc"）
     * @param model     model
     * @return index-v2 テンプレート名
     */
    @GetMapping("/v3/index")
    public String corporateViewV3(
            @RequestParam(name = "target", required = false) final String target,
            @RequestParam(name = "q", required = false) final String keyword,
            @RequestParam(name = "page", defaultValue = "0") final int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) final int size,
            @RequestParam(name = "sort", defaultValue = DEFAULT_SORT) final String sortParam,
            final Model model) {
        addCommonAttributes(model, target, keyword, page, size, sortParam, true);
        return INDEX_V2;
    }

    /**
     * 会社一覧 v3 のテーブル fragment（htmx 部分更新の対象）。
     *
     * @param target    表示対象の種別
     * @param keyword   検索キーワード
     * @param page      ページ番号
     * @param size      1 ページあたりの件数
     * @param sortParam ソート条件
     * @param model     model
     * @return fragments/index-table :: table
     */
    @GetMapping("/v3/index/table")
    public String corporateViewV3Table(
            @RequestParam(name = "target", required = false) final String target,
            @RequestParam(name = "q", required = false) final String keyword,
            @RequestParam(name = "page", defaultValue = "0") final int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) final int size,
            @RequestParam(name = "sort", defaultValue = DEFAULT_SORT) final String sortParam,
            final Model model) {
        addCommonAttributes(model, target, keyword, page, size, sortParam, false);
        return INDEX_TABLE_FRAGMENT;
    }

    @GetMapping("/v3/index/filter-setting")
    public String filterSetting(final Model model) {
        addFilterSettingAttributes(model);
        return INDEX_FILTER_SETTING_FRAGMENT;
    }

    @PostMapping("/v3/index/filter-setting")
    public String updateFilterSetting(
            @RequestParam(name = "discountRate") final String discountRate,
            @RequestParam(name = "outlierOfStandardDeviation") final String outlierOfStandardDeviation,
            @RequestParam(name = "coefficientOfVariation") final String coefficientOfVariation,
            @RequestParam(name = "diffForecastStock") final String diffForecastStock,
            @RequestParam(name = "corporateSize") final String corporateSize,
            final Model model) {
        final ViewFilterSettingInputData inputData = new ViewFilterSettingInputData(
                discountRate,
                outlierOfStandardDeviation,
                coefficientOfVariation,
                diffForecastStock,
                corporateSize
        );
        try {
            viewFilterSettingUseCase.updateSetting(inputData);
            addFilterSettingAttributes(model);
            model.addAttribute("successMessage", "フィルタ設定を更新しました。");
        } catch (FundanalyzerBadDataException e) {
            addFilterSettingAttributes(model, inputData);
            model.addAttribute("errorMessage", e.getMessage());
        }
        return INDEX_FILTER_SETTING_FRAGMENT;
    }

    /**
     * 会社一覧アコーディオン用 summaryChart フラグメント（htmx 遅延ロード）。
     * 年次企業価値と提出日ベース株価の時系列データを返す。
     * 企業コードが存在しない場合は空データのフラグメントを返す。
     *
     * @param code  会社コード（4〜5桁の数値）
     * @param model Thymeleaf モデル
     * @return fragments/index-summary-chart :: chart
     */
    @GetMapping("/v3/index/{code:[0-9]{4,5}}/summary")
    public String summaryCorporateChart(
            @PathVariable final String code,
            final Model model) {
        model.addAttribute("chartId", "summaryChart-" + code);
        try {
            final github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase.SummaryChartData data =
                    viewService.getSummaryChartView(CodeInputData.of(code));
            final List<AnalysisResultViewModel> analysis = resolveLatestPerPeriod(data.analysisResults());
            final List<StockPriceViewModel> allStockPrices = data.stockPrices().stream()
                    .sorted(Comparator.comparing(StockPriceViewModel::targetDate))
                    .toList();
            populateChartModel(model, analysis, allStockPrices);
        } catch (FundanalyzerNotExistException e) {
            log.warn("summaryChart: 企業コードが存在しない: code={}", code);
            model.addAttribute("labelsJson", "[]");
            model.addAttribute("cvJson", "[]");
            model.addAttribute("stJson", "[]");
        }
        return INDEX_SUMMARY_CHART_FRAGMENT;
    }

    /**
     * 会社一覧からお気に入り登録/解除をトグルする（htmx 部分更新）。
     * トグル後の状態を反映したお気に入りボタンのフラグメントを返す。
     *
     * @param code  会社コード（4〜5桁の数値）
     * @param model model
     * @return fragments/index-table :: favorite-button
     */
    @PostMapping("/v3/index/favorite")
    public String toggleFavorite(
            @RequestParam(name = "code") final String code,
            final Model model) {
        // 一覧ビューのコードは 4 桁。company マスタは 5 桁キーのため 5 桁へ正規化して更新する。
        final boolean favorite = analysisService.updateFavoriteCompany(
                CodeInputData.of(CodeInputData.of(code).getCode5()));
        model.addAttribute("code", code);
        model.addAttribute("favorite", favorite);
        return INDEX_FAVORITE_BUTTON_FRAGMENT;
    }

    /**
     * 会社一覧から注目登録/解除をトグルする（htmx 部分更新）。
     * トグル後の状態を反映した注目ボタンのフラグメントを返す。
     *
     * @param code  会社コード（4〜5桁の数値）
     * @param model model
     * @return fragments/index-table :: star-button
     */
    @PostMapping("/v3/index/star")
    public String toggleStar(
            @RequestParam(name = "code") final String code,
            final Model model) {
        // 一覧ビューのコードは 4 桁。company マスタは 5 桁キーのため 5 桁へ正規化して更新する。
        final boolean star = analysisService.updateStarCompany(
                CodeInputData.of(CodeInputData.of(code).getCode5()));
        model.addAttribute("code", code);
        model.addAttribute("star", star);
        return INDEX_STAR_BUTTON_FRAGMENT;
    }

    /**
     * 対象書類タイプのうち、期ごとに最新提出日のレコードを1件抽出し期順に返す。
     *
     * @param view 企業詳細ビュー
     * @return 期ごとの代表分析結果リスト（期昇順）
     */
    private List<AnalysisResultViewModel> resolveLatestPerPeriod(final List<AnalysisResultViewModel> list) {
        return list.stream()
                .filter(vm -> targetTypeCodes.stream().anyMatch(t -> vm.documentTypeCode().equals(t)))
                .map(AnalysisResultViewModel::documentPeriod)
                .distinct()
                .map(dp -> list.stream()
                        .filter(vm -> targetTypeCodes.stream().anyMatch(t -> vm.documentTypeCode().equals(t)))
                        .filter(vm -> dp.equals(vm.documentPeriod()))
                        .max(Comparator.comparing(AnalysisResultViewModel::submitDate)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(AnalysisResultViewModel::documentPeriod))
                .toList();
    }

    /**
     * チャート描画用の model 属性（labelsJson / cvJson / stJson）を設定する。
     *
     * @param model          Thymeleaf モデル
     * @param analysis       期ごとの代表分析結果リスト
     * @param allStockPrices 株価リスト（日付昇順）
     */
    private void populateChartModel(
            final Model model,
            final List<AnalysisResultViewModel> analysis,
            final List<StockPriceViewModel> allStockPrices) {
        final List<String> labelList = analysis.stream()
                .map(vm -> vm.documentPeriod().toString())
                .toList();
        final List<Object> cvList = analysis.stream()
                .map(AnalysisResultViewModel::corporateValue)
                .map(v -> (Object) v)
                .toList();
        final List<Object> stList = analysis.stream()
                .map(vm -> allStockPrices.stream()
                        .filter(sp -> !sp.targetDate().isAfter(vm.submitDate()))
                        .max(Comparator.comparing(StockPriceViewModel::targetDate))
                        .map(StockPriceViewModel::stockPrice)
                        .map(v -> (Object) v)
                        .orElse(null))
                .toList();
        try {
            model.addAttribute("labelsJson", objectMapper.writeValueAsString(labelList));
            model.addAttribute("cvJson", objectMapper.writeValueAsString(cvList));
            model.addAttribute("stJson", objectMapper.writeValueAsString(stList));
        } catch (JsonProcessingException e) {
            log.error("チャートデータのJSON変換に失敗: code={}", model.getAttribute("chartId"), e);
            model.addAttribute("labelsJson", "[]");
            model.addAttribute("cvJson", "[]");
            model.addAttribute("stJson", "[]");
        }
    }

    private void addCommonAttributes(
            final Model model,
            final String target,
            final String keyword,
            final int page,
            final int size,
            final String sortParam,
            final boolean includeRecentSystemEvents) {
        final int safePage = Math.max(0, page);
        final int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        final Sort sort = parseSort(sortParam);
        final PageRequest pageable = PageRequest.of(safePage, safeSize, sort);
        final CompanyTableQuery query = new CompanyTableQuery(target, keyword, pageable);
        final CompanyTablePage tablePage = viewService.findCompanyTable(query);

        model.addAttribute(TARGET, target);
        model.addAttribute("keyword", keyword);
        model.addAttribute("table", tablePage);
        if (includeRecentSystemEvents) {
            model.addAttribute("systemEventSummary",
                    SystemEventSummaryViewModel.of(systemEventUseCase.findRecent(RECENT_SYSTEM_EVENT_LIMIT)));
        }
        model.addAttribute("sortParam", sortParam);
    }

    private void addFilterSettingAttributes(final Model model) {
        final var setting = viewFilterSettingUseCase.getSetting();
        model.addAttribute("discountRate", setting.discountRate());
        model.addAttribute("outlierOfStandardDeviation", setting.outlierOfStandardDeviation());
        model.addAttribute("coefficientOfVariation", setting.coefficientOfVariation());
        model.addAttribute("diffForecastStock", setting.diffForecastStock());
        model.addAttribute("corporateSize", setting.corporateSize());
    }

    private void addFilterSettingAttributes(final Model model, final ViewFilterSettingInputData inputData) {
        model.addAttribute("discountRate", inputData.discountRate());
        model.addAttribute("outlierOfStandardDeviation", inputData.outlierOfStandardDeviation());
        model.addAttribute("coefficientOfVariation", inputData.coefficientOfVariation());
        model.addAttribute("diffForecastStock", inputData.diffForecastStock());
        model.addAttribute("corporateSize", inputData.corporateSize());
    }

    private static Sort parseSort(final String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return defaultSort();
        }
        final String[] parts = sortParam.split(",");
        final String field = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            return defaultSort();
        }
        final Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        if (SORT_FIELD_SUBMIT_DATE.equals(field)) {
            return Sort.by(direction, SORT_FIELD_SUBMIT_DATE)
                    .and(Sort.by(Sort.Direction.DESC, SORT_FIELD_CODE));
        }
        return Sort.by(direction, field);
    }

    private static Sort defaultSort() {
        return Sort.by(Sort.Direction.DESC, SORT_FIELD_SUBMIT_DATE)
                .and(Sort.by(Sort.Direction.DESC, SORT_FIELD_CODE));
    }
}
