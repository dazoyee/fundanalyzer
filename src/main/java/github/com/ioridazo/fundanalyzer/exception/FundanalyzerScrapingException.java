package github.com.ioridazo.fundanalyzer.exception;

public class FundanalyzerScrapingException extends RuntimeException {

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public FundanalyzerScrapingException(final String message) {
        super(message);
    }

    public FundanalyzerScrapingException(final Throwable cause) {
        super(cause);
    }
}
