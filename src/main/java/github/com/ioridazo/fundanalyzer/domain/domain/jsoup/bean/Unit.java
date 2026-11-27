package github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.regex.Pattern;

public enum Unit {

    THOUSANDS_OF_YEN(List.of("単位：千円", "単位:千円", "単位　千円", "金額（千円）", "（千万円）"), 1000),
    MILLIONS_OF_YEN(List.of("単位：百万円", "単位:百万円", "単位　百万円", "金額（百万円）", "（百万円）"), 1000000),
    ;

    private final List<String> name;

    private final int value;

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\s\\u3000\\u00A0]+");

    Unit(final List<String> name, final int value) {
        this.name = name;
        this.value = value;
    }

    @JsonValue
    public List<String> getName() {
        return name;
    }

    @JsonValue
    public int getValue() {
        return this.value;
    }

    public boolean matches(final String text) {
        final String normalizedText = normalize(text);
        return name.stream().map(Unit::normalize).anyMatch(normalizedText::contains);
    }

    private static String normalize(final String text) {
        return WHITESPACE_PATTERN.matcher(text.replace('(', '（').replace(')', '）').replace(':', '：'))
                .replaceAll("");
    }
}
