package github.com.ioridazo.fundanalyzer.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * 企業価値算出に用いる係数。
 *
 * <p>既定値は {@link #defaults()} が現行のハードコード値（10 / 1.2 / 4）と一致する。
 * イミュータブル（コンストラクタバインド）のため、登録は {@code FundanalyzerApplication} の
 * {@code @EnableConfigurationProperties} で行う。
 */
@ConfigurationProperties("app.config.analysis")
@AllArgsConstructor
@Getter
public class AnalysisCoefficient {

    /** 営業利益の倍率（収益力評価）。 */
    private final BigDecimal operatingProfitWeight;

    /** 流動負債の調整係数。 */
    private final BigDecimal currentLiabilitiesRatio;

    /** 年換算の四半期重み（分母の四半期重み既定値と分子の年換算倍率を兼ねる）。 */
    private final BigDecimal annualWeight;

    /**
     * 現行のハードコード値と一致する既定の係数を返す。
     *
     * @return 既定係数（営業利益重み 10 / 流動比率 1.2 / 年換算重み 4）
     */
    public static AnalysisCoefficient defaults() {
        return new AnalysisCoefficient(
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(1.2),
                BigDecimal.valueOf(4)
        );
    }
}
