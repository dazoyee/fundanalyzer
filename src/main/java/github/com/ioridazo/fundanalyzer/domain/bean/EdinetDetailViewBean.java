package github.com.ioridazo.fundanalyzer.domain.bean;

import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import lombok.Value;

import java.util.List;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class EdinetDetailViewBean {

    // 対象提出日の処理状況
    private EdinetListViewBean edinetListView;

    // 提出日に関連する未処理ドキュメントのリスト
    private List<DocumentDetail> documentDetailList;

    @Value
    public static class DocumentDetail {
        private Company company;
        private Document document;
        private ValuesForAnalysis values;
    }

    @Value
    public static class ValuesForAnalysis {
        // 流動資産合計
        private Long totalCurrentAssets;
        // 投資その他の資産合計
        private Long totalInvestmentsAndOtherAssets;
        // 流動負債合計
        private Long totalCurrentLiabilities;
        // 固定負債合計
        private Long totalFixedLiabilities;
        // 営業利益
        private Long operatingProfit;
        // 株式総数
        private Long numberOfShares;
    }
}
