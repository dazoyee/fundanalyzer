package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

public enum CreatedType {
    AUTO("0"),
    MANUAL("1"),
    ;

    private final String type;

    CreatedType(final String type) {
        this.type = type;
    }

    public String toValue() {
        return type;
    }

    @Override
    public String toString() {
        return "CreatedType{" +
                "type='" + type + '\'' +
                '}';
    }
}
