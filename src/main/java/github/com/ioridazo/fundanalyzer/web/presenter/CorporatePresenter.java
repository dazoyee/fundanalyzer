package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CorporatePresenter {

    private static final String CORPORATE = "corporate";

    private final ViewService viewService;

    public CorporatePresenter(final ViewService viewService) {
        this.viewService = viewService;
    }

    /**
     * 銘柄詳細を表示する
     *
     * @param code  会社コード
     * @param model model
     * @return BrandDetail
     */
    @GetMapping("fundanalyzer/v2/corporate")
    public String corporateDetailView(@RequestParam(name = "code") final String code, final Model model) {
        final CorporateDetailViewModel view = viewService.getCorporateDetailView(CodeInputData.of(code));
        model.addAttribute("corporate", view.getCompany());
        model.addAttribute("corporateView", view.getCorporate());
        model.addAttribute("analysisResults", view.getAnalysisResultList());
        model.addAttribute("financialStatements", view.getFinancialStatement());
        model.addAttribute("forecastStocks", view.getMinkabuList());
        model.addAttribute("stockPrices", view.getStockPriceList());
        return CORPORATE;
    }
}
