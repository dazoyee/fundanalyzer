package github.com.ioridazo.fundanalyzer.exception;

public class FundanalyzerBadDataException extends RuntimeException {

    public FundanalyzerBadDataException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
