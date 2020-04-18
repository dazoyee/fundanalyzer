package github.com.ioridazo.fundamentalanalysis.domain.jsoup.bean;

import lombok.Value;

@Value
public class FinancialTableResultBean {

    private String subject;

    private String previousValue;

    private String currentValue;
}
