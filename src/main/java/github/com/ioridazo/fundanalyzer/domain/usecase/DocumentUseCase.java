package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

public interface DocumentUseCase {

    /**
     * EDINETリストをデータベース登録
     * ↓
     * スクレイピング
     *
     * @param inputData 提出日
     * @return Void
     */
    @Async
    @NewSpan
    CompletableFuture<Void> allProcess(DateInputData inputData);

    /**
     * EDINETに書類有無を問い合わせ
     * ↓
     * 書類をデータベース登録
     *
     * @param inputData 提出日
     */
    @NewSpan
    void saveEdinetList(DateInputData inputData);

    /**
     * スクレイピング
     *
     * @param inputData 提出日
     */
    @NewSpan
    void scrape(DateInputData inputData);

    /**
     * スクレイピング
     *
     * @param inputData 書類ID
     */
    @NewSpan
    void scrape(IdInputData inputData);

    /**
     * 処理対象外に更新
     *
     * @param inputData 書類ID
     */
    @NewSpan
    void removeDocument(IdInputData inputData);
}
