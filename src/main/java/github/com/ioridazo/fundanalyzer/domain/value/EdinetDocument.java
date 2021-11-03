package github.com.ioridazo.fundanalyzer.domain.value;

import lombok.Data;

import java.time.LocalDate;
import java.util.Optional;

@Data
public class EdinetDocument {

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private String docDescription;

    private String parentDocId;

    public Optional<LocalDate> getPeriodStart() {
        return Optional.ofNullable(periodStart);
    }

    public Optional<LocalDate> getPeriodEnd() {
        return Optional.ofNullable(periodEnd);
    }

    public Optional<String> getDocDescription() {
        return Optional.ofNullable(docDescription);
    }

    public Optional<String> getParentDocId() {
        return Optional.ofNullable(parentDocId);
    }
}
