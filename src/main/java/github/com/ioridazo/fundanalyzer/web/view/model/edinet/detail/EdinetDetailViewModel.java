package github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail;

import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;

import java.util.List;

/**
 * EDINET 詳細ビュー
 *
 * @param edinetList         対象提出日の処理状況
 * @param documentDetailList 提出日に関連する未処理ドキュメントのリスト
 */
public record EdinetDetailViewModel(
        EdinetListViewModel edinetList,
        List<DocumentViewModel> documentDetailList) {

    /**
     * 静的ファクトリ
     */
    public static EdinetDetailViewModel of(
            final EdinetListViewModel edinetList,
            final List<DocumentViewModel> documentDetailList) {
        return new EdinetDetailViewModel(edinetList, documentDetailList);
    }

    public EdinetListViewModel getEdinetList() {
        return edinetList;
    }

    public List<DocumentViewModel> getDocumentDetailList() {
        return documentDetailList;
    }
}
