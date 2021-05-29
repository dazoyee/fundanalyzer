package github.com.ioridazo.fundanalyzer.client.edinet.entity.request;

import lombok.Value;

import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class ListRequestParameter {

    private final LocalDate date;

    private final ListType type;
}
