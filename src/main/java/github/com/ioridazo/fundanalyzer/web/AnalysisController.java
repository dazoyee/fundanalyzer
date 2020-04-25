package github.com.ioridazo.fundanalyzer.web;

import github.com.ioridazo.fundanalyzer.domain.AnalysisService;
import github.com.ioridazo.fundanalyzer.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundanalyzer.edinet.entity.request.AcquisitionType;
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

    @GetMapping("/insert/document/list/{date}")
    public String documentList(@PathVariable String date) {
        return service.documentList(date);
    }

    @GetMapping("/insert/document/list/{fromDate}/{toDate}")
    public String documentList(@PathVariable String fromDate, @PathVariable String toDate) {
        return service.documentList(fromDate, toDate);
    }

    @GetMapping("/document/{docTypeCode}")
    public String getDocId(@PathVariable String docTypeCode) {
        return service.docIdList(docTypeCode) + "\n";
    }

    @GetMapping("/document/acquisition/{docId}/{type}")
    public String documentAcquisition(@PathVariable String docId, @PathVariable String type) {
        return service.documentAcquisition(
                new AcquisitionRequestParameter(
                        docId,
                        AcquisitionType.DEFAULT.toValue().equals(type) ? AcquisitionType.DEFAULT :
                                AcquisitionType.PDF.toValue().equals(type) ? AcquisitionType.PDF :
                                        AcquisitionType.ALTERNATIVE.toValue().equals(type) ? AcquisitionType.ALTERNATIVE :
                                                AcquisitionType.ENGLISH.toValue().equals(type) ? AcquisitionType.ENGLISH :
                                                        null
                )
        );
    }

    @GetMapping("/operate/file/{docId}")
    public String operateFile(@PathVariable String docId) {
        return service.operateFile(docId);
    }

    @GetMapping("/scrape/{docId}")
    public String scrape(@PathVariable String docId) {
        var resultBeanList = service.scrape(docId);
        service.insert(resultBeanList);
        return "insert\n";
    }
}
