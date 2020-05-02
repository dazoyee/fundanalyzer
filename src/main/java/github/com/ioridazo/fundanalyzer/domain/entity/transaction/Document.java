package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import lombok.Builder;
import lombok.Data;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

@Builder
@Data
//@Entity // todo イミュータブル
@Table(name = "document")
public class Document {

    @Id
    private String docId;

    private String docTypeCode;

    private String downloaded;

    private String decoded;

    private String scrapedBalanceSheet;

    private String scrapedProfitAndLessStatement;

    private String scrapedCashFlowStatement;
}
