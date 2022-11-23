package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class AnalysisResultViewModel {
    private final LocalDate submitDate;
    private final LocalDate documentPeriod;
    private final BigDecimal corporateValue;
    private final String documentTypeCode;
    private final String quarterType;

    public static AnalysisResultViewModel of(final AnalysisResultEntity entity) {
        return new AnalysisResultViewModel(
                entity.submitDate(),
                entity.documentPeriod(),
                entity.corporateValue(),
                entity.documentTypeCode(),
                entity.quarterType()
        );
    }
}
