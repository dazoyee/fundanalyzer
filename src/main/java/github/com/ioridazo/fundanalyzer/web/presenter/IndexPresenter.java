package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.AnalysisResultViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.StockPriceViewModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
public class IndexPresenter {

    private static final String INDEX_V2 = "index-v2";
    private static final String INDEX_TABLE_FRAGMENT = "fragments/index-table :: table";
    private static final String INDEX_SUMMARY_CHART_FRAGMENT = "fragments/index-summary-chart :: chart";

    private static final String TARGET = "target";

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final String DEFAULT_SORT = "submitDate,desc";
    private static final String SORT_FIELD_SUBMIT_DATE = "submitDate";
    private static final String SORT_FIELD_CODE = "code";
    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            SORT_FIELD_CODE, "name", SORT_FIELD_SUBMIT_DATE, "latestCorporateValue", "discountRate", "grahamIndex");

    @Value("${app.config.view.document-type-code}")
    List<String> targetTypeCodes;

    private final ViewService viewService;

    public IndexPresenter(final ViewService viewService) {
        this.viewService = viewService;
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
        addCommonAttributes(model, target, keyword, page, size, sortParam);
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
        addCommonAttributes(model, target, keyword, page, size, sortParam);
        return INDEX_TABLE_FRAGMENT;
    }

    /**
     * 会社一覧アコーディオン用 summaryChart フラグメント（htmx 遅延ロード）。
     * 年次企業価値と提出日ベース株価の時系列データを返す。
     *
     * @param code  会社コード
     * @param model model
     * @return fragments/index-summary-chart :: chart
     */
    @GetMapping("/v3/index/{code}/summary")
    public String summaryCorporateChart(
            @PathVariable final String code,
            final Model model) {
        final CorporateDetailViewModel view = viewService.getCorporateDetailView(CodeInputData.of(code));
        final List<AnalysisResultViewModel> analysis = view.getAnalysisResultList().stream()
                .filter(vm -> targetTypeCodes.stream().anyMatch(t -> vm.documentTypeCode().equals(t)))
                .map(AnalysisResultViewModel::documentPeriod)
                .distinct()
                .map(dp -> view.getAnalysisResultList().stream()
                        .filter(vm -> targetTypeCodes.stream().anyMatch(t -> vm.documentTypeCode().equals(t)))
                        .filter(vm -> dp.equals(vm.documentPeriod()))
                        .max(Comparator.comparing(AnalysisResultViewModel::submitDate)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(AnalysisResultViewModel::documentPeriod))
                .toList();

        model.addAttribute("chartId", "summaryChart-" + code);
        model.addAttribute("analysisLabelAll", analysis.stream()
                .map(AnalysisResultViewModel::documentPeriod)
                .toList());
        model.addAttribute("analysisPointAll", analysis.stream()
                .map(AnalysisResultViewModel::corporateValue)
                .toList());

        final List<StockPriceViewModel> allStockPrices = view.getStockPriceList().stream()
                .sorted(Comparator.comparing(StockPriceViewModel::targetDate))
                .toList();
        model.addAttribute("stockPointBySubmit", analysis.stream()
                .map(vm -> allStockPrices.stream()
                        .filter(sp -> !sp.targetDate().isAfter(vm.submitDate()))
                        .max(Comparator.comparing(StockPriceViewModel::targetDate))
                        .map(StockPriceViewModel::stockPrice)
                        .orElse(null))
                .toList());

        return INDEX_SUMMARY_CHART_FRAGMENT;
    }

    private void addCommonAttributes(
            final Model model,
            final String target,
            final String keyword,
            final int page,
            final int size,
            final String sortParam) {
        final int safePage = Math.max(0, page);
        final int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        final Sort sort = parseSort(sortParam);
        final PageRequest pageable = PageRequest.of(safePage, safeSize, sort);
        final CompanyTableQuery query = new CompanyTableQuery(target, keyword, pageable);
        final CompanyTablePage tablePage = viewService.findCompanyTable(query);

        model.addAttribute(TARGET, target);
        model.addAttribute("keyword", keyword);
        model.addAttribute("table", tablePage);
        model.addAttribute("sortParam", sortParam);
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
