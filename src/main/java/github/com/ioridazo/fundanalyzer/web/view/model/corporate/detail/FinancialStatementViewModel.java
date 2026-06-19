package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import java.time.LocalDate;
import java.util.List;

/**
 * 財務諸表ビュー
 *
 * @param submitDate 提出日
 * @param key        財務諸表のキー
 * @param bs         貸借対照表の値リスト
 * @param pl         損益計算書の値リスト
 */
public record FinancialStatementViewModel(
        LocalDate submitDate,
        FinancialStatementKeyViewModel key,
        List<FinancialStatementValueViewModel> bs,
        List<FinancialStatementValueViewModel> pl) {

    /**
     * 静的ファクトリ
     *
     * @param submitDate 提出日
     * @param key        キー
     * @param bs         BS の値リスト
     * @param pl         PL の値リスト
     * @return FinancialStatementViewModel
     */
    public static FinancialStatementViewModel of(
            final LocalDate submitDate,
            final FinancialStatementKeyViewModel key,
            final List<FinancialStatementValueViewModel> bs,
            final List<FinancialStatementValueViewModel> pl) {
        return new FinancialStatementViewModel(submitDate, key, bs, pl);
    }

    /**
     * 互換アクセサ（既存呼び出し向け）
     *
     * @return 提出日
     */
    public LocalDate getSubmitDate() {
        return submitDate;
    }

    /**
     * 互換アクセサ（既存呼び出し向け）
     *
     * @return キー
     */
    public FinancialStatementKeyViewModel getKey() {
        return key;
    }

    /**
     * 互換アクセサ（既存呼び出し向け）
     *
     * @return BS の値リスト
     */
    public List<FinancialStatementValueViewModel> getBs() {
        return bs;
    }

    /**
     * 互換アクセサ（既存呼び出し向け）
     *
     * @return PL の値リスト
     */
    public List<FinancialStatementValueViewModel> getPl() {
        return pl;
    }
}
