package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.entity.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import github.com.ioridazo.fundanalyzer.domain.service.StockService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.util.Target;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
public class AnalysisController {

    private static final String REDIRECT_INDEX = "redirect:/fundanalyzer/v1/index";
    private static final String REDIRECT_CORPORATE = "redirect:/fundanalyzer/v1/corporate";

    private final DocumentService documentService;
    private final AnalysisService analysisService;
    private final StockService stockService;
    private final ViewService viewService;

    public AnalysisController(
            final DocumentService documentService,
            final AnalysisService analysisService,
            final StockService stockService,
            final ViewService viewService) {
        this.documentService = documentService;
        this.analysisService = analysisService;
        this.stockService = stockService;
        this.viewService = viewService;
    }

    /**
     * 指定提出日の書類をメインの一連処理をする
     *
     * @param fromDate 提出日
     * @param toDate   提出日
     * @return Index
     */
    @PostMapping("fundanalyzer/v1/document/analysis")
    public String documentAnalysis(final String fromDate, final String toDate) {
        FundanalyzerLogClient.logProcessStart(Category.DOCUMENT, Process.ANALYSIS);

        final List<DocumentTypeCode> targetTypes = Target.annualSecuritiesReport();

        LocalDate.parse(fromDate)
                .datesUntil(LocalDate.parse(toDate).plusDays(1))
                .forEach(date -> {
                    // execute実行
                    documentService.execute(date.toString(), targetTypes)
                            // execute完了後、analyze実行
                            .thenAcceptAsync(unused -> analysisService.analyze(date, targetTypes))
                            // analyze完了後、importStockPrice実行
                            .thenAcceptAsync(unused -> stockService.importStockPrice(date, targetTypes))
                            // importStockPrice完了後、updateCorporateView実行
                            .thenAcceptAsync(unused -> viewService.updateCorporateView(date, targetTypes))
                            // updateCorporateView完了後、updateEdinetListView実行
                            .thenAcceptAsync(unused -> viewService.updateEdinetListView(date, targetTypes))
                            // updateEdinetListView完了後、notice実行
                            .thenAcceptAsync(unused -> viewService.notice(date, targetTypes));
                });
        FundanalyzerLogClient.logProcessEnd(Category.DOCUMENT, Process.ANALYSIS);
        return REDIRECT_INDEX;
    }

    /**
     * 表示をアップデートする
     *
     * @return Index
     */
    @PostMapping("fundanalyzer/v1/update/view")
    public String updateView() {
        FundanalyzerLogClient.logProcessStart(Category.VIEW, Process.UPDATE);
        viewService.updateCorporateView(Target.annualSecuritiesReport());
        viewService.updateEdinetListView(Target.annualSecuritiesReport());
        FundanalyzerLogClient.logProcessEnd(Category.VIEW, Process.UPDATE);
        return REDIRECT_INDEX + "?message=updating";
    }

    /**
     * 指定提出日の書類を分析する
     *
     * @param date 提出日
     * @return Index
     */
    @PostMapping("fundanalyzer/v1/scrape/date")
    public String scrapeByDate(final String date) {
        FundanalyzerLogClient.logProcessStart(Category.DOCUMENT, Process.ANALYSIS);
        documentService.scrape(LocalDate.parse(date), Target.annualSecuritiesReport());
        analysisService.analyze(LocalDate.parse(date), Target.annualSecuritiesReport());
        FundanalyzerLogClient.logProcessEnd(Category.DOCUMENT, Process.ANALYSIS);
        return REDIRECT_INDEX;
    }

    /**
     * 指定書類IDを分析する
     *
     * @param documentId 書類ID（CSVで複数可能）
     * @return Index
     */
    @PostMapping("fundanalyzer/v1/scrape/id")
    public String scrapeById(final String documentId) {
        FundanalyzerLogClient.logProcessStart(Category.DOCUMENT, Process.ANALYSIS);
        Arrays.stream(documentId.split(","))
                .filter(dId -> dId.length() == 8)
                .forEach(dId -> {
                    documentService.scrape(dId);
                    analysisService.analyze(dId);
                });
        FundanalyzerLogClient.logProcessEnd(Category.DOCUMENT, Process.ANALYSIS);
        return REDIRECT_INDEX;
    }

    /**
     * 指定日に提出した企業の株価を取得する
     *
     * @param fromDate 提出日
     * @param toDate   提出日
     * @return Index
     */
    @PostMapping("fundanalyzer/v1/import/stock/date")
    public String importStocks(final String fromDate, final String toDate) {
        FundanalyzerLogClient.logProcessStart(Category.STOCK, Process.IMPORT);
        LocalDate.parse(fromDate)
                .datesUntil(LocalDate.parse(toDate).plusDays(1))
                .forEach(submitDate -> stockService.importStockPrice(submitDate, Target.annualSecuritiesReport()));
        FundanalyzerLogClient.logProcessEnd(Category.STOCK, Process.IMPORT);
        return REDIRECT_INDEX;
    }

    /**
     * 企業の株価を取得する
     *
     * @param code 会社コード
     * @return BrandDetail
     */
    @PostMapping("fundanalyzer/v1/import/stock/code")
    public String importStocks(final String code) {
        FundanalyzerLogClient.logProcessStart(Category.STOCK, Process.IMPORT);
        stockService.importStockPrice(code);
        FundanalyzerLogClient.logProcessEnd(Category.STOCK, Process.IMPORT);
        return REDIRECT_CORPORATE + "/" + code.substring(0, 4);
    }
}
