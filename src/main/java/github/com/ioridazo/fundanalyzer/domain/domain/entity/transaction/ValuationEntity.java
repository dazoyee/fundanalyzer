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

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "valuation")
public class ValuationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Integer id;

    private final String companyCode;

    private final LocalDate targetDate;

    private final BigDecimal stockPrice;

    private final BigDecimal goalsStock;

    private final Long daySinceSubmitDate;

    private final BigDecimal differenceFromSubmitDate;

    private final BigDecimal submitDateRatio;

    private final BigDecimal discountValue;

    private final BigDecimal discountRate;

    private final LocalDate submitDate;

    private final BigDecimal corporateValue;

    private final BigDecimal stockPriceOfSubmitDate;

    private final String documentId;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    public static ValuationEntity of(
            final String companyCode,
            final LocalDate targetDate,
            final BigDecimal stockPrice,
            final BigDecimal goalsStock,
            final Long monthSinceSubmitDate,
            final BigDecimal differenceFromSubmitDate,
            final BigDecimal submitDateRatio,
            final BigDecimal discountValue,
            final BigDecimal discountRate,
            final LocalDate submitDate,
            final BigDecimal corporateValue,
            final BigDecimal stockPriceOfSubmitDate,
            final String documentId,
            final LocalDateTime nowLocalDateTime) {
        return new ValuationEntity(
                null,
                companyCode,
                targetDate,
                stockPrice,
                goalsStock,
                monthSinceSubmitDate,
                differenceFromSubmitDate,
                submitDateRatio,
                discountValue,
                discountRate,
                submitDate,
                corporateValue,
                stockPriceOfSubmitDate,
                documentId,
                nowLocalDateTime
        );
    }
}
