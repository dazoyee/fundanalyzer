package github.com.ioridazo.fundanalyzer.exception;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Optional;

public class FundanalyzerNotExistException extends RuntimeException {

    private static final String MESSAGE = "次の値が存在していません。\t{0}";

    private FinancialStatementEnum fs;
    private String subjectName;
    private Document document;

    public FundanalyzerNotExistException(@NotNull final String... parameter) {
        super(MessageFormat.format(MESSAGE, String.join(",", parameter)));
    }

    public FundanalyzerNotExistException(
            final FinancialStatementEnum fs,
            final String subjectName,
            final Document document) {
        this.fs = fs;
        this.subjectName = subjectName;
        this.document = document;
    }

    public Optional<FinancialStatementEnum> getFs() {
        return Optional.ofNullable(fs);
    }

    public Optional<String> getSubjectName() {
        return Optional.ofNullable(subjectName);
    }

    public Optional<Document> getDocument() {
        return Optional.ofNullable(document);
    }
}
