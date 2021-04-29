package github.com.ioridazo.fundanalyzer.exception;

import java.text.MessageFormat;

public class FundanalyzerNotExistException extends RuntimeException {

    private static final String MESSAGE = "次の値が存在していません。\t{0}";

    public FundanalyzerNotExistException(final String... parameter) {
        super(MessageFormat.format(MESSAGE, String.join(",", parameter)));
    }
}
