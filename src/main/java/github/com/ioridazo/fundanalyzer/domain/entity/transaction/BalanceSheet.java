package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import lombok.Value;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;

@Value
@Entity(immutable = true)
@Table(name = "balance_sheet")
public class BalanceSheet {

    @Id
    String id;

    String companyCode;

    String financialStatementId;

    String detailId;

    LocalDate period;

    int value;
}
