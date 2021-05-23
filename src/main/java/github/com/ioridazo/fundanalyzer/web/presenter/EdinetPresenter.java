package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EdinetPresenter {

    private static final String EDINET = "edinet";

    private final ViewService viewService;

    public EdinetPresenter(final ViewService viewService) {
        this.viewService = viewService;
    }

    // TODO URL update

    /**
     * EDINETリストを表示する
     *
     * @param message message
     * @param model   model
     * @return EdinetList
     */
    @GetMapping("fundanalyzer/v1/edinet/list")
    public String edinetListView(@RequestParam(name = "message", required = false) final String message, final Model model) {
        FundanalyzerLogClient.logProcessStart(Category.VIEW, Process.EDINET);
        model.addAttribute("message", message);
        model.addAttribute("companyUpdated", viewService.getUpdateDate());
        model.addAttribute("edinetList", viewService.getEdinetListView());
        FundanalyzerLogClient.logProcessEnd(Category.VIEW, Process.EDINET);
        return EDINET;
    }

    /**
     * すべてのEDINETリストを表示する
     *
     * @param model model
     * @return EdinetList
     */
    @GetMapping("fundanalyzer/v1/edinet/list/all")
    public String allEdinetListView(final Model model) {
        FundanalyzerLogClient.logProcessStart(Category.VIEW, Process.EDINET);
        model.addAttribute("companyUpdated", viewService.getUpdateDate());
        model.addAttribute("edinetList", viewService.getAllEdinetListView());
        FundanalyzerLogClient.logProcessEnd(Category.VIEW, Process.EDINET);
        return EDINET;
    }
}
