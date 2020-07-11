package github.com.ioridazo.fundanalyzer.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum DocumentStatus {

    NOT_YET("0", "未着手"),
    DONE("1", "完了"),
    //    HALF_WAY("5", "途中"),
    ERROR("9", "失敗"),
    ;

    private final String code;

    private final String name;

    DocumentStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonCreator
    public static DocumentStatus fromValue(String code) {
        return Arrays.stream(values())
                .filter(v -> v.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(code)));
    }

    @JsonValue
    public String toValue() {
        return this.code;
    }

    @Override
    public String toString() {
        return String.format("DocumentStatus[code = %s]", this.code);
    }
}
