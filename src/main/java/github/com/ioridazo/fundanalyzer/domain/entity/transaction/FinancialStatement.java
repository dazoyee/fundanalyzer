package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import lombok.Value;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;

@Value
@Entity(immutable = true)
@Table(name = "financial_statement")
public class FinancialStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    String companyCode;

    String financialStatementId;

    String subjectId;

    LocalDate period;

    Long value;
}
