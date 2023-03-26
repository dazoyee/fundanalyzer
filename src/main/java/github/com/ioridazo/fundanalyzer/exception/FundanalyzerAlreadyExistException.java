package github.com.ioridazo.fundanalyzer.exception;

import java.time.LocalDate;

public class FundanalyzerAlreadyExistException extends RuntimeException {

    public FundanalyzerAlreadyExistException(final String companyCode, final LocalDate targetDate) {
        this.companyCode = companyCode;
        this.targetDate = targetDate;
    }

    private final String companyCode;

    private final LocalDate targetDate;

    public String getCompanyCode() {
        return companyCode;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }
}
