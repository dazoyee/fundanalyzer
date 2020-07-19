package github.com.ioridazo.fundanalyzer.exception;

import java.io.IOException;

public class FundanalyzerFileException extends IOException {

    public FundanalyzerFileException() {
    }

    public FundanalyzerFileException(final String message) {
        super(message);
    }

    public FundanalyzerFileException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public FundanalyzerFileException(Throwable cause) {
        super(cause);
    }
}
