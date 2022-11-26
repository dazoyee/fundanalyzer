package github.com.ioridazo.fundanalyzer.domain.value;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.InvestmentIndicatorEntity;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@Getter
public class IndicatorValue {

    private final BigDecimal priceCorporateValueRatio;

    /*
     * PER（株価収益率） = 株価 / EPS（1株あたり純利益）
     * 株価が1株あたりの純利益の何倍になっているのか
     */
    private final BigDecimal per;

    /*
     * PBR（株価純資産倍率）= 株価 / BPS（1株あたり純資産）
     * 1株あたりの純資産に対して、何倍の株価で株が買われているか
     */
    private final BigDecimal pbr;

    /*
     * グレアム指数 = PER * PBR
     * ベンジャミン・グレアムが提唱した割安さを測る指数
     */
    private final BigDecimal grahamIndex;

    private final LocalDate targetDate;

    private static final int TENTH_DECIMAL_PLACE = 10;

    public IndicatorValue(
            final BigDecimal priceCorporateValueRatio,
            final BigDecimal per,
            final BigDecimal pbr,
            final BigDecimal grahamIndex,
            final LocalDate targetDate) {
        this.priceCorporateValueRatio = priceCorporateValueRatio;
        this.per = per;
        this.pbr = pbr;
        this.grahamIndex = grahamIndex;
        this.targetDate = targetDate;
    }

    public IndicatorValue(
            final BigDecimal stockPrice, final AnalysisResultEntity analysisResultEntity) {
        this.priceCorporateValueRatio = calculatePriceCorporateValueRatio(stockPrice, analysisResultEntity);
        this.per = calculatePer(stockPrice, analysisResultEntity).orElse(null);
        this.pbr = calculatePbr(stockPrice, analysisResultEntity).orElse(null);
        this.grahamIndex = calculateGrahamIndex(this.per, this.pbr).orElse(null);
        this.targetDate = analysisResultEntity.getSubmitDate();
    }

    public static IndicatorValue of() {
        return new IndicatorValue(
                null,
                null,
                null,
                null,
                null
        );
    }

    public static IndicatorValue of(final InvestmentIndicatorEntity entity) {
        return new IndicatorValue(
                entity.getPriceCorporateValueRatio(),
                entity.getPer().orElse(null),
                entity.getPbr().orElse(null),
                entity.getGrahamIndex().orElse(null),
                entity.getTargetDate()
        );
    }

    public Optional<BigDecimal> getPer() {
        return Optional.ofNullable(per);
    }

    public Optional<BigDecimal> getPbr() {
        return Optional.ofNullable(pbr);
    }

    public Optional<BigDecimal> getGrahamIndex() {
        return Optional.ofNullable(grahamIndex);
    }

    BigDecimal calculatePriceCorporateValueRatio(
            final BigDecimal stockPrice, final AnalysisResultEntity analysisResultEntity) {
        return stockPrice.divide(analysisResultEntity.getCorporateValue(), TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP);
    }

    Optional<BigDecimal> calculatePer(
            final BigDecimal stockPrice, final AnalysisResultEntity analysisResultEntity) {
        return analysisResultEntity.getEps()
                .map(eps -> stockPrice.divide(eps, TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP));
    }

    Optional<BigDecimal> calculatePbr(
            final BigDecimal stockPrice, final AnalysisResultEntity analysisResultEntity) {
        return analysisResultEntity.getBps()
                .map(bps -> stockPrice.divide(bps, TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP));
    }

    Optional<BigDecimal> calculateGrahamIndex(final BigDecimal per, final BigDecimal pbr) {
        if (Objects.nonNull(per) && Objects.nonNull(pbr)) {
            return Optional.of(per.multiply(pbr));
        } else {
            return Optional.empty();
        }
    }
}
