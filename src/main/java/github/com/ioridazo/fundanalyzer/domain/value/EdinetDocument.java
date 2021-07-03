package github.com.ioridazo.fundanalyzer.domain.value;

import lombok.Data;

import java.time.LocalDate;
import java.util.Optional;

@Data
public class EdinetDocument {

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private String docDescription;

    public Optional<String> getDocDescription() {
        return Optional.ofNullable(docDescription);
    }
}
