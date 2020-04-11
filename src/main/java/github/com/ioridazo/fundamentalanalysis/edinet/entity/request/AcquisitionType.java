package github.com.ioridazo.fundamentalanalysis.edinet.entity.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AcquisitionType {
    DEFAULT("1"),
    PDF("2"),
    ALTERNATIVE("3"),
    ENGLISH("4")
    ;

    private final String code;

    AcquisitionType(String code) {
        this.code = code;
    }

    @JsonCreator
    public static AcquisitionType fromValue(String code) {
        return Arrays.stream(values())
                .filter(v -> v.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(code)));
    }

    @JsonValue
    public String toValue() {
        return this.code;
    }

    @Override
    public String toString() {
        return String.format("ListType[code = %s]", this.code);
    }

}
