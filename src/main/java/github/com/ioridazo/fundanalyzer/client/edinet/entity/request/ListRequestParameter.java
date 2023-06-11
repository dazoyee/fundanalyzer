package github.com.ioridazo.fundanalyzer.client.edinet.entity.request;

import java.time.LocalDate;

public record ListRequestParameter(
        LocalDate date,
        ListType type
) {
}
