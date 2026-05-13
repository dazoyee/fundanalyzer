package github.com.ioridazo.fundanalyzer.web.view.model.edinet;

import org.springframework.data.domain.Pageable;

/**
 * EDINET 一覧テーブルへの問い合わせ条件。
 *
 * @param target   表示対象。null（メイン）/ "all"
 * @param keyword  提出日（ISO_LOCAL_DATE 文字列）partial match キーワード
 * @param pageable Spring Data の Pageable
 */
public record EdinetListTableQuery(String target, String keyword, Pageable pageable) {
}
