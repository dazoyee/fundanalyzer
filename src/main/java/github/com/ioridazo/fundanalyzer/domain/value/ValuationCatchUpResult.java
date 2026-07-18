package github.com.ioridazo.fundanalyzer.domain.value;

/**
 * valuation catch-up バッチの結果。
 *
 * @param targetCompanyCount  対象会社数
 * @param advancedCount       前進した valuation 件数
 * @param unresolvedCompanyCount 未解消会社数
 */
public record ValuationCatchUpResult(
        int targetCompanyCount,
        int advancedCount,
        int unresolvedCompanyCount) {
}
