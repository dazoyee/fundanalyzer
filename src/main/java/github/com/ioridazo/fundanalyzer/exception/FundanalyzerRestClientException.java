package github.com.ioridazo.fundanalyzer.exception;

import org.springframework.web.client.RestClientException;

public class FundanalyzerRestClientException extends RestClientException {

    public FundanalyzerRestClientException(final String message) {
        super(message);
    }

    public FundanalyzerRestClientException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
