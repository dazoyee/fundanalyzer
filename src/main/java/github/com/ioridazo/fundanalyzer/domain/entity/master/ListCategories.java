package github.com.ioridazo.fundanalyzer.domain.entity.master;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ListCategories {

    LISTED("1", "上場"),
    UNLISTED("0", "非上場"),
    NULL("9", "情報なし"),
    ;

    private String code;

    private String name;

    ListCategories(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonCreator
    public static ListCategories fromValue(String code) {
        return Arrays.stream(values())
                .filter(v -> v.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(code)));
    }

    @JsonValue
    public String toValue() {
        return this.code;
    }

    public String toName() {
        return this.name;
    }

    @Override
    public String toString() {
        return String.format("ListCategories[code = %s]", this.code);
    }
}
