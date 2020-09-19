package github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.bean;

import lombok.Value;

import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class FinancialTableResultBean {

    private final String subject;

    private final String previousValue;

    private final String currentValue;

    private final Unit unit;

    public Optional<String> getSubject() {
        return Optional.ofNullable(subject);
    }

    public Optional<String> getPreviousValue() {
        return Optional.ofNullable(previousValue);
    }

//    public Optional<String> getCurrentValue() {
//        return Optional.ofNullable(currentValue);
//    }
}