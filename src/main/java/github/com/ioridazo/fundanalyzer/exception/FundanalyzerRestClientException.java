package github.com.ioridazo.fundanalyzer.exception;

import org.springframework.web.client.RestClientException;

public class FundanalyzerRestClientException extends RestClientException {

    private final boolean isRetry;

    public FundanalyzerRestClientException(final String message) {
        super(message);
        this.isRetry = false;
    }

    public FundanalyzerRestClientException(final String message, final boolean isRetry) {
        super(message);
        this.isRetry = isRetry;
    }

    public FundanalyzerRestClientException(final String message, final Throwable throwable) {
        super(message, throwable);
        this.isRetry = false;
    }

    public boolean isRetry() {
        return isRetry;
    }
}
