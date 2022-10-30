package github.com.ioridazo.fundanalyzer.web.presenter;

import java.util.Arrays;

public enum Target {
    MAIN("main"),
    QUART("quart"),
    ALL("all"),
    FAVORITE("favorite"),
    INDUSTRY("industry"),
    ;

    private final String value;

    Target(final String value) {
        this.value = value;
    }

    public static Target fromValue(final String value) {
        return Arrays.stream(values())
                .filter(v -> v.value.equals(value))
                .findFirst()
                .orElse(MAIN);
    }

    public String toValue() {
        return value;
    }
}
