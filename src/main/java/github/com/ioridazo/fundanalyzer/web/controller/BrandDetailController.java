package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.service.StockService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class BrandDetailController {

    private static final String CORPORATE = "corporate";
    private static final String REDIRECT_CORPORATE = "redirect:/fundanalyzer/v1/corporate";

    private final StockService stockService;
    private final ViewService viewService;

    public BrandDetailController(
            final StockService stockService,
            final ViewService viewService) {
        this.stockService = stockService;
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
        final var brandDetail = viewService.brandDetailView(code + "0");
        model.addAttribute("corporate", brandDetail.getCorporate());
        model.addAttribute("corporateView", brandDetail.getCorporateView());
        model.addAttribute("analysisResults", brandDetail.getAnalysisResultList());
        model.addAttribute("financialStatements", brandDetail.getFinancialStatement());
        model.addAttribute("forecastStocks", brandDetail.getMinkabuList());
        model.addAttribute("stockPrices", brandDetail.getStockPriceList());
        return CORPORATE;
    }

    /**
     * 企業の株価を取得する
     *
     * @param code 会社コード
     * @return BrandDetail
     */
    @PostMapping("fundanalyzer/v1/import/stock/code")
    public String importStocks(final String code) {
        stockService.importStockPrice(code);
        return REDIRECT_CORPORATE + "/" + code.substring(0, 4);
    }
}
