package github.com.ioridazo.fundanalyzer.domain.value;

public record IndicatorBackfillResult(
        int successCount,
        int skippedCount,
        int failedCount) {
}
