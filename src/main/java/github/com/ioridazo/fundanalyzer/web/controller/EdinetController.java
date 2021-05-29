package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.service.EdinetService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;

@Controller
public class EdinetController {

    private static final String REDIRECT_EDINET = "redirect:/fundanalyzer/v1/edinet/list";
    private static final String REDIRECT_EDINET_DETAIL = "redirect:/fundanalyzer/v1/edinet/list/detail";

    private final EdinetService edinetService;
    private final ViewService viewService;

    public EdinetController(
            final EdinetService edinetService,
            final ViewService viewService) {
        this.edinetService = edinetService;
        this.viewService = viewService;
    }

    /**
     * 会社情報を更新する
     *
     * @param model model
     * @return EdinetList
     */
    @GetMapping("fundanalyzer/v1/company")
    public String updateCompany(final Model model) {
        edinetService.updateCompany();
        return REDIRECT_EDINET + "?message=Company is updating!";
    }

    /**
     * EDINETから提出書類一覧を取得する
     *
     * @param fromDate 提出日
     * @param toDate   提出日
     * @return EdinetList
     */
    @PostMapping("fundanalyzer/v1/edinet/list")
    public String saveEdinet(final String fromDate, final String toDate) {
        edinetService.saveEdinetList(BetweenDateInputData.of(LocalDate.parse(fromDate), LocalDate.parse(toDate)));
        return REDIRECT_EDINET;
    }

    /**
     * EDINETリストをアップデートする
     *
     * @param date 提出日
     * @return EdinetList
     */
    @PostMapping("fundanalyzer/v1/update/edinet/list")
    public String updateEdinetList(final String date) {
        viewService.updateEdinetListView(DateInputData.of(LocalDate.parse(date)));
        return REDIRECT_EDINET;
    }

    /**
     * 対象書類IDを処理対象外にする
     *
     * @param submitDate 対象提出日
     * @param documentId 書類ID
     * @return EdinetDetail
     */
    @PostMapping("fundanalyzer/v1/remove/document")
    public String removeDocument(final String submitDate, final String documentId) {
        edinetService.removeDocument(IdInputData.of(documentId));
        return REDIRECT_EDINET_DETAIL + "?submitDate=" + submitDate;
    }
}
