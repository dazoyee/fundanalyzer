package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import lombok.Getter;

@Getter
public enum Flag {
    ON("1"),
    OFF("0"),
    ;

    private final String value;

    Flag(final String value) {
        this.value = value;
    }
}
