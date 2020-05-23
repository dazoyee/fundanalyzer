package github.com.ioridazo.fundanalyzer.domain.jsoup.bean;

import lombok.Value;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Value
public class PeriodResultBean {

    String term;

    LocalDate fromDate;

    LocalDate toDate;

    public PeriodResultBean(String scraped) {
        final var normalize = Normalizer.normalize(scraped, Normalizer.Form.NFKC);
        this.term = normalize.substring(normalize.indexOf("第"), normalize.indexOf("期") + 1);
        this.fromDate = parseLocalDate(normalize.substring(normalize.indexOf("自 ") + 2, normalize.indexOf("日") + 1));
        this.toDate = parseLocalDate(normalize.substring(normalize.indexOf("至 ") + 2, normalize.lastIndexOf("日") + 1));
    }

    LocalDate parseLocalDate(String scrapedDate) {
        try {
            return LocalDate.from(JapaneseDate.from(
                    DateTimeFormatter
                            .ofPattern("Gy年M月d日")
                            .withChronology(JapaneseChronology.INSTANCE)
                            .parse(scrapedDate)));
        } catch (DateTimeParseException e) {
            return LocalDate.parse(scrapedDate, DateTimeFormatter.ofPattern("y年M月d日"));
        }
    }

    public Optional<String> getTerm() {
        return Optional.ofNullable(term);
    }

    public Optional<LocalDate> getFromDate() {
        return Optional.ofNullable(fromDate);
    }

    public Optional<LocalDate> getToDate() {
        return Optional.ofNullable(toDate);
    }
}
