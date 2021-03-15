package github.com.ioridazo.fundanalyzer.domain.entity.master;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ListCategories {

    LISTED("1", "上場"),
    UNLISTED("0", "非上場"),
    NULL("9", ""),
    ;

    private final String code;

    private final String name;

    ListCategories(final String code, final String name) {
        this.code = code;
        this.name = name;
    }

    @JsonCreator
    public static ListCategories fromName(final String name) {
        return Arrays.stream(values())
                .filter(v -> v.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(name)));
    }

    @JsonValue
    public String toValue() {
        return this.code;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return String.format("ListCategories[code = %s]", this.code);
    }
}
