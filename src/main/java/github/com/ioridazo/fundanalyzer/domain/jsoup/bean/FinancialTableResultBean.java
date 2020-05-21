package github.com.ioridazo.fundanalyzer.domain.jsoup.bean;

import lombok.Value;

import java.util.Optional;

@Value
public class FinancialTableResultBean {

    String subject;

    String previousValue;

    String currentValue;

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
