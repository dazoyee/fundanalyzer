package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
    @GetMapping("fundanalyzer/v1/corporate/{code}")
    public String brandDetail(@PathVariable final String code, final Model model) {
        FundanalyzerLogClient.logProcessStart(Category.VIEW, Process.COMPANY);
        final var brandDetail = viewService.brandDetailView(code + "0");
        model.addAttribute("corporate", brandDetail.getCorporate());
        model.addAttribute("corporateView", brandDetail.getCorporateView());
        model.addAttribute("analysisResults", brandDetail.getAnalysisResultList());
        model.addAttribute("financialStatements", brandDetail.getFinancialStatement());
        model.addAttribute("forecastStocks", brandDetail.getMinkabuList());
        model.addAttribute("stockPrices", brandDetail.getStockPriceList());
        FundanalyzerLogClient.logProcessEnd(Category.VIEW, Process.COMPANY);
        return CORPORATE;
    }
}
