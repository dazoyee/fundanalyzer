package github.com.ioridazo.fundanalyzer.web;

import github.com.ioridazo.fundanalyzer.domain.AnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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

    @GetMapping("/edinet/document/{date}")
    public String documentList(@PathVariable String date) {
        service.insertDocumentList(LocalDate.parse(date));
        return "documentList\n";
    }

    @GetMapping("/edinet/{date}")
    public String document(@PathVariable String date) {
        return service.document(date, "120");
    }

    @GetMapping("/edinet/{fromDate}/{toDate}")
    public String document(@PathVariable String fromDate, @PathVariable String toDate) {
        return service.document(fromDate, toDate, "120");
    }
}
