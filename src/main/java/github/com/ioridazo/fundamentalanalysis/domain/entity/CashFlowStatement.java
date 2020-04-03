package github.com.ioridazo.fundamentalanalysis.domain.entity;

import lombok.Value;

import java.time.LocalDate;

@Value
public class CashFlowStatement {

    private String id;

    private String companyId;

    private String financialStatementId;

    private String detailId;

    private LocalDate period;

    private int value;
}
