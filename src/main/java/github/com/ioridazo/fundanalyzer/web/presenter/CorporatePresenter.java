package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.AnalysisResultViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.MinkabuViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.StockPriceViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class CorporatePresenter {

    private static final String CORPORATE = "corporate";

    private final ViewService viewService;

    @Value("${app.config.view.document-type-code}")
    List<String> targetTypeCodes;

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
    @GetMapping("/v2/corporate")
    public String corporateDetailView(
            @RequestParam(name = "code") final String code,
            @RequestParam(name = "target", required = false) final String target,
            final Model model) {
        CorporateDetailViewModel view;

        if (Objects.isNull(target)) {
            view = viewService.getCorporateDetailView(CodeInputData.of(code));
        } else {
            view = viewService.getCorporateDetailView(CodeInputData.of(code), Target.fromValue(target));
            model.addAttribute("target", Target.fromValue(target).toValue());
        }

        model.addAttribute(CORPORATE, view.getCompany());
        model.addAttribute("backwardCode", view.getBackwardCode());
        model.addAttribute("forwardCode", view.getForwardCode());
        model.addAttribute("corporateView", view.getCorporate());
        setAnalysisView(view, model);
        model.addAttribute("financialStatements", view.getFinancialStatement());
        setForecastStock(view, model);
        setStockPriceView(view, model);
        setValuationView(viewService.getValuationView(CodeInputData.of(code)), model);
        return CORPORATE;
    }

    private void setAnalysisView(final CorporateDetailViewModel view, final Model model) {
        model.addAttribute("analysisResults", view.getAnalysisResultList());

        final List<AnalysisResultViewModel> analysis = view.getAnalysisResultList().stream()
                .filter(vm -> targetTypeCodes.stream().anyMatch(t -> vm.getDocumentTypeCode().equals(t)))
                .map(AnalysisResultViewModel::getDocumentPeriod)
                .distinct()
                // 最新の企業価値を取得する
                .map(dp -> view.getAnalysisResultList().stream()
                        .filter(vm -> targetTypeCodes.stream().anyMatch(t -> vm.getDocumentTypeCode().equals(t)))
                        .filter(viewModel -> dp.equals(viewModel.getDocumentPeriod()))
                        .max(Comparator.comparing(AnalysisResultViewModel::getSubmitDate)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(AnalysisResultViewModel::getSubmitDate))
                .toList();

        model.addAttribute("analysisLabelAll", analysis.stream()
                .map(AnalysisResultViewModel::getDocumentPeriod)
                .collect(Collectors.toList()));
        model.addAttribute("analysisPointAll", analysis.stream()
                .map(AnalysisResultViewModel::getCorporateValue)
                .collect(Collectors.toList()));
    }

    private void setForecastStock(final CorporateDetailViewModel view, final Model model) {
        model.addAttribute("forecastStocks", view.getMinkabuList());

        final List<MinkabuViewModel> forecastStock = view.getMinkabuList().stream()
                .sorted(Comparator.comparing(MinkabuViewModel::getTargetDate))
                .collect(Collectors.toList());

        model.addAttribute("forecastStockLabel180", forecastStock.stream()
                .map(MinkabuViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(180)))
                .collect(Collectors.toList()));
        model.addAttribute("forecastStockPoint180", forecastStock.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(180)))
                .map(MinkabuViewModel::getGoalsStock)
                .collect(Collectors.toList()));

        model.addAttribute("forecastStockLabel365", forecastStock.stream()
                .map(MinkabuViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(365)))
                .collect(Collectors.toList()));
        model.addAttribute("forecastStockPoint365", forecastStock.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(365)))
                .map(MinkabuViewModel::getGoalsStock)
                .collect(Collectors.toList()));

        model.addAttribute("forecastStockLabelAll", forecastStock.stream()
                .map(MinkabuViewModel::getTargetDate)
                .collect(Collectors.toList()));
        model.addAttribute("forecastStockPointAll", forecastStock.stream()
                .map(MinkabuViewModel::getGoalsStock)
                .collect(Collectors.toList()));
    }

    private void setStockPriceView(final CorporateDetailViewModel view, final Model model) {
        model.addAttribute("stockPrices", view.getStockPriceList());

        final List<StockPriceViewModel> stockPrice = view.getStockPriceList().stream()
                .sorted(Comparator.comparing(StockPriceViewModel::getTargetDate))
                .collect(Collectors.toList());

        model.addAttribute("stockLabel30", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(30)))
                .collect(Collectors.toList()));
        model.addAttribute("stockPoint30", stockPrice.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(30)))
                .map(StockPriceViewModel::getStockPrice)
                .collect(Collectors.toList()));

        model.addAttribute("stockLabel90", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(90)))
                .collect(Collectors.toList()));
        model.addAttribute("stockPoint90", stockPrice.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(90)))
                .map(StockPriceViewModel::getStockPrice)
                .collect(Collectors.toList()));

        model.addAttribute("stockLabel180", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(180)))
                .collect(Collectors.toList()));
        model.addAttribute("stockPoint180", stockPrice.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(180)))
                .map(StockPriceViewModel::getStockPrice)
                .collect(Collectors.toList()));

        model.addAttribute("stockLabel365", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(365)))
                .collect(Collectors.toList()));
        model.addAttribute("stockPoint365", stockPrice.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(365)))
                .map(StockPriceViewModel::getStockPrice)
                .collect(Collectors.toList()));

        model.addAttribute("stockLabelAll", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .collect(Collectors.toList()));
        model.addAttribute("stockPointAll", stockPrice.stream()
                .map(StockPriceViewModel::getStockPrice)
                .collect(Collectors.toList()));
    }

    private void setValuationView(final List<CompanyValuationViewModel> viewList, final Model model) {
        model.addAttribute("valuations", viewList);


        final List<CompanyValuationViewModel> valuation = viewList.stream()
                .sorted(Comparator.comparing(CompanyValuationViewModel::getTargetDate))
                .collect(Collectors.toList());

        model.addAttribute("valuationLabel180", valuation.stream()
                .map(CompanyValuationViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(180)))
                .collect(Collectors.toList()));
        model.addAttribute("valuationPoint180", valuation.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(180)))
                .map(CompanyValuationViewModel::getDifferenceFromSubmitDate)
                .collect(Collectors.toList()));

        model.addAttribute("valuationLabel365", valuation.stream()
                .map(CompanyValuationViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(365)))
                .collect(Collectors.toList()));
        model.addAttribute("valuationPoint365", valuation.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(365)))
                .map(CompanyValuationViewModel::getDifferenceFromSubmitDate)
                .collect(Collectors.toList()));

        model.addAttribute("valuationLabelAll", valuation.stream()
                .map(CompanyValuationViewModel::getTargetDate)
                .collect(Collectors.toList()));
        model.addAttribute("valuationPointAll", valuation.stream()
                .map(CompanyValuationViewModel::getDifferenceFromSubmitDate)
                .collect(Collectors.toList()));
    }
}
