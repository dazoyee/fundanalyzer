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

    private final ViewService viewService;

    public EdinetDetailPresenter(final ViewService viewService) {
        this.viewService = viewService;
    }

    /**
     * EDINET処理状況 v3（Tailwind + layout-v2 継承）。
     *
     * @param submitDate 提出日（ISO_LOCAL_DATE 文字列）
     * @param model      model
     * @return edinet-list-detail-v2 テンプレート名
     */
    @GetMapping("/v3/edinet-list-detail")
    public String edinetListDetailV3(
            @RequestParam(name = "submitDate") final String submitDate,
            final Model model) {
        model.addAttribute("submitDate", submitDate);
        model.addAttribute("edinetDetail",
                viewService.getEdinetDetailView(DateInputData.of(LocalDate.parse(submitDate))));
        return "edinet-list-detail-v2";
    }
}
