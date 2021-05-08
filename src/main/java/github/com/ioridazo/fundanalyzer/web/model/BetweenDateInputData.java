package github.com.ioridazo.fundanalyzer.web.model;

import lombok.Value;

import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class BetweenDateInputData {

    private final LocalDate fromDate;

    private final LocalDate toDate;
}
