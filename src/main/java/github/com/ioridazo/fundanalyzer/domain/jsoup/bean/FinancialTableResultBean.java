package github.com.ioridazo.fundanalyzer.domain.jsoup.bean;

import lombok.Value;

@Value
public class FinancialTableResultBean {

    private String subject;

    private String previousValue;

    private String currentValue;
}
