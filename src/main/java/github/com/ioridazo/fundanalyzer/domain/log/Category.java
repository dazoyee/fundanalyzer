package github.com.ioridazo.fundanalyzer.domain.log;

public enum Category {
    VIEW("view"),
    DOCUMENT("document"),
    STOCK("stock"),
    SLACK("slack"),
    SCHEDULER("scheduler"),
    ERROR("error"),
    ;

    private final String value;

    Category(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
