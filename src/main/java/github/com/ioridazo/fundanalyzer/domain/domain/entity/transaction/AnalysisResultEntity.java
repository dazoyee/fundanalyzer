package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
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

@Entity(immutable = true)
@Table(name = "analysis_result")
public record AnalysisResultEntity(

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Integer id,

        String companyCode,

        LocalDate documentPeriod,

        BigDecimal corporateValue,

        BigDecimal bps,

        BigDecimal eps,

        BigDecimal roe,

        BigDecimal roa,

        String documentTypeCode,

        String quarterType,

        LocalDate submitDate,

        String documentId,

        @Column(updatable = false)
        LocalDateTime createdAt
) {

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
}
