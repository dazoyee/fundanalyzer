package github.com.ioridazo.fundanalyzer.web.view.model.edinet;

import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * EDINET 一覧テーブルの 1 ページ分の結果。
 *
 * @param rows          現ページに含まれる EDINET リスト
 * @param totalPages    総ページ数
 * @param totalElements 全ヒット件数（filter 適用後）
 * @param pageNumber    現在のページ番号（0 始まり）
 * @param pageSize      1 ページあたりの件数
 * @param sort          適用中のソート条件
 */
public record EdinetListTablePage(
        List<EdinetListViewModel> rows,
        int totalPages,
        long totalElements,
        int pageNumber,
        int pageSize,
        Sort sort) {
}
