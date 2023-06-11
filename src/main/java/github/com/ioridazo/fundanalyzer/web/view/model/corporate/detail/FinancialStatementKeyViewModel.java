package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;

import java.time.LocalDate;

public record FinancialStatementKeyViewModel(
        LocalDate periodStart,
        LocalDate periodEnd,
        String documentTypeCode,
        String documentTypeName,
        LocalDate submitDate
) {

    public static FinancialStatementKeyViewModel of(final FinancialStatementEntity entity) {
        final DocumentTypeCode documentTypeCode = DocumentTypeCode.fromValue(entity.getDocumentTypeCode());
        return new FinancialStatementKeyViewModel(
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                documentTypeCode.toValue(),
                documentTypeCode.getName(),
                entity.getSubmitDate()
        );
    }
}
