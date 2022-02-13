package github.com.ioridazo.fundanalyzer.exception;

public class FundanalyzerScrapingException extends RuntimeException {

    public FundanalyzerScrapingException(final String message) {
        super(message);
    }

    public FundanalyzerScrapingException(final Throwable cause) {
        super(cause);
    }

    public FundanalyzerScrapingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
