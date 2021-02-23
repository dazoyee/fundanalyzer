package github.com.ioridazo.fundanalyzer.domain.log;

public enum Category {
    VIEW("view"),
    DOCUMENT("document"),
    STOCK("stock"),
    SCHEDULER("scheduler"),
    ;

    private final String value;

    Category(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
