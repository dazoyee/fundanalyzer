package github.com.ioridazo.fundanalyzer.edinet.entity.request;

import lombok.Value;

@Value
public class AcquisitionRequestParameter {

    String docId;

    AcquisitionType type;
}
