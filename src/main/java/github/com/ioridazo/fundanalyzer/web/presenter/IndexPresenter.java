package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class IndexPresenter {

    private static final String INDEX = "index";

    private final ViewService viewService;

    public IndexPresenter(final ViewService viewService) {
        this.viewService = viewService;
    }

    // TODO URL update

    /**
     * 会社一覧を表示する
     *
     * @param message message
     * @param model   model
     * @return Index
     */
    @GetMapping("fundanalyzer/v1/index")
    public String index(@RequestParam(name = "message", required = false) final String message, final Model model) {
        FundanalyzerLogClient.logProcessStart(Category.VIEW, Process.COMPANY);
        model.addAttribute("message", message);
        model.addAttribute("companies", viewService.corporateView());
        FundanalyzerLogClient.logProcessEnd(Category.VIEW, Process.COMPANY);
        return INDEX;
    }

    /**
     * 割安比率でソートする
     *
     * @param model model
     * @return Index
     */
    @GetMapping("fundanalyzer/v1/index/sort/discount-rate")
    public String sortedDiscountRate(final Model model) {
        FundanalyzerLogClient.logProcessStart(Category.VIEW, Process.SORT);
        model.addAttribute("companies", viewService.sortByDiscountRate());
        FundanalyzerLogClient.logProcessEnd(Category.VIEW, Process.SORT);
        return INDEX;
    }

    /**
     * すべての企業情報を表示する
     *
     * @param model model
     * @return Index
     */
    @GetMapping("fundanalyzer/v1/index/all")
    public String indexAll(final Model model) {
        FundanalyzerLogClient.logProcessStart(Category.VIEW, Process.SORT);
        model.addAttribute("companies", viewService.corporateViewAll());
        FundanalyzerLogClient.logProcessEnd(Category.VIEW, Process.SORT);
        return INDEX;
    }
}