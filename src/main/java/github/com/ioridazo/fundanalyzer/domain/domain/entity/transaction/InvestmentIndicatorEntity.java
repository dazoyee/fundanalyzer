package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

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
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "investment_indicator")
public class InvestmentIndicatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Integer id;

    private final Integer stockId;

    private final Integer analysisResultId;

    private final String companyCode;

    private final LocalDate targetDate;

    private final BigDecimal priceCorporateValueRatio;

    private final BigDecimal per;

    private final BigDecimal pbr;

    private final String documentId;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    public static InvestmentIndicatorEntity of(
            final Integer stockId,
            final Integer analysisResultId,
            final String companyCode,
            final LocalDate targetDate,
            final BigDecimal priceCorporateValueRatio,
            final BigDecimal per,
            final BigDecimal pbr,
            final String documentId,
            final LocalDateTime createdAt
    ) {
        return new InvestmentIndicatorEntity(
                null,
                stockId,
                analysisResultId,
                companyCode,
                targetDate,
                priceCorporateValueRatio,
                per,
                pbr,
                documentId,
                createdAt
        );
    }

    public Optional<BigDecimal> getPer() {
        return Optional.ofNullable(per);
    }

    public Optional<BigDecimal> getPbr() {
        return Optional.ofNullable(pbr);
    }
}
