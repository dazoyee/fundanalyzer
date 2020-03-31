package github.com.ioridazo.fundamentalanalysis.domain.entity;

import lombok.Value;

import java.time.LocalDate;

@Value
public class Zaim {

    private String id;

    private String companyId;

    private String financialStatementId;

    private String subjectId;

    private LocalDate period;

    private int value;
}
