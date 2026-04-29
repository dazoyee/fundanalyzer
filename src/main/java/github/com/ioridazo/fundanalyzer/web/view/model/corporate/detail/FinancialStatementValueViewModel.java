package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

/**
 * 財務諸表の科目と金額のビュー
 *
 * @param subject 科目名
 * @param value   金額
 */
public record FinancialStatementValueViewModel(String subject, Long value) {

    /**
     * 静的ファクトリ
     *
     * @param subject 科目名
     * @param value   金額
     * @return FinancialStatementValueViewModel
     */
    public static FinancialStatementValueViewModel of(final String subject, final Long value) {
        return new FinancialStatementValueViewModel(subject, value);
    }

    /**
     * 互換アクセサ（既存呼び出し向け）
     *
     * @return 科目名
     */
    public String getSubject() {
        return subject;
    }

    /**
     * 互換アクセサ（既存呼び出し向け）
     *
     * @return 金額
     */
    public Long getValue() {
        return value;
    }
}
