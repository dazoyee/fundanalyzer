package github.com.ioridazo.fundanalyzer.domain.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Optional;

@Data
public class AverageInfo {

    private Year year;
    // 平均企業価値
    private BigDecimal averageCorporateValue;
    // 標準偏差
    private BigDecimal standardDeviation;
    // 変動係数
    private BigDecimal coefficientOfVariation;

    public Optional<BigDecimal> getAverageCorporateValue() {
        return Optional.ofNullable(averageCorporateValue);
    }

    public Optional<BigDecimal> getStandardDeviation() {
        return Optional.ofNullable(standardDeviation);
    }

    public Optional<BigDecimal> getCoefficientOfVariation() {
        return Optional.ofNullable(coefficientOfVariation);
    }

    public enum Year {
        THREE,
        FIVE,
        TEN,
        ALL,
        UNKNOWN
    }

    @JsonCreator
    public static Year yearFromValue(final Integer value) {
        switch (value) {
            case 3:
                return Year.THREE;
            case 5:
                return Year.FIVE;
            case 10:
                return Year.TEN;
            default:
                return Year.UNKNOWN;
        }
    }

    public static Integer parseYear(final Year year) {
        switch (year) {
            case THREE:
                return 3;
            case FIVE:
                return 5;
            case TEN:
                return 10;
            default:
                throw new FundanalyzerRuntimeException();
        }
    }
}