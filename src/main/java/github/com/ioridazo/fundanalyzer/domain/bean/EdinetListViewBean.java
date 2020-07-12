package github.com.ioridazo.fundanalyzer.domain.bean;

import lombok.Value;

import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class EdinetListViewBean {

    private final LocalDate submitDate;

    private final Long countAll;

    private final Long countTarget;

    private final Long countScraped;

    private final Long countAnalyzed;

    private final Long countNotAnalyzed;

    private final Long countNotScraped;

    private final Long countNotTarget;
}
