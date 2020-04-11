package github.com.ioridazo.fundamentalanalysis.edinet.entity.request;

import lombok.Value;

@Value
public class AcquisitionRequestParameter {

    private String docId;

    private AcquisitionType type;
}
