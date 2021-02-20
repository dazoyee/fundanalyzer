package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import github.com.ioridazo.fundanalyzer.domain.service.StockService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;

@Profile("!prod")
@Controller
public class DevelopController {

    private final DocumentService documentService;
    private final AnalysisService analysisService;
    private final StockService stockService;
    private final ViewService viewService;

    public DevelopController(
            final DocumentService documentService,
            final AnalysisService analysisService,
            final StockService stockService,
            final ViewService viewService) {
        this.documentService = documentService;
        this.analysisService = analysisService;
        this.stockService = stockService;
        this.viewService = viewService;
    }

    @GetMapping("/edinet/list")
    public String devEdinetList(final Model model) {
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetListview());
        return "edinet";
    }

    @GetMapping("/company")
    public String devCompany(final Model model) {
        documentService.readCompanyInfo();

        model.addAttribute("companies", viewService.corporateView());
        return "index";
    }

    @GetMapping("/scrape/{date}")
    public String devDocument(@PathVariable String date, final Model model) {
        documentService.readCompanyInfo();
        documentService.execute(date, "120")
                .thenRunAsync(viewService::updateCorporateView);

        model.addAttribute("companies", viewService.corporateView());
        return "index";
    }

    @GetMapping("/reset/status")
    public String devResetStatus(final Model model) {
        documentService.resetForRetry();

        model.addAttribute("companies", viewService.corporateView());
        return "index";
    }

    @GetMapping("/view/company/{year}")
    public String viewCompany(@PathVariable String year, final Model model) {
        documentService.readCompanyInfo();
        documentService.execute("2020-05-22", "120")
                .thenRunAsync(viewService::updateCorporateView);

        model.addAttribute("companies", viewService.corporateView());
        return "index";
    }

    @GetMapping("/scrape/analysis/{date}")
    public String scrapeAndAnalyze(@PathVariable String date, final Model model) {
        documentService.readCompanyInfo();

        // execute実行
        documentService.execute(date, "120")
                // execute完了後、analyze実行
                .thenRunAsync(() -> analysisService.analyze(LocalDate.parse(date)))
                // analyze完了後、importStockPrice実行
                .thenRunAsync(() -> stockService.importStockPrice(LocalDate.parse(date)))
                // importStockPrice完了後、notice実行
                .thenRunAsync(() -> viewService.notice(LocalDate.parse(date)))
                // notice完了後、update実行
                .thenRunAsync(() -> {
                    viewService.updateCorporateView();
                    viewService.updateEdinetListView("120");
                });

        model.addAttribute("companies", viewService.corporateView());
        return "redirect:/fundanalyzer/v1/index" + "?message=updating";
    }
}
