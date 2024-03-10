package github.com.ioridazo.fundanalyzer.exception;

public class FundanalyzerCircuitBreakerRecordException extends RuntimeException {

    public FundanalyzerCircuitBreakerRecordException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
