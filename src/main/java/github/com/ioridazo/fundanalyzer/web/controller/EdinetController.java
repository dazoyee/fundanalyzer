package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.util.Target;
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
        FundanalyzerLogClient.logProcessStart(Category.VIEW, Process.EDINET);
        model.addAttribute("message", message);
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetListview());
        FundanalyzerLogClient.logProcessEnd(Category.VIEW, Process.EDINET);
        return EDINET;
    }

    /**
     * 会社情報を更新する
     *
     * @param model model
     * @return EdinetList
     */
    @GetMapping("fundanalyzer/v1/company")
    public String company(final Model model) {
        FundanalyzerLogClient.logProcessStart(Category.DOCUMENT, Process.COMPANY);
        documentService.downloadCompanyInfo();
        FundanalyzerLogClient.logProcessEnd(Category.DOCUMENT, Process.COMPANY);
        return REDIRECT_EDINET + "?message=Company is updated!";
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
        FundanalyzerLogClient.logProcessStart(Category.DOCUMENT, Process.EDINET);
        documentService.edinetList(fromDate, toDate);
        FundanalyzerLogClient.logProcessEnd(Category.DOCUMENT, Process.EDINET);
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
        FundanalyzerLogClient.logProcessStart(Category.VIEW, Process.EDINET);
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetListViewAll());
        FundanalyzerLogClient.logProcessEnd(Category.VIEW, Process.EDINET);
        return EDINET;
    }

    /**
     * 処理ステータスが未完のものを初期化する
     *
     * @param model model
     * @return EdinetList
     */
    /*
    @PostMapping("fundanalyzer/v1/reset/status")
    public String resetStatus(final Model model) {
        documentService.resetForRetry();
        return REDIRECT_EDINET + "?message=updated";
    }
     */

    /**
     * EDINETリストをアップデートする
     *
     * @param date 提出日
     * @return EdinetList
     */
    @PostMapping("fundanalyzer/v1/update/edinet/list")
    public String updateEdinetList(final String date) {
        FundanalyzerLogClient.logProcessStart(Category.DOCUMENT, Process.UPDATE);
        viewService.updateEdinetListView(LocalDate.parse(date), Target.annualSecuritiesReport());
        FundanalyzerLogClient.logProcessEnd(Category.DOCUMENT, Process.UPDATE);
        return REDIRECT_EDINET;
    }
}
