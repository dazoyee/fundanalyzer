package github.com.ioridazo.fundanalyzer.domain.value;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EdinetDocument {

    private LocalDate periodStart;

    private LocalDate periodEnd;
}
