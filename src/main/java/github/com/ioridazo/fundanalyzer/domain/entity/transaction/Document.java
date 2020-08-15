package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
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

    public boolean getNotRemoved() {
        return "0".equals(removed);
    }
}
