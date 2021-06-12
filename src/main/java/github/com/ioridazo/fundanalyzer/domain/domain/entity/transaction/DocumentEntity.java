package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.Results;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.Builder;
import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Builder
@Value
@Entity(immutable = true)
@Table(name = "document")
public class DocumentEntity {

    private final Integer id;

    @Id
    private final String documentId;

    @Column(updatable = false)
    private final String documentTypeCode;

    @Column(updatable = false)
    private final String edinetCode;

    @Column(updatable = false)
    private final LocalDate documentPeriod;

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

    public static DocumentEntity of(
            final LocalDate submitDate,
            final LocalDate documentPeriod,
            final Results results,
            final LocalDateTime nowLocalDateTime) {
        return DocumentEntity.builder()
                .documentId(results.getDocId())
                .documentTypeCode(results.getDocTypeCode().orElse(null))
                .edinetCode(results.getEdinetCode().orElse(null))
                .documentPeriod(documentPeriod)
                .submitDate(submitDate)
                .createdAt(nowLocalDateTime)
                .updatedAt(nowLocalDateTime)
                .build();
    }

    public static DocumentEntity ofUpdateStoreToDone(final Document document, final LocalDateTime updatedAt) {
        return DocumentEntity.builder()
                .documentId(document.getDocumentId())
                .downloaded(DocumentStatus.DONE.toValue())
                .decoded(DocumentStatus.DONE.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static DocumentEntity ofUpdateDownloadToDone(final Document document, final LocalDateTime nowLocalDateTime) {
        return DocumentEntity.builder()
                .documentId(document.getDocumentId())
                .downloaded(DocumentStatus.DONE.toValue())
                .updatedAt(nowLocalDateTime)
                .build();
    }

    public static DocumentEntity ofUpdateDownloadToError(final Document document, final LocalDateTime updatedAt) {
        return DocumentEntity.builder()
                .documentId(document.getDocumentId())
                .downloaded(DocumentStatus.ERROR.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static DocumentEntity ofUpdateDecodeToDone(final Document document, final LocalDateTime updatedAt) {
        return DocumentEntity.builder()
                .documentId(document.getDocumentId())
                .decoded(DocumentStatus.DONE.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static DocumentEntity ofUpdateDecodeToError(final Document document, final LocalDateTime updatedAt) {
        return DocumentEntity.builder()
                .documentId(document.getDocumentId())
                .decoded(DocumentStatus.ERROR.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static DocumentEntity ofUpdateRemoved(final Document document, final LocalDateTime updatedAt) {
        return DocumentEntity.builder()
                .documentId(document.getDocumentId())
                .removed(Flag.ON.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static DocumentEntity ofUpdateSwitchFs(
            final FinancialStatementEnum fs,
            final String documentId,
            final DocumentStatus status,
            final String path,
            final LocalDateTime updatedAt) {
        switch (fs) {
            case BALANCE_SHEET:
                return DocumentEntity.builder()
                        .documentId(documentId)
                        .scrapedBs(status.toValue())
                        .bsDocumentPath(path)
                        .updatedAt(updatedAt)
                        .build();
            case PROFIT_AND_LESS_STATEMENT:
                return DocumentEntity.builder()
                        .documentId(documentId)
                        .scrapedPl(status.toValue())
                        .plDocumentPath(path)
                        .updatedAt(updatedAt)
                        .build();
            case TOTAL_NUMBER_OF_SHARES:
                return DocumentEntity.builder()
                        .documentId(documentId)
                        .scrapedNumberOfShares(status.toValue())
                        .numberOfSharesDocumentPath(path)
                        .updatedAt(updatedAt)
                        .build();
            default:
                throw new FundanalyzerRuntimeException();
        }
    }

    public Optional<String> getDocumentTypeCode() {
        return Optional.ofNullable(documentTypeCode);
    }

    public Optional<String> getEdinetCode() {
        return Optional.ofNullable(edinetCode);
    }

    public Optional<LocalDate> getDocumentPeriod() {
        return Optional.ofNullable(documentPeriod);
    }

    public Optional<String> getNumberOfSharesDocumentPath() {
        return Optional.ofNullable(numberOfSharesDocumentPath);
    }

    public Optional<String> getBsDocumentPath() {
        return Optional.ofNullable(bsDocumentPath);
    }

    public Optional<String> getPlDocumentPath() {
        return Optional.ofNullable(plDocumentPath);
    }
}
