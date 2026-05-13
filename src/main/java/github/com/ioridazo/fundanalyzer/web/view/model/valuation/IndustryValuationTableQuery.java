package github.com.ioridazo.fundanalyzer.web.view.model.valuation;

import org.springframework.data.domain.Pageable;

/**
 * 業種別評価テーブルへの問い合わせ条件。
 *
 * @param keyword  業種名の partial match キーワード
 * @param pageable Spring Data の Pageable
 */
public record IndustryValuationTableQuery(String keyword, Pageable pageable) {
}
