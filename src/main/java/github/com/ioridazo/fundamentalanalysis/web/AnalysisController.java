package github.com.ioridazo.fundamentalanalysis.web;

import github.com.ioridazo.fundamentalanalysis.domain.AnalysisService;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.RequestParameter;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.Type;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.response.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalysisController {

    private AnalysisService service;

    public AnalysisController(final AnalysisService service) {
        this.service = service;
    }

    @GetMapping("/insert")
    public String insert() {
        try {
            service.insert();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "string\n";
    }

    @GetMapping("/edinet")
    public Response edinet() {
        return service.documentList();
    }

    @GetMapping("/insert/document/list/{date}/{type}")
    public String insertDocumentList1(@PathVariable String date, @PathVariable String type) {
        return service.insertDocumentList(
                new RequestParameter(
                        date,
                        "1".equals(type) ? Type.DEFAULT : Type.GET_LIST
                )
        );
    }
}
