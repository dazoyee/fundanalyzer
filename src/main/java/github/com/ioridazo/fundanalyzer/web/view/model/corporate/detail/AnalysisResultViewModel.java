package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResultEntity;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class AnalysisResultViewModel {
    private final LocalDate documentPeriod;
    private final BigDecimal corporateValue;

    public static AnalysisResultViewModel of(final AnalysisResultEntity entity) {
        return new AnalysisResultViewModel(
                entity.getDocumentPeriod(),
                entity.getCorporateValue()
        );
    }
}
