package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.Builder;
import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Builder
@Entity(immutable = true)
@Table(name = "document")
public class Document {

    private final Integer id;

    @Id
    private final String documentId;

    @Column(updatable = false)
    private final String documentTypeCode;

    @Column(updatable = false)
    private final String edinetCode;

    @Column(updatable = false)
    private final LocalDate period;

    @Column(updatable = false)
    private final LocalDate submitDate;

    private final String downloaded;

    private final String decoded;

    private final String scrapedNumberOfShares;

    private final String numberOfSharesDocumentPath;

    private final String scrapedBs;

    private final String bsDocumentPath;

    private final String scrapedPl;

    private final String plDocumentPath;

    private final String scrapedCf;

    private final String cfDocumentPath;

    private final String removed;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;

    public static Document ofUpdated(
            final FinancialStatementEnum fs,
            final String documentId,
            final DocumentStatus status,
            final String path,
            final LocalDateTime updatedAt) {
        switch (fs) {
            case BALANCE_SHEET:
                return Document.builder()
                        .documentId(documentId)
                        .scrapedBs(status.toValue())
                        .bsDocumentPath(path)
                        .updatedAt(updatedAt)
                        .build();
            case PROFIT_AND_LESS_STATEMENT:
                return Document.builder()
                        .documentId(documentId)
                        .scrapedPl(status.toValue())
                        .plDocumentPath(path)
                        .updatedAt(updatedAt)
                        .build();
            case TOTAL_NUMBER_OF_SHARES:
                return Document.builder()
                        .documentId(documentId)
                        .scrapedNumberOfShares(status.toValue())
                        .numberOfSharesDocumentPath(path)
                        .updatedAt(updatedAt)
                        .build();
            default:
                throw new FundanalyzerRuntimeException();
        }
    }

    public boolean getNotRemoved() {
        return "0".equals(removed);
    }
}
