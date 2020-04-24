package github.com.ioridazo.fundanalyzer.edinet.entity.request;

import lombok.Value;

@Value
public class ListRequestParameter {

    private String date;

    private ListType type;
}
