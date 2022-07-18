package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.StockPriceViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    public String corporateDetailView(
            @RequestParam(name = "code") final String code, final Model model) {
        final CorporateDetailViewModel view = viewService.getCorporateDetailView(CodeInputData.of(code));
        model.addAttribute(CORPORATE, view.getCompany());
        model.addAttribute("corporateView", view.getCorporate());
        model.addAttribute("analysisResults", view.getAnalysisResultList());
        model.addAttribute("financialStatements", view.getFinancialStatement());
        model.addAttribute("forecastStocks", view.getMinkabuList());
        model.addAttribute("stockPrices", view.getStockPriceList());

        final List<StockPriceViewModel> stockPrice = view.getStockPriceList().stream()
                .sorted(Comparator.comparing(StockPriceViewModel::getTargetDate))
                .collect(Collectors.toList());

        model.addAttribute("label30", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(30)))
                .collect(Collectors.toList()));
        model.addAttribute("point30", stockPrice.stream()
                .filter(spvm -> spvm.getTargetDate().isAfter(LocalDate.now().minusDays(30)))
                .map(StockPriceViewModel::getStockPrice)
                .collect(Collectors.toList()));

        model.addAttribute("label90", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(90)))
                .collect(Collectors.toList()));
        model.addAttribute("point90", stockPrice.stream()
                .filter(spvm -> spvm.getTargetDate().isAfter(LocalDate.now().minusDays(90)))
                .map(StockPriceViewModel::getStockPrice)
                .collect(Collectors.toList()));

        model.addAttribute("label180", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(180)))
                .collect(Collectors.toList()));
        model.addAttribute("point180", stockPrice.stream()
                .filter(spvm -> spvm.getTargetDate().isAfter(LocalDate.now().minusDays(180)))
                .map(StockPriceViewModel::getStockPrice)
                .collect(Collectors.toList()));

        model.addAttribute("label365", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(365)))
                .collect(Collectors.toList()));
        model.addAttribute("point365", stockPrice.stream()
                .filter(spvm -> spvm.getTargetDate().isAfter(LocalDate.now().minusDays(365)))
                .map(StockPriceViewModel::getStockPrice)
                .collect(Collectors.toList()));

        model.addAttribute("labelAll", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .collect(Collectors.toList()));
        model.addAttribute("pointAll", stockPrice.stream()
                .map(StockPriceViewModel::getStockPrice)
                .collect(Collectors.toList()));

        return CORPORATE;
    }
}
