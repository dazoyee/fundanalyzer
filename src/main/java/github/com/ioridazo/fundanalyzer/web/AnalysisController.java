package github.com.ioridazo.fundanalyzer.web;

import github.com.ioridazo.fundanalyzer.domain.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.DocumentService;
import github.com.ioridazo.fundanalyzer.domain.StockService;
import github.com.ioridazo.fundanalyzer.domain.ViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;

@Controller
public class AnalysisController {

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
    public String index(final Model model) {
        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }

    @GetMapping("fundanalyzer/v1/edinet/list")
    public String edinetList(final Model model) {
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetList("120"));
        return "edinet";
    }

    @GetMapping("fundanalyzer/v1/company")
    public String company(final Model model) {
        documentService.company();

        model.addAttribute("companies", viewService.viewCompanyAll());
        return "index";
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
                    documentService.document(date.toString(), "120");
                    analysisService.analyze(date);
                    stockService.importStockPrice(date);
                });
        return "redirect:/fundanalyzer/v1/index";
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
        return "redirect:/fundanalyzer/v1/index";
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
        return "redirect:/fundanalyzer/v1/index";
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
        return "redirect:/fundanalyzer/v1/index";
    }

    /**
     * 割安比率でソートする
     *
     * @return Index
     */
    @GetMapping("fundanalyzer/v1/index/sort/discount-rate")
    public String sortedDiscountRate(final Model model) {
        model.addAttribute("companies", viewService.sortedCompanyByDiscountRate());
        return "index";
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
        return "redirect:/fundanalyzer/v1/edinet/list";
    }

    /**
     * すべてのリストを参照する
     *
     * @param model model
     * @return EdinetList
     */
    @PostMapping("fundanalyzer/v1/edinet/list/all")
    public String edinetListAll(final Model model) {
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetListAll("120"));
        return "edinet";
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

        model.addAttribute("message", "更新しました");
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetList("120"));
        return "edinet";
    }

    // -------------------------------------------------------

    @GetMapping("/edinet/list")
    public String devEdinetList(final Model model) {
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetList("120"));
        return "edinet";
    }

    @GetMapping("/company")
    public String devCompany(final Model model) {
        documentService.company();

        model.addAttribute("companies", viewService.viewCompanyAll());
        return "index";
    }

    @GetMapping("/edinet/list/{date}")
    public String documentList(@PathVariable String date, final Model model) {
        documentService.company();
        documentService.edinetList(LocalDate.parse(date));

        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetList("120"));
        return "edinet";
    }

    @GetMapping("/edinet/list/{fromDate}/{toDate}")
    public String document(@PathVariable String fromDate, @PathVariable String toDate, final Model model) {
        documentService.company();
        documentService.edinetList(fromDate, toDate);

        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetList("120"));
        return "edinet";
    }

    @GetMapping("/scrape/{date}")
    public String devDocument(@PathVariable String date, final Model model) {
        documentService.company();
        documentService.document(date, "120");

        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }

    @GetMapping("/reset/status")
    public String devResetStatus(final Model model) {
        documentService.resetForRetry();

        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }

    @GetMapping("/view/company/{year}")
    public String viewCompany(@PathVariable String year, final Model model) {
        documentService.company();
        documentService.document("2020-05-22", "120");

        model.addAttribute("companies", viewService.viewCompany(Integer.parseInt(year)));
        return "index";
    }

    @GetMapping("/scrape/analysis/{date}")
    public String scrapeAndAnalyze(@PathVariable String date, final Model model) {
        documentService.company();
        documentService.document(date, "120");
        analysisService.analyze(LocalDate.parse(date));
        stockService.importStockPrice(LocalDate.parse(date));

        model.addAttribute("companies", viewService.viewCompany());
        return "index";
    }
}
