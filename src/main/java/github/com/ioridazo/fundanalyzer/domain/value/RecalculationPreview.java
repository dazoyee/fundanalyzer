package github.com.ioridazo.fundanalyzer.domain.value;

/**
 * 係数一括再計算バッチの事前確認（対象件数のみ。値の変化有無はバッチ実行まで確定しない）。
 *
 * @param analysisResultCount analysis_result の全件数
 * @param valuationCount      valuation の全件数
 */
public record RecalculationPreview(
        int analysisResultCount,
        int valuationCount) {
}
