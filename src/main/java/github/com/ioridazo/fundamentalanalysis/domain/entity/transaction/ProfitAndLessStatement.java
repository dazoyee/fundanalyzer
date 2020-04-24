package github.com.ioridazo.fundamentalanalysis.domain.entity.transaction;

import lombok.Value;

import java.time.LocalDate;

@Value
public class ProfitAndLessStatement {

    private String id;

    private String companyCode;

    private String financialStatementId;

    private String detailId;

    private LocalDate period;

    private int value;
}
