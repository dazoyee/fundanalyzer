package github.com.ioridazo.fundanalyzer.domain.entity.master;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Consolidated {

    CONSOLIDATED("1", "有"),
    NO_CONSOLIDATED("0", "無"),
    NULL("9", ""),
    ;

    private final String code;

    private final String name;

    Consolidated(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonCreator
    public static Consolidated fromName(String name) {
        return Arrays.stream(values())
                .filter(v -> v.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(name)));
    }

    @JsonValue
    public String toValue() {
        return this.code;
    }

    @JsonValue
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return String.format("Consolidated[code = %s]", this.code);
    }
}
