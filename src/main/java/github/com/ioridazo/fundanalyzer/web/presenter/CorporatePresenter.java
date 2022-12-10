package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.AnalysisResultViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.IndicatorViewModel;
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
        setInvestmentIndicator(view, model);
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
                .toList());
        model.addAttribute("analysisPointAll", analysis.stream()
                .map(AnalysisResultViewModel::getCorporateValue)
                .toList());
    }

    private void setInvestmentIndicator(final CorporateDetailViewModel view, final Model model) {
        model.addAttribute("indicators", view.getIndicatorList());

        final List<IndicatorViewModel> indicator = view.getIndicatorList().stream()
                .sorted(Comparator.comparing(IndicatorViewModel::targetDate))
                .toList();

        model.addAttribute("indicatorLabel30", indicator.stream()
                .map(IndicatorViewModel::targetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(30)))
                .toList());
        model.addAttribute("indicatorPoint30", indicator.stream()
                .filter(vm -> vm.targetDate().isAfter(LocalDate.now().minusDays(30)))
                .map(IndicatorViewModel::grahamIndex)
                .toList());

        model.addAttribute("indicatorLabel180", indicator.stream()
                .map(IndicatorViewModel::targetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(180)))
                .toList());
        model.addAttribute("indicatorPoint180", indicator.stream()
                .filter(vm -> vm.targetDate().isAfter(LocalDate.now().minusDays(180)))
                .map(IndicatorViewModel::grahamIndex)
                .toList());

        model.addAttribute("indicatorLabel365", indicator.stream()
                .map(IndicatorViewModel::targetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(365)))
                .toList());
        model.addAttribute("indicatorPoint365", indicator.stream()
                .filter(vm -> vm.targetDate().isAfter(LocalDate.now().minusDays(365)))
                .map(IndicatorViewModel::grahamIndex)
                .toList());

        model.addAttribute("indicatorLabelAll", indicator.stream()
                .map(IndicatorViewModel::targetDate)
                .toList());
        model.addAttribute("indicatorPointAll", indicator.stream()
                .map(IndicatorViewModel::grahamIndex)
                .toList());
    }

    private void setForecastStock(final CorporateDetailViewModel view, final Model model) {
        model.addAttribute("forecastStocks", view.getMinkabuList());

        final List<MinkabuViewModel> forecastStock = view.getMinkabuList().stream()
                .sorted(Comparator.comparing(MinkabuViewModel::getTargetDate))
                .toList();

        model.addAttribute("forecastStockLabel180", forecastStock.stream()
                .map(MinkabuViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(180)))
                .toList());
        model.addAttribute("forecastStockPoint180", forecastStock.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(180)))
                .map(MinkabuViewModel::getGoalsStock)
                .toList());

        model.addAttribute("forecastStockLabel365", forecastStock.stream()
                .map(MinkabuViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(365)))
                .toList());
        model.addAttribute("forecastStockPoint365", forecastStock.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(365)))
                .map(MinkabuViewModel::getGoalsStock)
                .toList());

        model.addAttribute("forecastStockLabelAll", forecastStock.stream()
                .map(MinkabuViewModel::getTargetDate)
                .toList());
        model.addAttribute("forecastStockPointAll", forecastStock.stream()
                .map(MinkabuViewModel::getGoalsStock)
                .toList());
    }

    private void setStockPriceView(final CorporateDetailViewModel view, final Model model) {
        model.addAttribute("stockPrices", view.getStockPriceList());

        final List<StockPriceViewModel> stockPrice = view.getStockPriceList().stream()
                .sorted(Comparator.comparing(StockPriceViewModel::getTargetDate))
                .toList();

        model.addAttribute("stockLabel30", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(30)))
                .toList());
        model.addAttribute("stockPoint30", stockPrice.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(30)))
                .map(StockPriceViewModel::getStockPrice)
                .toList());

        model.addAttribute("stockLabel90", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(90)))
                .toList());
        model.addAttribute("stockPoint90", stockPrice.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(90)))
                .map(StockPriceViewModel::getStockPrice)
                .toList());

        model.addAttribute("stockLabel180", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(180)))
                .toList());
        model.addAttribute("stockPoint180", stockPrice.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(180)))
                .map(StockPriceViewModel::getStockPrice)
                .toList());

        model.addAttribute("stockLabel365", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(365)))
                .toList());
        model.addAttribute("stockPoint365", stockPrice.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(365)))
                .map(StockPriceViewModel::getStockPrice)
                .toList());

        model.addAttribute("stockLabelAll", stockPrice.stream()
                .map(StockPriceViewModel::getTargetDate)
                .toList());
        model.addAttribute("stockPointAll", stockPrice.stream()
                .map(StockPriceViewModel::getStockPrice)
                .toList());
    }

    private void setValuationView(final List<CompanyValuationViewModel> viewList, final Model model) {
        model.addAttribute("valuations", viewList);

        final List<CompanyValuationViewModel> valuation = viewList.stream()
                .sorted(Comparator.comparing(CompanyValuationViewModel::getTargetDate))
                .toList();

        model.addAttribute("valuationLabel180", valuation.stream()
                .map(CompanyValuationViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(180)))
                .toList());
        model.addAttribute("valuationPoint180", valuation.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(180)))
                .map(CompanyValuationViewModel::getDifferenceFromSubmitDate)
                .toList());

        model.addAttribute("valuationLabel365", valuation.stream()
                .map(CompanyValuationViewModel::getTargetDate)
                .filter(targetDate -> targetDate.isAfter(LocalDate.now().minusDays(365)))
                .toList());
        model.addAttribute("valuationPoint365", valuation.stream()
                .filter(vm -> vm.getTargetDate().isAfter(LocalDate.now().minusDays(365)))
                .map(CompanyValuationViewModel::getDifferenceFromSubmitDate)
                .toList());

        model.addAttribute("valuationLabelAll", valuation.stream()
                .map(CompanyValuationViewModel::getTargetDate)
                .toList());
        model.addAttribute("valuationPointAll", valuation.stream()
                .map(CompanyValuationViewModel::getDifferenceFromSubmitDate)
                .toList());
    }
}
