package github.com.ioridazo.fundanalyzer.web;

import github.com.ioridazo.fundanalyzer.domain.AnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalysisController {

    final private AnalysisService service;

    public AnalysisController(final AnalysisService service) {
        this.service = service;
    }

    @GetMapping("/company")
    public String company() {
        return service.company();
    }

    @GetMapping("/edinet/{date}")
    public String documentList(@PathVariable String date) {
        return service.getDocumentFile(date, date, "120");
    }

    @GetMapping("/edinet/{fromDate}/{toDate}")
    public String documentList(@PathVariable String fromDate, @PathVariable String toDate) {
        return service.getDocumentFile(fromDate, toDate, "120");
    }

    @GetMapping("/scrape/{docId}")
    public String scrape(@PathVariable String docId) {
        var resultBeanList = service.scrape(docId);
        service.insert(resultBeanList);
        return "insert\n";
    }
}
