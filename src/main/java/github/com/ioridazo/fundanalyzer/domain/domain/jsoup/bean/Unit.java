package github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

public enum Unit {

    THOUSANDS_OF_YEN(List.of("単位：千円", "単位:千円", "単位　千円", "金額（千円）", "（千万円）"), 1000),
    MILLIONS_OF_YEN(List.of("単位：百万円", "単位:百万円", "単位　百万円", "金額（百万円）", "（百万円）"), 1000000),
    ;

    private final List<String> name;

    private final int value;

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
}
