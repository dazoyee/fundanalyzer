package github.com.ioridazo.fundanalyzer.exception;

public class FundanalyzerRuntimeException extends RuntimeException {

    public FundanalyzerRuntimeException() {
    }

    public FundanalyzerRuntimeException(final String message) {
        super(message);
    }

    public FundanalyzerRuntimeException(final Throwable cause) {
        super(cause);
    }

    public FundanalyzerRuntimeException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
