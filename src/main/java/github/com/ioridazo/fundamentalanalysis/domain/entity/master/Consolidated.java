package github.com.ioridazo.fundamentalanalysis.domain.entity.master;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Consolidated {

    CONSOLIDATED("1", "有"),
    NO_CONSOLIDATED("0", "無"),
    NULL("9", "情報なし"),
    ;

    private String code;

    private String name;

    Consolidated(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonCreator
    public static Consolidated fromValue(String code) {
        return Arrays.stream(values())
                .filter(v -> v.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(code)));
    }

    @JsonValue
    public String toValue() {
        return this.code;
    }

    @JsonValue
    public String toName() {
        return this.name;
    }

    @Override
    public String toString() {
        return String.format("Consolidated[code = %s]", this.code);
    }
}
