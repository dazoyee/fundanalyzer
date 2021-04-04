package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.Flag;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.response.Results;
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

    public static Document of(
            final LocalDate submitDate,
            final LocalDate documentPeriod,
            final Results results,
            final LocalDateTime nowLocalDateTime) {
        return Document.builder()
                .documentId(results.getDocId())
                .documentTypeCode(results.getDocTypeCode())
                .edinetCode(results.getEdinetCode().orElse(null))
                .documentPeriod(documentPeriod)
                .submitDate(submitDate)
                .createdAt(nowLocalDateTime)
                .updatedAt(nowLocalDateTime)
                .build();
    }

    public static Document ofUpdateStoreToDone(final String documentId, final LocalDateTime updatedAt) {
        return Document.builder()
                .documentId(documentId)
                .downloaded(DocumentStatus.DONE.toValue())
                .decoded(DocumentStatus.DONE.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static Document ofUpdateDownloadToDone(final String documentId, final LocalDateTime updatedAt) {
        return Document.builder()
                .documentId(documentId)
                .downloaded(DocumentStatus.DONE.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static Document ofUpdateDownloadToError(final String documentId, final LocalDateTime updatedAt) {
        return Document.builder()
                .documentId(documentId)
                .downloaded(DocumentStatus.ERROR.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static Document ofUpdateDecodeToDone(final String documentId, final LocalDateTime updatedAt) {
        return Document.builder()
                .documentId(documentId)
                .decoded(DocumentStatus.DONE.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static Document ofUpdateDecodeToError(final String documentId, final LocalDateTime updatedAt) {
        return Document.builder()
                .documentId(documentId)
                .decoded(DocumentStatus.ERROR.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static Document ofUpdateBsToNotYet(final String documentId, final LocalDateTime updatedAt) {
        return Document.builder()
                .documentId(documentId)
                .scrapedBs(DocumentStatus.NOT_YET.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static Document ofUpdateBsToHalfWay(final String documentId, final LocalDateTime updatedAt) {
        return Document.builder()
                .documentId(documentId)
                .scrapedBs(DocumentStatus.HALF_WAY.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static Document ofUpdatePlToNotYet(final String documentId, final LocalDateTime updatedAt) {
        return Document.builder()
                .documentId(documentId)
                .scrapedPl(DocumentStatus.NOT_YET.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static Document ofUpdatePlToHalfWay(final String documentId, final LocalDateTime updatedAt) {
        return Document.builder()
                .documentId(documentId)
                .scrapedPl(DocumentStatus.HALF_WAY.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static Document ofUpdateNumberOfSharesToHalfWay(final String documentId, final LocalDateTime updatedAt) {
        return Document.builder()
                .documentId(documentId)
                .scrapedNumberOfShares(DocumentStatus.HALF_WAY.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static Document ofUpdateRemoved(final String documentId, final LocalDateTime updatedAt) {
        return Document.builder()
                .documentId(documentId)
                .removed(Flag.ON.toValue())
                .updatedAt(updatedAt)
                .build();
    }

    public static Document ofUpdateSwitchFs(
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
