package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import java.util.Objects;

@Controller
public class EdinetPresenter {

    private static final String EDINET = "edinet";

    private final ViewService viewService;

    public EdinetPresenter(final ViewService viewService) {
        this.viewService = viewService;
    }

    /**
     * EDINETリストを表示する
     *
     * @param message message
     * @param model   model
     * @return EdinetList
     */
    @GetMapping("fundanalyzer/v2/edinet-list")
    public String edinetListView(
            @RequestParam(name = "message", required = false) final String message,
            final Model model) {
        if (Objects.nonNull(message)) {
            model.addAttribute("message", UriUtils.decode(message, "UTF-8"));
        }
        model.addAttribute("companyUpdated", viewService.getUpdateDate());
        model.addAttribute("edinetList", viewService.getEdinetListView());
        return EDINET;
    }

    /**
     * すべてのEDINETリストを表示する
     *
     * @param model model
     * @return EdinetList
     */
    @GetMapping("fundanalyzer/v2/edinet-list-all")
    public String allEdinetListView(final Model model) {
        model.addAttribute("companyUpdated", viewService.getUpdateDate());
        model.addAttribute("edinetList", viewService.getAllEdinetListView());
        return EDINET;
    }
}
