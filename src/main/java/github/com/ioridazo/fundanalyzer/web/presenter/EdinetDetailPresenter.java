package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.util.Target;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

public class EdinetDetailPresenter {

    private static final String EDINET_DETAIL = "edinet-detail";

    private final ViewService viewService;

    public EdinetDetailPresenter(final ViewService viewService) {
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
        FundanalyzerLogClient.logProcessStart(Category.VIEW, Process.EDINET);
        model.addAttribute("edinetDetail", viewService.edinetDetailView(LocalDate.parse(submitDate), Target.annualSecuritiesReport()));
        FundanalyzerLogClient.logProcessEnd(Category.VIEW, Process.EDINET);
        return EDINET_DETAIL;
    }
}
