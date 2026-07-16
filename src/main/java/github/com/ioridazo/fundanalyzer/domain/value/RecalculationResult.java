package github.com.ioridazo.fundanalyzer.domain.value;

/**
 * 係数一括再計算バッチの結果。
 *
 * @param targetCount           対象件数（analysis_result の走査件数）
 * @param updatedCount          企業価値または RIM 理論株価が変わり更新した件数
 * @param skippedCount          値に変化がなく更新をスキップした件数
 * @param failedCount           入力欠損等により算出できず対象外とした件数
 * @param valuationUpdatedCount valuation の割引値・割引率を一括更新した件数
 */
public record RecalculationResult(
        int targetCount,
        int updatedCount,
        int skippedCount,
        int failedCount,
        int valuationUpdatedCount) {
}
