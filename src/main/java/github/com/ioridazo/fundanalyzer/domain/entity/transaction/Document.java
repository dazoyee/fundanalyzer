package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

@Builder
@Getter
@AllArgsConstructor
@Entity(immutable = true)
@Table(name = "document")
public class Document {

    @Id
    private final String docId;

    @Column(updatable = false)
    private final String docTypeCode;

    @Column(updatable = false)
    private final String filerName;

    private final String downloaded;

    private final String decoded;

    private final String scrapedBalanceSheet;

    private final String scrapedProfitAndLessStatement;

    private final String scrapedCashFlowStatement;
}
