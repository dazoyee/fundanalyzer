package github.com.ioridazo.fundanalyzer.web.presenter;

public enum Target {
    ALL("all"),
    FAVORITE("favorite"),
    INDUSTRY("industry"),
    ;

    private final String value;

    Target(final String value) {
        this.value = value;
    }

    public String toValue() {
        return value;
    }
}
