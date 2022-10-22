package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import java.util.Objects;

@Controller
public class ValuationPresenter {

    private static final String VALUATION = "valuation";

    private static final String TARGET = "target";
    private static final String VALUATIONS = "valuations";


    private final ViewService viewService;

    public ValuationPresenter(final ViewService viewService) {
        this.viewService = viewService;
    }

    /**
     * 会社一覧を表示する
     *
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
}
