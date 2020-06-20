package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;

@Builder
@Getter
@AllArgsConstructor
@Entity(immutable = true)
@Table(name = "document")
public class Document {

    private final Integer id;

    @Id
    private final String docId;

    @Column(updatable = false)
    private final String docTypeCode;

    @Column(updatable = false)
    private final String edinetCode;

    @Column(updatable = false)
    private final LocalDate submitDate;

    private final String downloaded;

    private final String decoded;

    private final String scrapedNumberOfShares;

    private final String scrapedBalanceSheet;

    private final String scrapedProfitAndLessStatement;

    private final String scrapedCashFlowStatement;
}
