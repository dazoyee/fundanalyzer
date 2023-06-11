package github.com.ioridazo.fundanalyzer.client.log;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.ScrapingKeywordEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;

public enum Process {
    BEGINNING("beginning"),
    END("end"),

    EDINET("edinet"),
    SCRAPING("scraping"),
    REMOVE("remove"),

    DOWNLOAD("download"),
    DECODE("decode"),
    BS("bs"),
    PL("pl"),
    NS("ns"),

    ANALYSIS("analysis"),
    IMPORT("import"),
    REGISTER("register"),
    UPDATE("update"),
    SLACK("slack"),
    EVALUATE("evaluate"),
    INDICATE("indicate"),
    ;

    private final String value;

    Process(final String value) {
        this.value = value;
    }

    public static Process of(final FinancialStatementEnum fs) {
        return switch (fs) {
            case BALANCE_SHEET -> BS;
            case PROFIT_AND_LESS_STATEMENT -> PL;
            case TOTAL_NUMBER_OF_SHARES -> NS;
            default -> throw new FundanalyzerRuntimeException();
        };
    }

    public static Process of(final ScrapingKeywordEntity scrapingKeywordEntity) {
        return of(FinancialStatementEnum.fromId(scrapingKeywordEntity.getFinancialStatementId()));
    }

    public String getValue() {
        return value;
    }
}
