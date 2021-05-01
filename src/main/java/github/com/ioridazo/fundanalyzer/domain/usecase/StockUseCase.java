package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

public interface StockUseCase {

    /**
     * 株価取得
     *
     * @param inputData 提出日
     * @return Void
     */
    @Async
    @NewSpan
    CompletableFuture<Void> importStockPrice(DateInputData inputData);

    /**
     * 株価取得
     *
     * @param inputData 企業コード
     */
    @NewSpan
    void importStockPrice(CodeInputData inputData);
}
