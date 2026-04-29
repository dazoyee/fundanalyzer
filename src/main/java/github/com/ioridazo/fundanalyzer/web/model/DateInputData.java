package github.com.ioridazo.fundanalyzer.web.model;

import java.time.LocalDate;

/**
 * 日付入力データ
 *
 * @param date 対象日
 */
public record DateInputData(LocalDate date) {

    /**
     * 静的ファクトリ
     *
     * @param date 対象日
     * @return DateInputData
     */
    public static DateInputData of(final LocalDate date) {
        return new DateInputData(date);
    }

    /**
     * 互換アクセサ（既存呼び出し向け）
     *
     * @return 対象日
     */
    public LocalDate getDate() {
        return date;
    }
}
