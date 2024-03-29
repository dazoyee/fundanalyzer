package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
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
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "analysis_result")
public class AnalysisResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Integer id;

    private final String companyCode;

    private final LocalDate documentPeriod;

    private final BigDecimal corporateValue;

    private final BigDecimal bps;

    private final BigDecimal eps;

    private final BigDecimal roe;

    private final BigDecimal roa;

    private final String documentTypeCode;

    private final String quarterType;

    private final LocalDate submitDate;

    private final String documentId;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    public static AnalysisResultEntity of(
            final String companyCode,
            final LocalDate period,
            final BigDecimal corporateValue,
            final BigDecimal bps,
            final BigDecimal eps,
            final BigDecimal roe,
            final BigDecimal roa,
            final DocumentTypeCode documentTypeCode,
            final QuarterType quarterType,
            final LocalDate submitDate,
            final String documentId,
            final LocalDateTime createdAt) {
        if (DocumentTypeCode.DTC_140 == documentTypeCode && Objects.isNull(quarterType.toValue())) {
            throw new FundanalyzerNotExistException("四半期種別");
        }

        return new AnalysisResultEntity(
                null,
                companyCode,
                period,
                corporateValue,
                bps,
                eps,
                roe,
                roa,
                documentTypeCode.toValue(),
                quarterType.toValue(),
                submitDate,
                documentId,
                createdAt
        );
    }

    public Optional<BigDecimal> getBps() {
        return Optional.ofNullable(bps);
    }

    public Optional<BigDecimal> getEps() {
        return Optional.ofNullable(eps);
    }

    public Optional<BigDecimal> getRoe() {
        return Optional.ofNullable(roe);
    }

    public Optional<BigDecimal> getRoa() {
        return Optional.ofNullable(roa);
    }
}
