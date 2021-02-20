package github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.bean;

import lombok.Value;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class NumberOfSharesResultBean {

    private final String type;

    private final String fiscalYearEndNumber;

    private final String presentNumber;

    private final String stockExchangeName;

    private final String content;
}
