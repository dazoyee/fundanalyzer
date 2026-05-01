package github.com.ioridazo.fundanalyzer.web.view.model.valuation;

import org.springframework.data.domain.Pageable;

/**
 * 株価評価（会社別）テーブルへの問い合わせ条件。Phase 4 で導入したテーブル汎用パターン拡張版。
 *
 * @param target   表示対象の種別。null（メイン）/ "all" / "favorite"
 * @param keyword  証券コードまたは会社名の partial match キーワード
 * @param view     表示する view 種別。"stock" / "submit" / "graham-index" / "dividend-yield"
 * @param pageable Spring Data の Pageable
 */
public record CompanyValuationTableQuery(String target, String keyword, String view, Pageable pageable) {
}
