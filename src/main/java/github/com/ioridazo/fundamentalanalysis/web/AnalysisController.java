package github.com.ioridazo.fundamentalanalysis.web;

import github.com.ioridazo.fundamentalanalysis.domain.AnalysisService;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.AcquisitionType;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.ListType;
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

    @GetMapping("/edinet")
    public Response edinet() {
        return service.documentList();
    }

    @GetMapping("/insert/document/list/{date}/{type}")
    public String insertDocumentList1(@PathVariable String date, @PathVariable String type) {
        return service.insertDocumentList(
                new ListRequestParameter(
                        date,
                        ListType.DEFAULT.toValue().equals(type) ? ListType.DEFAULT :
                                ListType.GET_LIST.toValue().equals(type) ? ListType.GET_LIST :
                                        null
                )
        );
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
                                AcquisitionType.PDF.toValue() == type ? AcquisitionType.PDF :
                                        AcquisitionType.ALTERNATIVE.toValue() == type ? AcquisitionType.ALTERNATIVE :
                                                AcquisitionType.ENGLISH.toValue() == type ? AcquisitionType.ENGLISH :
                                                        null
                )
        );
    }

    @GetMapping("/operate/file/{docId}")
    public String operateFile(@PathVariable String docId) {
        return service.operateFile(docId);
    }

    @GetMapping("/scrape/{docId}")
    public String scrape(@PathVariable String docId){
        var resultBeanList = service.scrape(docId);
        service.insert(resultBeanList);
        return "insert\n";
    }
}
