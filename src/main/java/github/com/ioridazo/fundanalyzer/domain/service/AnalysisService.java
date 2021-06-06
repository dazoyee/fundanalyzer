package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.usecase.AnalyzeUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.NoticeUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.StockUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewEdinetUseCase;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    private final DocumentUseCase documentUseCase;
    private final AnalyzeUseCase analyzeUseCase;
    private final StockUseCase stockUseCase;
    private final ViewCorporateUseCase viewCorporateUseCase;
    private final ViewEdinetUseCase viewEdinetUseCase;
    private final NoticeUseCase noticeUseCase;

    public AnalysisService(
            final DocumentUseCase documentUseCase,
            final AnalyzeUseCase analyzeUseCase,
            final StockUseCase stockUseCase,
            final ViewCorporateUseCase viewCorporateUseCase,
            final ViewEdinetUseCase viewEdinetUseCase,
            final NoticeUseCase noticeUseCase) {
        this.documentUseCase = documentUseCase;
        this.analyzeUseCase = analyzeUseCase;
        this.stockUseCase = stockUseCase;
        this.viewCorporateUseCase = viewCorporateUseCase;
        this.viewEdinetUseCase = viewEdinetUseCase;
        this.noticeUseCase = noticeUseCase;
    }

    /**
     * メイン分析処理
     *
     * @param inputData 複数の提出日
     */
    @NewSpan
    @Async
    public void doMain(final BetweenDateInputData inputData) {
        inputData.getFromDate()
                .datesUntil(inputData.getToDate().plusDays(1))
                .map(DateInputData::of)
                .forEach(date -> {
                    // scraping
                    documentUseCase.allProcess(date);
                    // analysis
                    analyzeUseCase.analyze(date);
                    // stock
                    stockUseCase.importStockPrice(date);
                    // view corporate
                    viewCorporateUseCase.updateView(date);
                    // view edinet
                    viewEdinetUseCase.updateView(date);
                    // slack
                    noticeUseCase.noticeSlack(date);
                });
    }

    /**
     * 指定提出日の書類分析
     *
     * @param inputData 提出日
     */
    @NewSpan
    public void doByDate(final DateInputData inputData) {
        // scraping
        documentUseCase.scrape(inputData);
        // analysis
        analyzeUseCase.analyze(inputData);
    }

    /**
     * 指定書類IDを分析
     *
     * @param inputData 書類ID
     */
    @NewSpan
    public void doById(final IdInputData inputData) {
        // scraping
        documentUseCase.scrape(inputData);
        // analysis
        analyzeUseCase.analyze(inputData);
    }

    /**
     * 指定提出日の株価取得
     *
     * @param inputData 複数の提出日
     */
    @NewSpan
    public void importStock(final BetweenDateInputData inputData) {
        inputData.getFromDate()
                .datesUntil(inputData.getToDate().plusDays(1))
                .map(DateInputData::of)
                // stock
                .forEach(stockUseCase::importStockPrice);
    }

    /**
     * 提出日の株価取得
     *
     * @param inputData 提出日
     */
    @NewSpan
    public void importStock(final DateInputData inputData) {
        // stock
        stockUseCase.importStockPrice(inputData);
    }

    /**
     * 企業コードの株価取得
     *
     * @param inputData 企業コード
     */
    @NewSpan
    public void importStock(final CodeInputData inputData) {
        // stock
        stockUseCase.importStockPrice(inputData);
    }

    /**
     * 過去の株価削除
     */
    @NewSpan
    public int deleteStock() {
        // delete stock
        return stockUseCase.deleteStockPrice();
    }
}
