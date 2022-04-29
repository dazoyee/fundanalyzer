package github.com.ioridazo.fundanalyzer.exception;

public class FundanalyzerRateLimiterException extends RuntimeException {

    public FundanalyzerRateLimiterException(final String message) {
        super(message);
    }
}
