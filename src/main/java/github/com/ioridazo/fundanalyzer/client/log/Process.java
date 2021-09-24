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
    ;

    private final String value;

    Process(final String value) {
        this.value = value;
    }

    public static Process of(final FinancialStatementEnum fs) {
        switch (fs) {
            case BALANCE_SHEET:
                return BS;
            case PROFIT_AND_LESS_STATEMENT:
                return PL;
            case TOTAL_NUMBER_OF_SHARES:
                return NS;
            default:
                throw new FundanalyzerRuntimeException();
        }
    }

    public static Process of(final ScrapingKeywordEntity scrapingKeywordEntity) {
        return of(FinancialStatementEnum.fromId(scrapingKeywordEntity.getFinancialStatementId()));
    }

    public String getValue() {
        return value;
    }
}
