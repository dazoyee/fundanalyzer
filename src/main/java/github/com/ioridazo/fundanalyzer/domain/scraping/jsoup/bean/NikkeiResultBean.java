package github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.bean;

import lombok.Value;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class NikkeiResultBean {

    private final String stockPrice;

    private final String targetDate;

    private final String openingPrice;

    private final String highPrice;

    private final String lowPrice;

    private final String volume;

    private final String per;

    private final String pbr;

    private final String roe;

    private final String numberOfShares;

    private final String marketCapitalization;

    private final String dividendYield;

    private final String shareholderBenefit;
}
