package github.com.ioridazo.fundanalyzer.web.view.model.corporate;

import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * 会社一覧テーブルの 1 ページ分の結果。Phase 4 以降の各画面でも同じパターンで XxxTablePage を定義する。
 *
 * @param companies     現ページに含まれる会社リスト（既に filter / sort / page 済み）
 * @param totalPages    総ページ数
 * @param totalElements 全ヒット件数（filter 適用後）
 * @param pageNumber    現在のページ番号（0 始まり）
 * @param pageSize      1 ページあたりの件数
 * @param sort          適用中のソート条件
 */
public record CompanyTablePage(
        List<CorporateViewModel> companies,
        int totalPages,
        long totalElements,
        int pageNumber,
        int pageSize,
        Sort sort) {
}
