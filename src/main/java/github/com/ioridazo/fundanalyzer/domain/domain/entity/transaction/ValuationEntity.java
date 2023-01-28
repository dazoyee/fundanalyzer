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
@Table(name = "valuation")
public class ValuationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Integer id;

    private final String companyCode;

    private final LocalDate submitDate;

    private final LocalDate targetDate;

    private final Integer stockPriceId;

    private final BigDecimal stockPrice;

    private final Integer investmentIndicatorId;

    private final BigDecimal grahamIndex;

    private final Long daySinceSubmitDate;

    private final BigDecimal differenceFromSubmitDate;

    private final BigDecimal submitDateRatio;

    private final BigDecimal discountValue;

    private final BigDecimal discountRate;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    public static ValuationEntity of(
            final String companyCode,
            final LocalDate submitDate,
            final LocalDate targetDate,
            final Integer stockPriceId,
            final BigDecimal stockPrice,
            final Integer investmentIndicatorId,
            final BigDecimal grahamIndex,
            final Long monthSinceSubmitDate,
            final BigDecimal differenceFromSubmitDate,
            final BigDecimal submitDateRatio,
            final BigDecimal discountValue,
            final BigDecimal discountRate,
            final LocalDateTime nowLocalDateTime) {
        return new ValuationEntity(
                null,
                companyCode,
                submitDate,
                targetDate,
                stockPriceId,
                stockPrice,
                investmentIndicatorId,
                grahamIndex,
                monthSinceSubmitDate,
                differenceFromSubmitDate,
                submitDateRatio,
                discountValue,
                discountRate,
                nowLocalDateTime
        );
    }

    public Optional<Integer> getStockPriceId() {
        return Optional.ofNullable(stockPriceId);
    }

    public Optional<Integer> getInvestmentIndicatorId() {
        return Optional.ofNullable(investmentIndicatorId);
    }

    public Optional<BigDecimal> getGrahamIndex() {
        return Optional.ofNullable(grahamIndex);
    }
}
