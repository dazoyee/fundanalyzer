package github.com.ioridazo.fundanalyzer.web.model;

import lombok.Data;

@Data
public class FinancialStatementInputData {

    private String edinetCode;

    private String documentId;

    private String financialStatementId;

    private String subjectId;

    private long value;

    public static FinancialStatementInputData of(
            final String edinetCode,
            final String documentId,
            final String financialStatementId,
            final String subjectId,
            final long value) {
        final FinancialStatementInputData inputData = new FinancialStatementInputData();
        inputData.setEdinetCode(edinetCode);
        inputData.setDocumentId(documentId);
        inputData.setFinancialStatementId(financialStatementId);
        inputData.setSubjectId(subjectId);
        inputData.setValue(value);
        return inputData;
    }
}
