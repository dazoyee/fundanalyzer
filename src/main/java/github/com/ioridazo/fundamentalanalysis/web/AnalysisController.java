package github.com.ioridazo.fundamentalanalysis.web;

import github.com.ioridazo.fundamentalanalysis.domain.service.AnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalysisController {

    private AnalysisService service;

    public AnalysisController(final AnalysisService service) {
        this.service = service;
    }

    @GetMapping
    public String insert() {
        try {
            service.insert();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "string\n";
    }
}
