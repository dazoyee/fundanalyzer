package github.com.ioridazo.fundanalyzer.domain.usecase;

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
    void importStockPrice(DateInputData inputData, Place place);

    /**
     * 株価取得
     *
     * @param inputData 企業コード
     * @param place     通信先
     */
    @NewSpan
    void importStockPrice(CodeInputData inputData, Place place);

    /**
     * 株価削除
     *
     * @return 削除カウント
     */
    @NewSpan
    int deleteStockPrice();

    enum Place {
        NIKKEI,
        KABUOJI3,
        MINKABU,
        YAHOO_FINANCE,
    }
}
