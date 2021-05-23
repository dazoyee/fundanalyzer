package github.com.ioridazo.fundanalyzer.domain.domain.jsoup;

public enum Tag {
    TABLE("table"),
    TR("tr"),
    TD("td"),
    ;

    private final String name;

    Tag(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
