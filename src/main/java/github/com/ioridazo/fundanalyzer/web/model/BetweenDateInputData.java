package github.com.ioridazo.fundanalyzer.web.model;

import java.time.LocalDate;

/**
 * 期間（開始日・終了日）入力データ
 *
 * @param fromDate 開始日
 * @param toDate   終了日
 */
public record BetweenDateInputData(LocalDate fromDate, LocalDate toDate) {

    /**
     * 静的ファクトリ
     *
     * @param fromDate 開始日
     * @param toDate   終了日
     * @return BetweenDateInputData
     */
    public static BetweenDateInputData of(final LocalDate fromDate, final LocalDate toDate) {
        return new BetweenDateInputData(fromDate, toDate);
    }

    /**
     * 互換アクセサ（既存呼び出し向け）
     *
     * @return 開始日
     */
    public LocalDate getFromDate() {
        return fromDate;
    }

    /**
     * 互換アクセサ（既存呼び出し向け）
     *
     * @return 終了日
     */
    public LocalDate getToDate() {
        return toDate;
    }
}
