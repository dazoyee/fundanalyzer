package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationTableQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class ValuationPresenter {

    private static final String VALUATION = "valuation";
    private static final String VALUATION_V2 = "valuation-v2";
    private static final String FRAGMENT_PREFIX = "fragments/valuation-table :: ";

    private static final String TARGET = "target";
    private static final String VALUATIONS = "valuations";

    private static final String VIEW_STOCK = "stock";
    private static final String VIEW_SUBMIT = "submit";
    private static final String VIEW_GRAHAM = "graham-index";
    private static final String VIEW_DIVIDEND = "dividend-yield";
    private static final String VIEW_INDUSTRY = "industry";

    private static final String DEFAULT_VIEW = VIEW_STOCK;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 25;

    private static final List<String> ALLOWED_NON_INDUSTRY_VIEWS = List.of(
            VIEW_STOCK, VIEW_SUBMIT, VIEW_GRAHAM, VIEW_DIVIDEND);

    private static final Map<String, List<String>> ALLOWED_SORT_BY_VIEW = Map.of(
            VIEW_STOCK, List.of(
                    "code", "name", "targetDate", "stockPrice",
                    "differenceFromSubmitDate", "submitDateRatio"),
            VIEW_SUBMIT, List.of(
                    "code", "name", "submitDate", "stockPriceOfSubmitDate",
                    "grahamIndexOfSubmitDate", "corporateValue"),
            VIEW_GRAHAM, List.of(
                    "code", "name", "grahamIndex", "grahamIndexOfSubmitDate"),
            VIEW_DIVIDEND, List.of(
                    "code", "name", "dividendYield"),
            VIEW_INDUSTRY, List.of(
                    "name", "differenceFromSubmitDate", "submitDateRatio",
                    "grahamIndex", "count"));

    private final ViewService viewService;

    public ValuationPresenter(final ViewService viewService) {
        this.viewService = viewService;
    }

    /**
     * 会社一覧を表示する
     *
     * @param target  target
     * @param message message
     * @param model   model
     * @return Index
     */
    @GetMapping("/v2/valuation")
    public String valuationView(
            @RequestParam(name = "target", required = false) final String target,
            @RequestParam(name = "message", required = false) final String message,
            final Model model) {
        if (Objects.nonNull(message)) {
            model.addAttribute("message", UriUtils.decode(message, "UTF-8"));
        }

        if (Target.ALL.toValue().equals(target)) {
            model.addAttribute(TARGET, Target.ALL.toValue());
            model.addAttribute(VALUATIONS, viewService.getAllValuationView());
        } else if (Target.FAVORITE.toValue().equals(target)) {
            model.addAttribute(TARGET, Target.FAVORITE.toValue());
            model.addAttribute(VALUATIONS, viewService.getFavoriteValuationView());
        } else if (Target.INDUSTRY.toValue().equals(target)) {
            model.addAttribute(TARGET, Target.INDUSTRY.toValue());
            model.addAttribute(VALUATIONS, viewService.getIndustryValuationView());
        } else {
            model.addAttribute(VALUATIONS, viewService.getValuationView());
        }
        return VALUATION;
    }

    /**
     * 株価評価 v3（Tailwind + htmx）。HTML 全体を返す。
     *
     * @param target    表示対象。null（メイン）/ "all" / "favorite" / "industry"
     * @param view      view 種別。stock / submit / graham-index / dividend-yield / industry
     * @param keyword   証券コード or 会社名（industry view では業種名）の partial match キーワード
     * @param page      ページ番号
     * @param size      1 ページあたり件数
     * @param sortParam ソート条件
     * @param model     model
     * @return valuation-v2 テンプレート名
     */
    @GetMapping("/v3/valuation")
    public String valuationViewV3(
            @RequestParam(name = "target", required = false) final String target,
            @RequestParam(name = "view", required = false) final String view,
            @RequestParam(name = "q", required = false) final String keyword,
            @RequestParam(name = "page", defaultValue = "0") final int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) final int size,
            @RequestParam(name = "sort", required = false) final String sortParam,
            final Model model) {
        addCommonAttributes(model, target, view, keyword, page, size, sortParam);
        return VALUATION_V2;
    }

    /**
     * 株価評価 v3 のテーブル fragment。view に応じて 5 種類の fragment を返す。
     *
     * @param target    表示対象
     * @param view      view 種別
     * @param keyword   検索キーワード
     * @param page      ページ番号
     * @param size      件数
     * @param sortParam ソート条件
     * @param model     model
     * @return fragments/valuation-table :: <view>-table
     */
    @GetMapping("/v3/valuation/table")
    public String valuationViewV3Table(
            @RequestParam(name = "target", required = false) final String target,
            @RequestParam(name = "view", required = false) final String view,
            @RequestParam(name = "q", required = false) final String keyword,
            @RequestParam(name = "page", defaultValue = "0") final int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) final int size,
            @RequestParam(name = "sort", required = false) final String sortParam,
            final Model model) {
        final String resolvedView = addCommonAttributes(model, target, view, keyword, page, size, sortParam);
        return FRAGMENT_PREFIX + resolvedView + "-table";
    }

    private String addCommonAttributes(
            final Model model,
            final String target,
            final String view,
            final String keyword,
            final int page,
            final int size,
            final String sortParam) {
        final String resolvedView = resolveView(target, view);
        final int safePage = Math.max(0, page);
        final int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        final Sort sort = parseSort(resolvedView, sortParam);
        final PageRequest pageable = PageRequest.of(safePage, safeSize, sort);
        final String resolvedSortParam = formatSort(sort);

        if (VIEW_INDUSTRY.equals(resolvedView)) {
            final IndustryValuationTableQuery query = new IndustryValuationTableQuery(keyword, pageable);
            final IndustryValuationTablePage tablePage = viewService.findIndustryValuationTable(query);
            model.addAttribute("table", tablePage);
        } else {
            final CompanyValuationTableQuery query = new CompanyValuationTableQuery(
                    target, keyword, resolvedView, pageable);
            final CompanyValuationTablePage tablePage = viewService.findCompanyValuationTable(query);
            model.addAttribute("table", tablePage);
        }

        model.addAttribute(TARGET, target);
        model.addAttribute("view", resolvedView);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortParam", resolvedSortParam);
        return resolvedView;
    }

    private static String resolveView(final String target, final String view) {
        if (Target.INDUSTRY.toValue().equals(target)) {
            return VIEW_INDUSTRY;
        }
        if (view != null && ALLOWED_NON_INDUSTRY_VIEWS.contains(view)) {
            return view;
        }
        return DEFAULT_VIEW;
    }

    private static Sort parseSort(final String view, final String sortParam) {
        final List<String> allowed = ALLOWED_SORT_BY_VIEW.getOrDefault(view, List.of("code"));
        final String defaultField = VIEW_INDUSTRY.equals(view) ? "name" : "code";
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.ASC, defaultField);
        }
        final String[] parts = sortParam.split(",");
        final String field = parts[0].trim();
        if (!allowed.contains(field)) {
            return Sort.by(Sort.Direction.ASC, defaultField);
        }
        final Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }

    private static String formatSort(final Sort sort) {
        final Sort.Order order = sort.iterator().next();
        return order.getProperty() + "," + (order.isDescending() ? "desc" : "asc");
    }
}
