package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SourceOfStockPrice;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.usecase.AnalyzeUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.StockUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewEdinetUseCase;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalTime;

@Profile("!prod")
@Controller
public class DevelopController {

    private final ViewService viewService;
    private final CompanyUseCase companyUseCase;
    private final DocumentUseCase documentUseCase;
    private final AnalyzeUseCase analyzeUseCase;
    private final StockUseCase stockUseCase;
    private final ViewCorporateUseCase viewCorporateUseCase;
    private final ViewEdinetUseCase viewEdinetUseCase;

    public DevelopController(
            final ViewService viewService,
            final CompanyUseCase companyUseCase,
            final DocumentUseCase documentUseCase,
            final AnalyzeUseCase analyzeUseCase,
            final StockUseCase stockUseCase,
            final ViewCorporateUseCase viewCorporateUseCase,
            final ViewEdinetUseCase viewEdinetUseCase) {
        this.documentUseCase = documentUseCase;
        this.analyzeUseCase = analyzeUseCase;
        this.viewEdinetUseCase = viewEdinetUseCase;
        this.viewService = viewService;
        this.companyUseCase = companyUseCase;
        this.stockUseCase = stockUseCase;
        this.viewCorporateUseCase = viewCorporateUseCase;
    }

    @GetMapping("/edinet-list")
    public String devEdinetList(final Model model) {
        model.addAttribute("companyUpdated", companyUseCase.getUpdateDate());
        model.addAttribute("edinetList", viewService.getEdinetListView());
        return "edinet";
    }

    @GetMapping("/company")
    public String devCompany(final Model model) {
        // company
        companyUseCase.saveCompanyInfo();

        model.addAttribute("companies", viewService.getCorporateView());
        return "index";
    }

    @GetMapping("/scrape/analysis/{date}")
    public String devDoMain(@PathVariable final String date, final Model model) {
        // company
        companyUseCase.saveCompanyInfo();

        final DateInputData inputData = DateInputData.of(LocalDate.parse(date));
        // scraping
        documentUseCase.allProcess(inputData);
        // remove
        documentUseCase.removeDocument(inputData);
        // stock
        stockUseCase.importStockPrice(inputData, SourceOfStockPrice.MINKABU);
        stockUseCase.importStockPrice(inputData, SourceOfStockPrice.NIKKEI);
        // analysis
        analyzeUseCase.analyze(inputData);
        // view corporate
        viewCorporateUseCase.updateView(inputData);
        // view edinet
        viewEdinetUseCase.updateView(inputData);

        model.addAttribute("companies", viewService.getCorporateView());
        return "redirect:" + UriComponentsBuilder.fromUriString("/v2/index").toUriString();
    }

    /**
     * 画面刷新タスク Phase 1 POC エンドポイント。Phase 2 開始時に削除する。
     *
     * @return Tailwind / htmx / Alpine.js / Lucide / Litepicker / Chart.js / ダークモード /
     *         レスポンシブ動作確認用のテンプレート名
     */
    @GetMapping("/v2/__phase1-poc")
    public String phase1Poc() {
        return "__phase1-poc";
    }

    /**
     * 画面刷新タスク Phase 1 POC の htmx 部分更新フラグメント。Phase 2 開始時に削除する。
     *
     * @return 現在時刻を埋め込んだ HTML スニペット
     */
    @GetMapping("/v2/__phase1-poc/fragment")
    @ResponseBody
    public String phase1PocFragment() {
        return "<span class=\"font-bold text-emerald-600 dark:text-emerald-400\">"
                + "htmx fragment loaded at " + LocalTime.now() + "</span>";
    }
}
