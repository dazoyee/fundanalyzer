package github.com.ioridazo.fundanalyzer.client.log;

public enum Process {
    EDINET("edinet"),
    COMPANY("company"),
    SCRAPING("scraping"),
    DOWNLOAD("download"),
    DECODE("decode"),
    ANALYSIS("analysis"),
    IMPORT("import"),
    UPDATE("update"),
    NOTICE("notice"),
    SORT("sort"),
    ;

    private final String value;

    Process(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
