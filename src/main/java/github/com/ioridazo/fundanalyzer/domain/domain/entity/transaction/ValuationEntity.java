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

    private final BigDecimal dividendYield;

    private final BigDecimal goalsStock;

    private final Integer investmentIndicatorId;

    private final BigDecimal grahamIndex;

    private final Long daySinceSubmitDate;

    private final BigDecimal differenceFromSubmitDate;

    private final BigDecimal submitDateRatio;

    private final BigDecimal discountValue;

    private final BigDecimal discountRate;

    private final BigDecimal corporateValue;

    private final Integer valuationIdOfSubmitDate;

    private final BigDecimal stockPriceOfSubmitDate;

    private final BigDecimal grahamIndexOfSubmitDate;

    private final Integer analysisResultId;

    private final String documentId;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    // TODO
    public static ValuationEntity of(
            final String companyCode,
            final LocalDate submitDate,
            final LocalDate targetDate,
            final BigDecimal stockPrice,
            final BigDecimal goalsStock,
            final BigDecimal grahamIndex,
            final Long monthSinceSubmitDate,
            final BigDecimal differenceFromSubmitDate,
            final BigDecimal submitDateRatio,
            final BigDecimal discountValue,
            final BigDecimal discountRate,
            final BigDecimal corporateValue,
            final BigDecimal stockPriceOfSubmitDate,
            final BigDecimal grahamIndexOfSubmitDate,
            final String documentId,
            final LocalDateTime nowLocalDateTime) {
        return new ValuationEntity(
                null,
                companyCode,
                submitDate,
                targetDate,
                null,
                stockPrice,
                null,
                goalsStock,
                null,
                grahamIndex,
                monthSinceSubmitDate,
                differenceFromSubmitDate,
                submitDateRatio,
                discountValue,
                discountRate,
                corporateValue,
                null,
                stockPriceOfSubmitDate,
                grahamIndexOfSubmitDate,
                null,
                documentId,
                nowLocalDateTime
        );
    }

    public Optional<Integer> getStockPriceId() {
        return Optional.ofNullable(stockPriceId);
    }

    public Optional<BigDecimal> getDividendYield() {
        return Optional.ofNullable(dividendYield);
    }

    public Optional<BigDecimal> getGoalsStock() {
        return Optional.ofNullable(goalsStock);
    }

    public Optional<Integer> getInvestmentIndicatorId() {
        return Optional.ofNullable(investmentIndicatorId);
    }

    public Optional<BigDecimal> getGrahamIndex() {
        return Optional.ofNullable(grahamIndex);
    }

    public Optional<Integer> getValuationIdOfSubmitDate() {
        return Optional.ofNullable(valuationIdOfSubmitDate);
    }

    public Optional<BigDecimal> getGrahamIndexOfSubmitDate() {
        return Optional.ofNullable(grahamIndexOfSubmitDate);
    }

    // TODO
    public Optional<Integer> getAnalysisResultId() {
        return Optional.ofNullable(analysisResultId);
    }
}
