package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class EdinetController {

    private static final String EDINET = "edinet";
    private static final String REDIRECT_EDINET = "redirect:/fundanalyzer/v1/edinet/list";

    private final DocumentService documentService;
    private final ViewService viewService;

    public EdinetController(
            final DocumentService documentService,
            final ViewService viewService) {
        this.documentService = documentService;
        this.viewService = viewService;
    }

    /**
     * EDINETリストを表示する
     *
     * @param message message
     * @param model   model
     * @return EdinetList
     */
    @GetMapping("fundanalyzer/v1/edinet/list")
    public String edinetList(@RequestParam(name = "message", required = false) final String message, final Model model) {
        model.addAttribute("message", message);
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetListview());
        return EDINET;
    }

    /**
     * EDINETから提出書類一覧を取得する
     *
     * @param fromDate 提出日
     * @param toDate   提出日
     * @return EdinetList
     */
    @PostMapping("fundanalyzer/v1/edinet/list")
    public String edinet(final String fromDate, final String toDate) {
        documentService.edinetList(fromDate, toDate);
        return REDIRECT_EDINET;
    }

    /**
     * すべてのEDINETリストを表示する
     *
     * @param model model
     * @return EdinetList
     */
    @GetMapping("fundanalyzer/v1/edinet/list/all")
    public String edinetListAll(final Model model) {
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetListViewAll());
        return EDINET;
    }

    /**
     * 処理ステータスが未完のものを初期化する
     *
     * @param model model
     * @return EdinetList
     */
    @PostMapping("fundanalyzer/v1/reset/status")
    public String resetStatus(final Model model) {
        documentService.resetForRetry();
        return REDIRECT_EDINET + "?message=updated";
    }

    /**
     * EDINETリストをアップデートする
     *
     * @return EdinetList
     */
    @PostMapping("fundanalyzer/v1/update/edinet/list")
    public String updateEdinetList(final String date) {
        viewService.updateEdinetListView("120", LocalDate.parse(date));
        return REDIRECT_EDINET;
    }
}
