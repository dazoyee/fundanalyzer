package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnalysisResultViewModel(
        LocalDate submitDate,
        LocalDate documentPeriod,
        BigDecimal corporateValue,
        BigDecimal rimValue,
        String documentTypeCode,
        String quarterType
) {
    public static AnalysisResultViewModel of(final AnalysisResultEntity entity) {
        return new AnalysisResultViewModel(
                entity.getSubmitDate(),
                entity.getDocumentPeriod(),
                entity.getCorporateValue(),
                entity.getRimValue().orElse(null),
                entity.getDocumentTypeCode(),
                entity.getQuarterType()
        );
    }
}
