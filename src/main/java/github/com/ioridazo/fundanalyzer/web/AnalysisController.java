package github.com.ioridazo.fundanalyzer.web;

import github.com.ioridazo.fundanalyzer.domain.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.DocumentService;
import github.com.ioridazo.fundanalyzer.domain.ViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;

@Controller
public class AnalysisController {

    final private DocumentService documentService;
    final private AnalysisService analysisService;
    final private ViewService viewService;

    public AnalysisController(
            DocumentService documentService,
            AnalysisService analysisService,
            ViewService viewService) {
        this.documentService = documentService;
        this.analysisService = analysisService;
        this.viewService = viewService;
    }

    @GetMapping("fundanalyzer/v1/company")
    public String company(final Model model) {
        documentService.company();

        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }

    @GetMapping("fundanalyzer/v1/edinet/list/{fromDate}/{toDate}")
    public String edinet(@PathVariable String fromDate, @PathVariable String toDate, final Model model) {
        documentService.edinetList(fromDate, toDate);

        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }

    @GetMapping("fundanalyzer/v1/document/{date}")
    public String document(@PathVariable String date, final Model model) {
        documentService.document(date, "120");

        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }

    @GetMapping("fundanalyzer/v1/analysis/{year}")
    public String analysis(@PathVariable String year, final Model model) {
        model.addAttribute("companies", analysisService.analyze(year));
        return "index";
    }

    // -------------------------------------------------------

    @GetMapping("/company")
    public String devCompany(final Model model) {
        documentService.company();

        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }

    @GetMapping("/edinet/list/{date}")
    public String documentList(@PathVariable String date, final Model model) {
        documentService.company();
        documentService.insertDocumentList(LocalDate.parse(date));

        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }

    @GetMapping("/edinet/list/{fromDate}/{toDate}")
    public String document(@PathVariable String fromDate, @PathVariable String toDate, final Model model) {
        documentService.company();
        documentService.edinetList(fromDate, toDate);

        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }

    @GetMapping("/scrape/{date}")
    public String devDocument(@PathVariable String date, final Model model) {
        documentService.company();
        documentService.document(date, "120");

        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }

    @GetMapping("/view/company/{year}")
    public String viewCompany(@PathVariable String year, final Model model) {
        documentService.company();
        documentService.document("2020-05-22", "120");

        model.addAttribute("companies", viewService.viewCompany(year));
        return "index";
    }

    @GetMapping("/analysis/{year}")
    public String devAnalysis(@PathVariable String year, final Model model) {
        model.addAttribute("companies", analysisService.analyze(year));
        return "index";
    }
}
