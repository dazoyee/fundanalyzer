package github.com.ioridazo.fundanalyzer.domain.value;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import lombok.Value;

import java.time.LocalDate;
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class Document {

    private final String documentId;

    private final DocumentTypeCode documentTypeCode;

    private final QuarterType quarterType;

    private final String edinetCode;

    private final LocalDate documentPeriod;

    private final LocalDate submitDate;

    private final LocalDate periodStart;

    private final LocalDate periodEnd;

    private final DocumentStatus downloaded;

    private final DocumentStatus decoded;

    private final DocumentStatus scrapedNumberOfShares;

    private final String numberOfSharesDocumentPath;

    private final DocumentStatus scrapedBs;

    private final String bsDocumentPath;

    private final DocumentStatus scrapedPl;

    private final String plDocumentPath;

    private final boolean isRemove;

    public static Document of(final DocumentEntity entity, final EdinetDocument edinetDocument) {
        return new Document(
                entity.getDocumentId(),
                DocumentTypeCode.fromValue(entity.getDocumentTypeCode().orElse(null)),
                QuarterType.fromDocDescription(edinetDocument.getDocDescription().orElse(null)),
                entity.getEdinetCode().orElse(null),
                entity.getDocumentPeriod().orElse(null),
                entity.getSubmitDate(),
                edinetDocument.getPeriodStart().orElse(null),
                edinetDocument.getPeriodEnd().orElse(null),
                DocumentStatus.fromValue(entity.getDownloaded()),
                DocumentStatus.fromValue(entity.getDecoded()),
                DocumentStatus.fromValue(entity.getScrapedNumberOfShares()),
                entity.getNumberOfSharesDocumentPath().orElse(null),
                DocumentStatus.fromValue(entity.getScrapedBs()),
                entity.getBsDocumentPath().orElse(null),
                DocumentStatus.fromValue(entity.getScrapedPl()),
                entity.getPlDocumentPath().orElse(null),
                "1".equals(entity.getRemoved())
        );
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

    public Optional<String> getFsDocumentPath(final FinancialStatementEnum fs) {
        return switch (fs) {
            case BALANCE_SHEET -> getBsDocumentPath();
            case PROFIT_AND_LESS_STATEMENT -> getPlDocumentPath();
            case TOTAL_NUMBER_OF_SHARES -> getNumberOfSharesDocumentPath();
            default -> Optional.empty();
        };
    }

    public boolean isTarget() {
        return !isRemove;
    }
}
