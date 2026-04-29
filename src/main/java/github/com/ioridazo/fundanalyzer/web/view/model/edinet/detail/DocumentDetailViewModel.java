package github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail;

import github.com.ioridazo.fundanalyzer.domain.value.Document;

import java.time.LocalDate;

/**
 * ドキュメント詳細ビュー
 */
public record DocumentDetailViewModel(
        String documentId,
        String documentTypeCode,
        String documentTypeName,
        String edinetCode,
        LocalDate documentPeriod,
        String downloaded,
        String decoded,
        String scrapedNumberOfShares,
        String numberOfSharesDocumentPath,
        String scrapedBs,
        String bsDocumentPath,
        String scrapedPl,
        String plDocumentPath) {

    /**
     * Document からビューを生成する
     *
     * @param document ドキュメント
     * @return DocumentDetailViewModel
     */
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

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentTypeCode() {
        return documentTypeCode;
    }

    public String getDocumentTypeName() {
        return documentTypeName;
    }

    public String getEdinetCode() {
        return edinetCode;
    }

    public LocalDate getDocumentPeriod() {
        return documentPeriod;
    }

    public String getDownloaded() {
        return downloaded;
    }

    public String getDecoded() {
        return decoded;
    }

    public String getScrapedNumberOfShares() {
        return scrapedNumberOfShares;
    }

    public String getNumberOfSharesDocumentPath() {
        return numberOfSharesDocumentPath;
    }

    public String getScrapedBs() {
        return scrapedBs;
    }

    public String getBsDocumentPath() {
        return bsDocumentPath;
    }

    public String getScrapedPl() {
        return scrapedPl;
    }

    public String getPlDocumentPath() {
        return plDocumentPath;
    }
}
