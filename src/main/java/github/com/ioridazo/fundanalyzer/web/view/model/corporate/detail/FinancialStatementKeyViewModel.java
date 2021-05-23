package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import lombok.Value;

import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class FinancialStatementKeyViewModel {
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final String documentTypeCode;
    private final String documentTypeName;
    private final LocalDate submitDate;

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
