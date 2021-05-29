package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.domain.value.Document;
import org.springframework.cloud.sleuth.annotation.NewSpan;

public interface ScrapingUseCase {

    /**
     * ファイル取得
     * ↓
     * ファイル解凍
     *
     * @param document ドキュメント
     */
    @NewSpan
    void download(Document document);

    /**
     * 貸借対照表スクレイピング
     *
     * @param document ドキュメント
     */
    @NewSpan
    void bs(Document document);

    /**
     * 損益計算書スクレイピング
     *
     * @param document ドキュメント
     */
    @NewSpan
    void pl(Document document);

    /**
     * 株式総数スクレイピング
     *
     * @param document ドキュメント
     */
    @NewSpan
    void ns(Document document);
}
