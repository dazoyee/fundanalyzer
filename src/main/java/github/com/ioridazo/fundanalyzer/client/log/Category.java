package github.com.ioridazo.fundanalyzer.client.log;

public enum Category {
    ACCESS("access"),
    SCHEDULER("scheduler"),

    COMPANY("company"),
    DOCUMENT("document"),
    SCRAPING("scraping"),
    ANALYSIS("analysis"),
    VIEW("view"),
    STOCK("stock"),
    NOTICE("notice"),

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
