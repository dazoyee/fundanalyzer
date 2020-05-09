package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;

@Entity(immutable = true)
@Table(name = "balance_sheet")
public class BalanceSheet extends FinancialStatement {

    @Id
    private final String id;

    private final String companyCode;

    private final String financialStatementId;

    private final String detailId;

    private final LocalDate period;

    private final int value;

    public BalanceSheet(
            String id,
            String companyCode,
            String financialStatementId,
            String detailId,
            LocalDate period,
            int value) {
        super(id, companyCode, financialStatementId, detailId, period, value);
        this.id = id;
        this.companyCode = companyCode;
        this.financialStatementId = financialStatementId;
        this.detailId = detailId;
        this.period = period;
        this.value = value;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getCompanyCode() {
        return companyCode;
    }

    @Override
    public String getFinancialStatementId() {
        return financialStatementId;
    }

    @Override
    public String getDetailId() {
        return detailId;
    }

    @Override
    public LocalDate getPeriod() {
        return period;
    }

    @Override
    public int getValue() {
        return value;
    }
}
