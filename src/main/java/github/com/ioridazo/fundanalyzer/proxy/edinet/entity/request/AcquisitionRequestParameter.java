package github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request;

import lombok.Value;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class AcquisitionRequestParameter {

    private final String docId;

    private final AcquisitionType type;
}
