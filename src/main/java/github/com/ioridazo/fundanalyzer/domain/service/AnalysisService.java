package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.usecase.AnalyzeUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.NoticeUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.StockUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ValuationUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewEdinetUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Result;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.FinancialStatementInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    private final CompanyUseCase companyUseCase;
    private final DocumentUseCase documentUseCase;
    private final AnalyzeUseCase analyzeUseCase;
    private final StockUseCase stockUseCase;
    private final ValuationUseCase valuationUseCase;
    private final ViewCorporateUseCase viewCorporateUseCase;
    private final ViewEdinetUseCase viewEdinetUseCase;
    private final NoticeUseCase noticeUseCase;

    public AnalysisService(
            final CompanyUseCase companyUseCase,
            final DocumentUseCase documentUseCase,
            final AnalyzeUseCase analyzeUseCase,
            final StockUseCase stockUseCase,
            final ValuationUseCase valuationUseCase,
            final ViewCorporateUseCase viewCorporateUseCase,
            final ViewEdinetUseCase viewEdinetUseCase,
            final NoticeUseCase noticeUseCase) {
        this.companyUseCase = companyUseCase;
        this.documentUseCase = documentUseCase;
        this.analyzeUseCase = analyzeUseCase;
        this.stockUseCase = stockUseCase;
        this.valuationUseCase = valuationUseCase;
        this.viewCorporateUseCase = viewCorporateUseCase;
        this.viewEdinetUseCase = viewEdinetUseCase;
        this.noticeUseCase = noticeUseCase;
    }

    /**
     * すべてのメイン分析処理
     *
     * @param inputData 複数の提出日
     */
    @NewSpan
    @Async
    public void executeAllMain(final BetweenDateInputData inputData) {
        inputData.getFromDate()
                .datesUntil(inputData.getToDate().plusDays(1))
                .map(DateInputData::of)
                .forEach(date -> {
                    // scraping
                    documentUseCase.allProcess(date);
                    // remove
                    documentUseCase.removeDocument(date);
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
     * 一部のメイン分析処理
     *
     * @param inputData 複数の提出日
     */
    @NewSpan
    @Async
    public void executePartOfMain(final BetweenDateInputData inputData) {
        inputData.getFromDate()
                .datesUntil(inputData.getToDate().plusDays(1))
                .map(DateInputData::of)
                .forEach(date -> {
                    // scraping
                    documentUseCase.allProcess(date);
                    // remove
                    documentUseCase.removeDocument(date);
                    // analysis
                    analyzeUseCase.analyze(date);
                    // view corporate
                    viewCorporateUseCase.updateView(date);
                    // view edinet
                    viewEdinetUseCase.updateView(date);
                });
    }

    /**
     * 指定提出日をスクレイピング/分析
     *
     * @param inputData 提出日
     */
    @NewSpan
    public void executeByDate(final DateInputData inputData) {
        // scraping
        documentUseCase.scrape(inputData);
        // analysis
        analyzeUseCase.analyze(inputData);
    }

    /**
     * 指定書類IDをスクレイピング/分析
     *
     * @param inputData 書類ID
     */
    @NewSpan
    public void executeById(final IdInputData inputData) {
        // scraping
        documentUseCase.scrape(inputData);
        // analysis
        analyzeUseCase.analyze(inputData);
    }

    /**
     * 財務諸表の値の登録
     *
     * @param inputData 財務諸表の登録情報
     * @return 処理結果
     */
    @NewSpan
    public Result registerFinancialStatementValue(final FinancialStatementInputData inputData) {
        // register
        return documentUseCase.registerFinancialStatementValue(inputData);
    }

    /**
     * 指定書類IDを分析
     *
     * @param inputData 提出日
     */
    @NewSpan
    public void analyzeByDate(final DateInputData inputData) {
        // recovery
        documentUseCase.updateDocumentPeriodIfNotExist(inputData);
        // analyze
        analyzeUseCase.analyze(inputData);
        // view edinet
        viewEdinetUseCase.updateView(inputData);
    }

    /**
     * 指定提出日を分析
     *
     * @param inputData 書類ID
     */
    @NewSpan
    public void analyzeById(final IdInputData inputData) {
        // analyze
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
        stockUseCase.importStockPrice(inputData, StockUseCase.Place.NIKKEI);
        stockUseCase.importStockPrice(inputData, StockUseCase.Place.KABUOJI3);
        stockUseCase.importStockPrice(inputData, StockUseCase.Place.MINKABU);
        stockUseCase.importStockPrice(inputData, StockUseCase.Place.YAHOO_FINANCE);
    }

    /**
     * 過去の株価削除
     */
    @NewSpan
    public int deleteStock() {
        // delete stock
        return stockUseCase.deleteStockPrice();
    }

    /**
     * お気に入り企業の登録
     *
     * @param inputData 企業コード
     * @return お気に入りかどうか
     */
    @NewSpan
    public boolean updateFavoriteCompany(final CodeInputData inputData) {
        // update favorite company
        return companyUseCase.updateFavoriteCompany(inputData);
    }

    /**
     * 株価の評価
     */
    @NewSpan
    public int evaluate() {
        // evaluate
        return valuationUseCase.evaluate();
    }

    @NewSpan
    public boolean evaluate(final CodeInputData inputData) {
        // evaluate
        return valuationUseCase.evaluate(inputData);
    }
}
