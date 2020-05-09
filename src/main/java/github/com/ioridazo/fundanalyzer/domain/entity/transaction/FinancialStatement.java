package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import java.time.LocalDate;

public class FinancialStatement {

    String id;

    String companyCode;

    String financialStatementId;

    String detailId;

    LocalDate period;

    int value;

    public FinancialStatement(
            String id,
            String companyCode,
            String financialStatementId,
            String detailId,
            LocalDate period,
            int value) {
        this.id = id;
        this.companyCode = companyCode;
        this.financialStatementId = financialStatementId;
        this.detailId = detailId;
        this.period = period;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getFinancialStatementId() {
        return financialStatementId;
    }

    public String getDetailId() {
        return detailId;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public int getValue() {
        return value;
    }
}
