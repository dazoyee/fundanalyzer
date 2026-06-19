package github.com.ioridazo.fundanalyzer.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 企業価値・RIM 算出に用いる業種別係数。
 *
 * <p>営業利益倍率・流動負債調整係数・資本コストを保持する値オブジェクト。値は業種マスタ（industry 列）に
 * 由来し、既定値も列 DEFAULT（10 / 1.2 / 0.08）としてテーブル側で保持する。年換算重みは業種非依存の
 * 不変定数のため本オブジェクトは持たず {@code AnalysisResult.ANNUAL_WEIGHT} で定義する。
 */
@AllArgsConstructor
@Getter
public class AnalysisCoefficient {

    /** 営業利益の倍率（収益力評価）。 */
    private final BigDecimal operatingProfitWeight;

    /** 流動負債の調整係数。 */
    private final BigDecimal currentLiabilitiesRatio;

    /** 資本コスト（RIM の割引率）。 */
    private final BigDecimal costOfEquity;

    /**
     * 資本コストを持たない簡易コンストラクタ（costOfEquity は null。企業価値算出のみ用いる場合・テスト用）。
     *
     * @param operatingProfitWeight   営業利益倍率
     * @param currentLiabilitiesRatio 流動負債調整係数
     */
    public AnalysisCoefficient(final BigDecimal operatingProfitWeight, final BigDecimal currentLiabilitiesRatio) {
        this(operatingProfitWeight, currentLiabilitiesRatio, null);
    }
}
