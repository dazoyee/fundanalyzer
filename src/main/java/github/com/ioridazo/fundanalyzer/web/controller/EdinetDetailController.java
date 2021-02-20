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
public class EdinetDetailController {

    private static final String EDINET_DETAIL = "edinet-detail";
    private static final String REDIRECT_EDINET_DETAIL = "redirect:/fundanalyzer/v1/edinet/list/detail";

    private final DocumentService documentService;
    private final ViewService viewService;

    public EdinetDetailController(
            final DocumentService documentService,
            final ViewService viewService) {
        this.documentService = documentService;
        this.viewService = viewService;
    }

    /**
     * EDINET処理状況を表示する
     *
     * @param submitDate 提出日
     * @param model      model
     * @return EdinetDetail
     */
    @GetMapping("fundanalyzer/v1/edinet/list/detail")
    public String edinetListDetail(@RequestParam(name = "submitDate") final String submitDate, final Model model) {
        model.addAttribute("edinetDetail", viewService.edinetDetailView(LocalDate.parse(submitDate)));
        return EDINET_DETAIL;
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
        documentService.removeDocument(documentId);
        return REDIRECT_EDINET_DETAIL + "?submitDate=" + submitDate;
    }
}
