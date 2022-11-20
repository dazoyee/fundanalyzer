package github.com.ioridazo.fundanalyzer.domain.value;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private static final int TENTH_DECIMAL_PLACE = 10;

    public IndicatorValue(
            final BigDecimal priceCorporateValueRatio,
            final BigDecimal per,
            final BigDecimal pbr) {
        this.priceCorporateValueRatio = priceCorporateValueRatio;
        this.per = per;
        this.pbr = pbr;
    }

    public IndicatorValue(
            final BigDecimal stockPrice, final AnalysisResultEntity analysisResultEntity) {
        this.priceCorporateValueRatio = calculatePriceCorporateValueRatio(stockPrice, analysisResultEntity);
        this.per = calculatePer(stockPrice, analysisResultEntity).orElse(null);
        this.pbr = calculatePbr(stockPrice, analysisResultEntity).orElse(null);
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
}
