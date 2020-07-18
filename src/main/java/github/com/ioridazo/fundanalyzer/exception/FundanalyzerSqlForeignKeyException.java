package github.com.ioridazo.fundanalyzer.exception;

public class FundanalyzerSqlForeignKeyException extends RuntimeException {

    public FundanalyzerSqlForeignKeyException() {
    }

    public FundanalyzerSqlForeignKeyException(final String message) {
        super(message);
    }

    public FundanalyzerSqlForeignKeyException(Throwable cause) {
        super(cause);
    }

    public FundanalyzerSqlForeignKeyException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
