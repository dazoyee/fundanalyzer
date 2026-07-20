package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;

public enum SystemEventType {
    ERROR("ERROR"),
    WARNING("WARNING");

    private final String value;

    SystemEventType(final String value) {
        this.value = value;
    }

    public String toValue() {
        return value;
    }

    public static SystemEventType fromValue(final String value) {
        for (SystemEventType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new FundanalyzerNotExistException("systemEventType");
    }
}
