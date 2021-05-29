package github.com.ioridazo.fundanalyzer.web.model;

import lombok.Value;

import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class DateInputData {

    private final LocalDate date;
}
