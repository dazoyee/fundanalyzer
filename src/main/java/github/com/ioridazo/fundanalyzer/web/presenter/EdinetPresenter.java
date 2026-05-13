package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListTableQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class EdinetPresenter {

    private static final String EDINET_V2 = "edinet-list-v2";
    private static final String EDINET_TABLE_FRAGMENT = "fragments/edinet-list-table :: table";

    private static final String TARGET = "target";

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final String DEFAULT_SORT = "submitDate,desc";
    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "submitDate", "countAll", "countTarget", "countScraped",
            "countAnalyzed", "countNotScraped", "countNotTarget");

    private final ViewService viewService;

    public EdinetPresenter(final ViewService viewService) {
        this.viewService = viewService;
    }

    /**
     * EDINET 一覧 v3（Tailwind + htmx）。HTML 全体を返す。
     *
     * @param target    表示対象。null（メイン）/ "all"
     * @param keyword   提出日検索キーワード
     * @param page      ページ番号
     * @param size      件数
     * @param sortParam ソート条件
     * @param model     model
     * @return edinet-list-v2 テンプレート名
     */
    @GetMapping("/v3/edinet-list")
    public String edinetListViewV3(
            @RequestParam(name = "target", required = false) final String target,
            @RequestParam(name = "q", required = false) final String keyword,
            @RequestParam(name = "page", defaultValue = "0") final int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) final int size,
            @RequestParam(name = "sort", defaultValue = DEFAULT_SORT) final String sortParam,
            final Model model) {
        addCommonAttributes(model, target, keyword, page, size, sortParam);
        return EDINET_V2;
    }

    /**
     * EDINET 一覧 v3 のテーブル fragment（htmx 部分更新の対象）。
     *
     * @param target    表示対象
     * @param keyword   検索キーワード
     * @param page      ページ番号
     * @param size      件数
     * @param sortParam ソート条件
     * @param model     model
     * @return fragments/edinet-list-table :: table
     */
    @GetMapping("/v3/edinet-list/table")
    public String edinetListViewV3Table(
            @RequestParam(name = "target", required = false) final String target,
            @RequestParam(name = "q", required = false) final String keyword,
            @RequestParam(name = "page", defaultValue = "0") final int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) final int size,
            @RequestParam(name = "sort", defaultValue = DEFAULT_SORT) final String sortParam,
            final Model model) {
        addCommonAttributes(model, target, keyword, page, size, sortParam);
        return EDINET_TABLE_FRAGMENT;
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
        final EdinetListTableQuery query = new EdinetListTableQuery(target, keyword, pageable);
        final EdinetListTablePage tablePage = viewService.findEdinetListTable(query);

        model.addAttribute(TARGET, target);
        model.addAttribute("keyword", keyword);
        model.addAttribute("table", tablePage);
        model.addAttribute("sortParam", sortParam);
        model.addAttribute("companyUpdated", viewService.getUpdateDate());
    }

    private static Sort parseSort(final String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "submitDate");
        }
        final String[] parts = sortParam.split(",");
        final String field = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            return Sort.by(Sort.Direction.DESC, "submitDate");
        }
        final Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
