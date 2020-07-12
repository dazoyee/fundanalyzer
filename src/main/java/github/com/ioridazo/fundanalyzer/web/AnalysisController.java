package github.com.ioridazo.fundanalyzer.web;

import github.com.ioridazo.fundanalyzer.domain.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.DocumentService;
import github.com.ioridazo.fundanalyzer.domain.ViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

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

    @GetMapping("fundanalyzer/v1/index")
    public String index(final Model model) {
        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }

    @GetMapping("fundanalyzer/v1/edinet/list")
    public String edinetList(final Model model) {
        model.addAttribute("edinetList", viewService.edinetList("120"));
        return "edinet";
    }

    @GetMapping("fundanalyzer/v1/company")
    public String company(final Model model) {
        documentService.company();

        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }

    @PostMapping("fundanalyzer/v1/edinet/list")
    public String edinet(final String fromDate, final String toDate) {
        documentService.edinetList(fromDate, toDate);
        return "redirect:/fundanalyzer/v1/edinet/list";
    }

    @PostMapping("fundanalyzer/v1/document/analysis")
    public String documentAnalysis(final String date, final Model model) {
        final var year = LocalDate.parse(date).getYear();

        documentService.document(date, "120");
        analysisService.analyze(year);

        model.addAttribute("companies", viewService.viewCompany(year));
        return "index";
    }

    // -------------------------------------------------------

    @GetMapping("/edinet/list")
    public String devEdinetList(final Model model) {
        model.addAttribute("edinetList", viewService.edinetList("120"));
        return "edinet";
    }

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

        model.addAttribute("edinetList", viewService.edinetList("120"));
        return "edinet";
    }

    @GetMapping("/edinet/list/{fromDate}/{toDate}")
    public String document(@PathVariable String fromDate, @PathVariable String toDate, final Model model) {
        documentService.company();
        documentService.edinetList(fromDate, toDate);

        model.addAttribute("edinetList", viewService.edinetList("120"));
        return "edinet";
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

        model.addAttribute("companies", viewService.viewCompany(Integer.parseInt(year)));
        return "index";
    }

    @GetMapping("/analysis/{year}")
    public String devAnalysis(@PathVariable String year, final Model model) {
        analysisService.analyze(Integer.parseInt(year));

        model.addAttribute("companies", viewService.viewCompany(Integer.parseInt(year)));
        return "index";
    }
}
