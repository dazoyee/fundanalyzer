package github.com.ioridazo.fundanalyzer.domain.entity.master;

import lombok.Value;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

@Value
@Entity(immutable = true)
@Table(name = "balance_sheet_detail")
public class BalanceSheetDetail {

    @Id
    String id;

    String subjectId;

    String name;
}
