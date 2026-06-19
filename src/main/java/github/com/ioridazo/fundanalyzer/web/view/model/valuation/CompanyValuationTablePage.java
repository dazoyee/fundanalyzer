package github.com.ioridazo.fundanalyzer.web.view.model.valuation;

import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * 株価評価（会社別）テーブルの 1 ページ分の結果。
 *
 * @param rows          現ページに含まれる会社別評価リスト
 * @param totalPages    総ページ数
 * @param totalElements 全ヒット件数（filter 適用後）
 * @param pageNumber    現在のページ番号（0 始まり）
 * @param pageSize      1 ページあたりの件数
 * @param sort          適用中のソート条件
 * @param view          適用中の view 種別
 */
public record CompanyValuationTablePage(
        List<CompanyValuationViewModel> rows,
        int totalPages,
        long totalElements,
        int pageNumber,
        int pageSize,
        Sort sort,
        String view) {
}
