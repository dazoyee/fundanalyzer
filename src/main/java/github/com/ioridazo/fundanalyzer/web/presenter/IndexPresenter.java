package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTableQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import java.util.List;
import java.util.Objects;

@Controller
public class IndexPresenter {

    private static final String INDEX = "index";
    private static final String INDEX_V2 = "index-v2";
    private static final String INDEX_TABLE_FRAGMENT = "fragments/index-table :: table";

    private static final String TARGET = "target";
    private static final String COMPANIES = "companies";

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final String DEFAULT_SORT = "code,asc";
    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "code", "name", "submitDate", "latestCorporateValue");

    private final ViewService viewService;

    public IndexPresenter(final ViewService viewService) {
        this.viewService = viewService;
    }

    /**
     * 会社一覧を表示する
     *
     * @param message message
     * @param model   model
     * @return Index
     */
    @GetMapping("/v2/index")
    public String corporateView(
            @RequestParam(name = "target", required = false) final String target,
            @RequestParam(name = "message", required = false) final String message,
            final Model model) {
        if (Objects.nonNull(message)) {
            model.addAttribute("message", UriUtils.decode(message, "UTF-8"));
        }

        if (Target.QUART.toValue().equals(target)) {
            model.addAttribute(TARGET, Target.QUART.toValue());
            model.addAttribute(COMPANIES, viewService.getQuartCorporateView());
        } else if (Target.ALL.toValue().equals(target)) {
            model.addAttribute(TARGET, Target.ALL.toValue());
            model.addAttribute(COMPANIES, viewService.getAllCorporateView());
        } else if (Target.FAVORITE.toValue().equals(target)) {
            model.addAttribute(TARGET, Target.FAVORITE.toValue());
            model.addAttribute(COMPANIES, viewService.getFavoriteCorporateView());
        } else {
            model.addAttribute(COMPANIES, viewService.getCorporateView());
        }
        return INDEX;
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
            return Sort.by(Sort.Direction.ASC, "code");
        }
        final String[] parts = sortParam.split(",");
        final String field = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            return Sort.by(Sort.Direction.ASC, "code");
        }
        final Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
