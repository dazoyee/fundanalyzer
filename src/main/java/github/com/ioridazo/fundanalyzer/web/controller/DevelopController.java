package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
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

    private final AnalysisService analysisService;
    private final ViewService viewService;
    private final CompanyUseCase companyUseCase;

    public DevelopController(
            final AnalysisService analysisService,
            final ViewService viewService,
            final CompanyUseCase companyUseCase) {
        this.analysisService = analysisService;
        this.viewService = viewService;
        this.companyUseCase = companyUseCase;
    }

    @GetMapping("/edinet/list")
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

        analysisService.doMain(BetweenDateInputData.of(LocalDate.parse(date), LocalDate.parse(date)));

        model.addAttribute("companies", viewService.getCorporateView());
        return "redirect:" + UriComponentsBuilder.fromUriString("/fundanalyzer/v1/index")
                .queryParam("message", "処理を要求しました。").build().encode().toUriString();
    }
}
