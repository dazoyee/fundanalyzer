package github.com.ioridazo.fundanalyzer.exception;

public class FundanalyzerCalculateException extends RuntimeException {

    public FundanalyzerCalculateException() {
    }

    public FundanalyzerCalculateException(final String message) {
        super(message);
    }

    public FundanalyzerCalculateException(final Throwable cause) {
        super(cause);
    }

    public FundanalyzerCalculateException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
