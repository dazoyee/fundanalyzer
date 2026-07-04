package github.com.ioridazo.fundanalyzer.domain.value;

import java.util.Arrays;

/**
 * バックテスト集計の評価期間。
 */
public enum Horizon {

    M3(90, "3ヶ月"),
    M6(180, "6ヶ月"),
    M12(365, "12ヶ月"),
    ;

    private final int days;

    private final String label;

    Horizon(final int days, final String label) {
        this.days = days;
        this.label = label;
    }

    /**
     * 期間日数。
     *
     * @return 期間日数
     */
    public int getDays() {
        return days;
    }

    /**
     * 表示ラベル。
     *
     * @return 表示ラベル
     */
    public String getLabel() {
        return label;
    }

    /**
     * 表示ラベルから期間を取得する。
     *
     * @param label 表示ラベル
     * @return 評価期間
     */
    public static Horizon fromLabel(final String label) {
        return Arrays.stream(values())
                .filter(value -> value.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(label)));
    }
}
