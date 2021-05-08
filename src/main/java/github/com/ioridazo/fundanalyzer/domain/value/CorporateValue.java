package github.com.ioridazo.fundanalyzer.domain.value;

import lombok.Value;

import java.math.BigDecimal;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class CorporateValue {

    // 最新企業価値
    private final BigDecimal latestCorporateValue;
    // 平均企業価値
    private final BigDecimal averageCorporateValue;
    // 標準偏差
    private final BigDecimal standardDeviation;
    // 変動係数
    private final BigDecimal coefficientOfVariation;
    // 対象年カウント
    private final BigDecimal countYear;
}
