package github.com.ioridazo.fundanalyzer.domain.value;

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

    /**
     * 都度計算値から投資指標を構築する。
     *
     * <p>投資指標の対象日は補正後株価の対象日（{@code targetDate}）であり、分析結果の提出日とは異なる
     * （分析結果は「提出日が対象日以前で最新のもの」が突合された結果であるため）。
     *
     * @param stockPrice     補正後株価
     * @param analysisResult 分析結果（都度計算値）
     * @param targetDate     投資指標の対象日（株価の対象日）
     */
    public IndicatorValue(
            final BigDecimal stockPrice, final AnalysisResult analysisResult, final LocalDate targetDate) {
        this.priceCorporateValueRatio = calculatePriceCorporateValueRatio(stockPrice, analysisResult);
        this.per = calculatePer(stockPrice, analysisResult).orElse(null);
        this.pbr = calculatePbr(stockPrice, analysisResult).orElse(null);
        this.grahamIndex = calculateGrahamIndex(this.per, this.pbr).orElse(null);
        this.targetDate = targetDate;
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

    /**
     * 都度計算値から投資指標を構築する。
     *
     * @param stockPrice     補正後株価
     * @param analysisResult 分析結果（都度計算値）
     * @param targetDate     投資指標の対象日（株価の対象日）
     * @return 投資指標
     */
    public static IndicatorValue of(
            final BigDecimal stockPrice, final AnalysisResult analysisResult, final LocalDate targetDate) {
        return new IndicatorValue(stockPrice, analysisResult, targetDate);
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
            final BigDecimal stockPrice, final AnalysisResult analysisResult) {
        return stockPrice.divide(analysisResult.getCorporateValue(), TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP);
    }

    Optional<BigDecimal> calculatePer(
            final BigDecimal stockPrice, final AnalysisResult analysisResult) {
        return analysisResult.getEps()
                .filter(eps -> eps.compareTo(BigDecimal.ZERO) > 0)
                .map(eps -> stockPrice.divide(eps, TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP));
    }

    Optional<BigDecimal> calculatePbr(
            final BigDecimal stockPrice, final AnalysisResult analysisResult) {
        return analysisResult.getBps()
                .filter(bps -> bps.compareTo(BigDecimal.ZERO) != 0)
                .map(bps -> stockPrice.divide(bps, TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP));
    }

    Optional<BigDecimal> calculateGrahamIndex(final BigDecimal per, final BigDecimal pbr) {
        if (Objects.nonNull(per) && Objects.nonNull(pbr)
                && per.compareTo(BigDecimal.ZERO) > 0
                && pbr.compareTo(BigDecimal.ZERO) > 0) {
            return Optional.of(per.multiply(pbr));
        } else {
            return Optional.empty();
        }
    }
}
