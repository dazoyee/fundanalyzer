package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import java.util.Objects;

@Controller
public class IndexPresenter {

    private static final String INDEX = "index";

    private static final String TARGET_QUART = "quart";
    private static final String TARGET_ALL = "all";
    private static final String TARGET_FAVORITE = "favorite";

    private static final String TARGET = "target";
    private static final String COMPANIES = "companies";


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
    @GetMapping("fundanalyzer/v2/index")
    public String corporateView(
            @RequestParam(name = "target", required = false) final String target,
            @RequestParam(name = "message", required = false) final String message,
            final Model model) {
        if (Objects.nonNull(message)) {
            model.addAttribute("message", UriUtils.decode(message, "UTF-8"));
        }

        if (TARGET_QUART.equals(target)) {
            model.addAttribute(TARGET, TARGET_QUART);
            model.addAttribute(COMPANIES, viewService.getQuartCorporateView());
        } else if (TARGET_ALL.equals(target)) {
            model.addAttribute(TARGET, TARGET_ALL);
            model.addAttribute(COMPANIES, viewService.getAllCorporateView());
        } else if (TARGET_FAVORITE.equals(target)) {
            model.addAttribute(TARGET, TARGET_FAVORITE);
            model.addAttribute(COMPANIES, viewService.getFavoriteCorporateView());
        } else {
            model.addAttribute(COMPANIES, viewService.getCorporateView());
        }
        return INDEX;
    }
}
