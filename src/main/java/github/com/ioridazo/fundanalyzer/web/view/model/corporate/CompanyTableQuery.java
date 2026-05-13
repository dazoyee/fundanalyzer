package github.com.ioridazo.fundanalyzer.web.view.model.corporate;

import org.springframework.data.domain.Pageable;

/**
 * 会社一覧テーブルへの問い合わせ条件。Phase 4 以降の各画面でも同じパターンで XxxTableQuery を定義する。
 *
 * @param target   表示対象の種別。null（メイン）/ "quart" / "all" / "favorite"
 * @param keyword  証券コードまたは会社名の partial match キーワード。null または空文字なら絞り込みなし
 * @param pageable Spring Data の Pageable（ページ番号・ページサイズ・ソート条件）
 */
public record CompanyTableQuery(String target, String keyword, Pageable pageable) {
}
