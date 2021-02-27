package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "analysis_result")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Integer id;

    private final String companyCode;

    private final LocalDate period;

    private final BigDecimal corporateValue;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    public static AnalysisResult of(
            final String companyCode,
            final LocalDate period,
            final BigDecimal corporateValue,
            final LocalDateTime createdAt) {
        return new AnalysisResult(
                null,
                companyCode,
                period,
                corporateValue,
                createdAt
        );
    }
}
