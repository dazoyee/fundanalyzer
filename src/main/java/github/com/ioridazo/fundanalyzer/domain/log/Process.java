package github.com.ioridazo.fundanalyzer.domain.log;

public enum Process {
    EDINET("edinet"),
    COMPANY("company"),
    UPDATE("update");

    private final String value;

    Process(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
