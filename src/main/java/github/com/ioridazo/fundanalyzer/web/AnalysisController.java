package github.com.ioridazo.fundanalyzer.web;

import github.com.ioridazo.fundanalyzer.domain.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.DocumentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class AnalysisController {

    final private DocumentService documentService;
    final private AnalysisService analysisService;

    public AnalysisController(
            DocumentService documentService,
            AnalysisService analysisService) {
        this.documentService = documentService;
        this.analysisService = analysisService;
    }

    @GetMapping("/company")
    public String company() {
        return documentService.company();
    }

    @GetMapping("/edinet/document/{date}")
    public String documentList(@PathVariable String date) {
        documentService.insertDocumentList(LocalDate.parse(date));
        return "documentList\n";
    }

    @GetMapping("/edinet/{date}")
    public String document(@PathVariable String date) {
        return documentService.document(date, "120");
    }

    @GetMapping("/edinet/{fromDate}/{toDate}")
    public String document(@PathVariable String fromDate, @PathVariable String toDate) {
        return documentService.document(fromDate, toDate, "120");
    }

    @GetMapping("/analysis/{company}/{year}")
    public String analysis(@PathVariable String company, @PathVariable String year) {
        return analysisService.analyze(company, year);
    }
}
