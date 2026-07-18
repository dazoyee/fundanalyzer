package github.com.ioridazo.fundanalyzer.domain.value;

/**
 * valuation catch-up バッチの事前確認。
 *
 * @param targetCompanyCount catch-up 対象会社数
 */
public record ValuationCatchUpPreview(
        int targetCompanyCount) {
}
