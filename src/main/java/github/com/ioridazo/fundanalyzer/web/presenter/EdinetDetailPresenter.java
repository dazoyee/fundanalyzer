package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
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
    @GetMapping("fundanalyzer/v2/edinet-list-detail")
    public String edinetListDetail(@RequestParam(name = "submitDate") final String submitDate, final Model model) {
        model.addAttribute("edinetDetail", viewService.getEdinetDetailView(DateInputData.of(LocalDate.parse(submitDate))));
        return EDINET_DETAIL;
    }
}
