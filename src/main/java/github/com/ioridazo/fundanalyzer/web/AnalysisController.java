package github.com.ioridazo.fundanalyzer.web;

import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import github.com.ioridazo.fundanalyzer.domain.service.StockService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class AnalysisController {

    private static final String INDEX = "index";
    private static final String REDIRECT_INDEX = "redirect:/fundanalyzer/v1/index";
    private static final String EDINET = "edinet";
    private static final String REDIRECT_EDINET = "redirect:/fundanalyzer/v1/edinet/list";
    private static final String CORPORATE = "corporate";
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

    @GetMapping("fundanalyzer/v1/index")
    public String index(@RequestParam(name = "message", required = false) final String message, final Model model) {
        model.addAttribute("message", message);
        model.addAttribute("companies", viewService.corporateView());
        return INDEX;
    }

    @GetMapping("fundanalyzer/v1/edinet/list")
    public String edinetList(@RequestParam(name = "message", required = false) final String message, final Model model) {
        model.addAttribute("message", message);
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetListview());
        return EDINET;
    }

    @GetMapping("fundanalyzer/v1/corporate/{code}")
    public String brandDetail(@PathVariable final String code, final Model model) {
        final var brandDetail = viewService.brandDetailView(code + "0");
        model.addAttribute("corporate", brandDetail.getCorporate());
        model.addAttribute("corporateView", brandDetail.getCorporateView());
        model.addAttribute("analysisResults", brandDetail.getAnalysisResultList());
        model.addAttribute("financialStatements", brandDetail.getFinancialStatement());
        model.addAttribute("forecastStocks", brandDetail.getMinkabuList());
        model.addAttribute("stockPrices", brandDetail.getStockPriceList());
        return CORPORATE;
    }

    @GetMapping("fundanalyzer/v1/company")
    public String company(final Model model) {
        documentService.company();

        model.addAttribute("companies", viewService.corporateView());
        return INDEX;
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
        LocalDate.parse(fromDate)
                .datesUntil(LocalDate.parse(toDate).plusDays(1))
                .forEach(date -> {
                    // execute実行
                    documentService.execute(date.toString(), "120")
                            // execute完了後、analyze実行
                            .thenAcceptAsync(unused -> analysisService.analyze(date))
                            // analyze完了後、importStockPrice実行
                            .thenAcceptAsync(unused -> stockService.importStockPrice(date))
                            // importStockPrice完了後、notice実行
                            .thenAcceptAsync(unused -> viewService.notice(date));
                });
        return REDIRECT_INDEX;
    }

    /**
     * 表示をアップデートする
     *
     * @return Index
     */
    @PostMapping("fundanalyzer/v1/update/view")
    public String updateView() {
        viewService.updateCorporateView();
        viewService.updateEdinetListView("120");
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
        documentService.scrape(LocalDate.parse(date));
        analysisService.analyze(LocalDate.parse(date));
        return REDIRECT_INDEX;
    }

    /**
     * 指定書類IDを分析する
     *
     * @param documentId 書類ID
     * @return Index
     */
    @PostMapping("fundanalyzer/v1/scrape/id")
    public String scrapeById(final String documentId) {
        documentService.scrape(documentId);
        analysisService.analyze(documentId);
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
        LocalDate.parse(fromDate)
                .datesUntil(LocalDate.parse(toDate).plusDays(1))
                .forEach(stockService::importStockPrice);
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
        stockService.importStockPrice(code);
        return REDIRECT_CORPORATE + "/" + code.substring(0, 4);
    }

    /**
     * 割安比率でソートする
     *
     * @return Index
     */
    @GetMapping("fundanalyzer/v1/index/sort/discount-rate")
    public String sortedDiscountRate(final Model model) {
        model.addAttribute("companies", viewService.sortByDiscountRate());
        return INDEX;
    }

    /**
     * すべての企業情報を表示する
     *
     * @return Index
     */
    @GetMapping("fundanalyzer/v1/index/all")
    public String indexAll(final Model model) {
        model.addAttribute("companies", viewService.corporateViewAll());
        return INDEX;
    }

    /**
     * EDINETから提出書類一覧を取得する
     *
     * @param fromDate 提出日
     * @param toDate   提出日
     * @return EdinetList
     */
    @PostMapping("fundanalyzer/v1/edinet/list")
    public String edinet(final String fromDate, final String toDate) {
        documentService.edinetList(fromDate, toDate);
        return REDIRECT_EDINET;
    }

    /**
     * すべてのEDINETリストを表示する
     *
     * @param model model
     * @return EdinetList
     */
    @GetMapping("fundanalyzer/v1/edinet/list/all")
    public String edinetListAll(final Model model) {
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetListViewAll());
        return EDINET;
    }

    /**
     * 処理ステータスが未完のものを初期化する
     *
     * @param model model
     * @return EdinetList
     */
    @PostMapping("fundanalyzer/v1/reset/status")
    public String resetStatus(final Model model) {
        documentService.resetForRetry();
        return REDIRECT_EDINET + "?message=updated";
    }

    // -------------------------------------------------------

    @GetMapping("/edinet/list")
    public String devEdinetList(final Model model) {
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetListview());
        return EDINET;
    }

    @GetMapping("/company")
    public String devCompany(final Model model) {
        documentService.company();

        model.addAttribute("companies", viewService.corporateView());
        return INDEX;
    }

    @GetMapping("/scrape/{date}")
    public String devDocument(@PathVariable String date, final Model model) {
        documentService.company();
        documentService.execute(date, "120")
                .thenRunAsync(viewService::updateCorporateView);

        model.addAttribute("companies", viewService.corporateView());
        return INDEX;
    }

    @GetMapping("/reset/status")
    public String devResetStatus(final Model model) {
        documentService.resetForRetry();

        model.addAttribute("companies", viewService.corporateView());
        return INDEX;
    }

    @GetMapping("/view/company/{year}")
    public String viewCompany(@PathVariable String year, final Model model) {
        documentService.company();
        documentService.execute("2020-05-22", "120")
                .thenRunAsync(viewService::updateCorporateView);

        model.addAttribute("companies", viewService.corporateView());
        return INDEX;
    }

    @GetMapping("/scrape/analysis/{date}")
    public String scrapeAndAnalyze(@PathVariable String date, final Model model) {
        documentService.company();

        // execute実行
        documentService.execute(date, "120")
                // execute完了後、analyze実行
                .thenRunAsync(() -> analysisService.analyze(LocalDate.parse(date)))
                // analyze完了後、importStockPrice実行
                .thenRunAsync(() -> stockService.importStockPrice(LocalDate.parse(date)))
                // importStockPrice完了後、notice実行
                .thenRunAsync(() -> viewService.notice(LocalDate.parse(date)))
                // notice完了後、update実行
                .thenRunAsync(() -> {
                    viewService.updateCorporateView();
                    viewService.updateEdinetListView("120");
                });

        model.addAttribute("companies", viewService.corporateView());
        return REDIRECT_INDEX + "?message=updating";
    }
}
