package github.com.ioridazo.fundanalyzer.domain.value;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Optional;

@Data(staticConstructor = "of")
public class CorporateValue {

    // 最新企業価値
    private BigDecimal latestCorporateValue;
    // 平均企業価値
    private BigDecimal averageCorporateValue;
    // 標準偏差
    private BigDecimal standardDeviation;
    // 変動係数
    private BigDecimal coefficientOfVariation;
    // 対象年カウント
    private BigDecimal countYear;

    public Optional<BigDecimal> getLatestCorporateValue() {
        return Optional.ofNullable(latestCorporateValue);
    }

    public Optional<BigDecimal> getAverageCorporateValue() {
        return Optional.ofNullable(averageCorporateValue);
    }

    public Optional<BigDecimal> getStandardDeviation() {
        return Optional.ofNullable(standardDeviation);
    }

    public Optional<BigDecimal> getCoefficientOfVariation() {
        return Optional.ofNullable(coefficientOfVariation);
    }

    public Optional<BigDecimal> getCountYear() {
        return Optional.ofNullable(countYear);
    }
}
