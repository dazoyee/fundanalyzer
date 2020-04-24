package github.com.ioridazo.fundanalyzer.edinet.entity.request;

import lombok.Value;

@Value
public class AcquisitionRequestParameter {

    private String docId;

    private AcquisitionType type;
}
