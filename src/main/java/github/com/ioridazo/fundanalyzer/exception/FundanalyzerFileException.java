package github.com.ioridazo.fundanalyzer.exception;

public class FundanalyzerFileException extends RuntimeException {

    public FundanalyzerFileException() {
    }

    public FundanalyzerFileException(final String message) {
        super(message);
    }

    public FundanalyzerFileException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public FundanalyzerFileException(final Throwable cause) {
        super(cause);
    }
}
