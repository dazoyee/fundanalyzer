package github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail;

import github.com.ioridazo.fundanalyzer.domain.value.Document;
import lombok.Value;

import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class DocumentDetailViewModel {
    private final String documentId;
    private final String documentTypeCode;
    private final String documentTypeName;
    private final String edinetCode;
    private final LocalDate documentPeriod;
    private final String downloaded;
    private final String decoded;
    private final String scrapedNumberOfShares;
    private final String numberOfSharesDocumentPath;
    private final String scrapedBs;
    private final String bsDocumentPath;
    private final String scrapedPl;
    private final String plDocumentPath;

    public static DocumentDetailViewModel of(final Document document) {
        return new DocumentDetailViewModel(
                document.getDocumentId(),
                document.getDocumentTypeCode().toValue(),
                document.getDocumentTypeCode().getName(),
                document.getEdinetCode(),
                document.getDocumentPeriod().orElse(null),
                document.getDownloaded().toValue(),
                document.getDecoded().toValue(),
                document.getScrapedNumberOfShares().toValue(),
                document.getNumberOfSharesDocumentPath().orElse(null),
                document.getScrapedBs().toValue(),
                document.getBsDocumentPath().orElse(null),
                document.getScrapedPl().toValue(),
                document.getPlDocumentPath().orElse(null)
        );
    }
}
