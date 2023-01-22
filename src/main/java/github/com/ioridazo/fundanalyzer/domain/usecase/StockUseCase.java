package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SourceOfStockPrice;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.springframework.cloud.sleuth.annotation.NewSpan;

public interface StockUseCase {

    /**
     * 株価取得
     *
     * @param inputData 提出日
     * @param place     通信先
     */
    @NewSpan
    void importStockPrice(DateInputData inputData, SourceOfStockPrice place);

    /**
     * 株価取得
     *
     * @param inputData 企業コード
     * @param place     通信先
     */
    @NewSpan
    void importStockPrice(CodeInputData inputData, SourceOfStockPrice place);

    /**
     * 株価削除
     *
     * @return 削除カウント
     */
    @NewSpan
    int deleteStockPrice();
}
