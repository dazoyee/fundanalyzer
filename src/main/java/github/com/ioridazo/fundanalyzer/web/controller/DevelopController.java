package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.usecase.AnalyzeUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.StockUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewEdinetUseCase;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

@Profile("!prod")
@Controller
public class DevelopController {

    private final ViewService viewService;
    private final CompanyUseCase companyUseCase;
    private final DocumentUseCase documentUseCase;
    private final AnalyzeUseCase analyzeUseCase;
    private final StockUseCase stockUseCase;
    private final ViewCorporateUseCase viewCorporateUseCase;
    private final ViewEdinetUseCase viewEdinetUseCase;

    public DevelopController(
            final ViewService viewService,
            final CompanyUseCase companyUseCase,
            final DocumentUseCase documentUseCase,
            final AnalyzeUseCase analyzeUseCase,
            final StockUseCase stockUseCase,
            final ViewCorporateUseCase viewCorporateUseCase,
            final ViewEdinetUseCase viewEdinetUseCase) {
        this.documentUseCase = documentUseCase;
        this.analyzeUseCase = analyzeUseCase;
        this.viewEdinetUseCase = viewEdinetUseCase;
        this.viewService = viewService;
        this.companyUseCase = companyUseCase;
        this.stockUseCase = stockUseCase;
        this.viewCorporateUseCase = viewCorporateUseCase;
    }

    @GetMapping("/template")
    public String template() {
        return "template";
    }

    @GetMapping("/edinet-list")
    public String devEdinetList(final Model model) {
        model.addAttribute("companyUpdated", companyUseCase.getUpdateDate());
        model.addAttribute("edinetList", viewService.getEdinetListView());
        return "edinet";
    }

    @GetMapping("/company")
    public String devCompany(final Model model) {
        // company
        companyUseCase.saveCompanyInfo();

        model.addAttribute("companies", viewService.getCorporateView());
        return "index";
    }

    @GetMapping("/scrape/analysis/{date}")
    public String devDoMain(@PathVariable final String date, final Model model) {
        // company
        companyUseCase.saveCompanyInfo();

        final DateInputData inputData = DateInputData.of(LocalDate.parse(date));
        // scraping
        documentUseCase.allProcess(inputData);
        // remove
        documentUseCase.removeDocument(inputData);
        // stock
        stockUseCase.importStockPrice(inputData, StockUseCase.Place.NIKKEI);
        stockUseCase.importStockPrice(inputData, StockUseCase.Place.MINKABU);
        // analysis
        analyzeUseCase.analyze(inputData);
        // view corporate
        viewCorporateUseCase.updateView(inputData);
        // view edinet
        viewEdinetUseCase.updateView(inputData);

        model.addAttribute("companies", viewService.getCorporateView());
        return "redirect:" + UriComponentsBuilder.fromUriString("/v2/index").toUriString();
    }
}
