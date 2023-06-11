package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.EdinetService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.value.Result;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.FinancialStatementInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@Controller
public class EdinetController {

    private static final String REDIRECT = "redirect:";
    private static final URI V2_EDINET_PATH = URI.create("/v2/edinet-list");
    private static final URI V2_EDINET_DETAIL_PATH = URI.create("/v2/edinet-list-detail");

    private final AnalysisService analysisService;
    private final EdinetService edinetService;
    private final ViewService viewService;

    public EdinetController(
            final AnalysisService analysisService,
            final EdinetService edinetService,
            final ViewService viewService) {
        this.analysisService = analysisService;
        this.edinetService = edinetService;
        this.viewService = viewService;
    }

    /**
     * 会社情報を更新する
     *
     * @param redirectAttributes redirectAttributes
     * @return EdinetList
     */
    @GetMapping("/v1/company")
    public String updateCompany(final RedirectAttributes redirectAttributes) {
        edinetService.updateCompany();
        redirectAttributes.addFlashAttribute("message", "会社情報アップデート処理を要求しました。しばらく経ってから再度アクセスしてください。");
        return REDIRECT + UriComponentsBuilder.fromUri(V2_EDINET_PATH).toUriString();
    }

    /**
     * 処理状況を更新する
     *
     * @param redirectAttributes redirectAttributes
     * @return EdinetList
     */
    @GetMapping("/v1/update/edinet/view")
    public String updateEdinetView(final RedirectAttributes redirectAttributes) {
        viewService.updateEdinetView();
        redirectAttributes.addFlashAttribute("message", "処理状況アップデート処理を要求しました。しばらく経ってから再度アクセスしてください。");
        return REDIRECT + UriComponentsBuilder.fromUri(V2_EDINET_PATH).toUriString();
    }

    /**
     * EDINETから提出書類一覧を取得する
     *
     * @param fromDate 提出日
     * @param toDate   提出日
     * @return EdinetList
     */
    @PostMapping("/v2/edinet-list")
    public String saveEdinet(final String fromDate, final String toDate) {
        edinetService.saveEdinetList(BetweenDateInputData.of(LocalDate.parse(fromDate), LocalDate.parse(toDate)));
        return REDIRECT + UriComponentsBuilder.fromUri(V2_EDINET_PATH).toUriString();
    }

    /**
     * EDINETリストをアップデートする
     *
     * @param date 提出日
     * @return EdinetList
     */
    @PostMapping("/v1/update/edinet-list")
    public String updateEdinetList(final String date) {
        viewService.updateEdinetListView(DateInputData.of(LocalDate.parse(date)));
        return REDIRECT + UriComponentsBuilder.fromUri(V2_EDINET_PATH).toUriString();
    }

    /**
     * 財務諸表の値を登録する
     *
     * @param date      提出日
     * @param inputData 財務諸表の登録情報
     * @return EdinetDetail
     */
    @PostMapping("/v1/fix-fundamental-value")
    public String registerFinancialStatementValue(
            @ModelAttribute("submitDate") final String date, final FinancialStatementInputData inputData) {
        final Result result = analysisService.registerFinancialStatementValue(inputData);
        if (Result.OK.equals(result)) {
            return REDIRECT + UriComponentsBuilder.fromUri(V2_EDINET_DETAIL_PATH)
                    .queryParam("submitDate", date).build().encode().toUriString();
        } else {
            return REDIRECT + UriComponentsBuilder.fromUri(V2_EDINET_DETAIL_PATH)
                    .queryParam("submitDate", date)
                    .queryParam("error", "登録に失敗しました。").build().encode().toUriString();
        }
    }

    /**
     * 処理ステータスを更新する
     *
     * @param date       提出日
     * @param documentId 書類ID
     * @return EdinetDetail
     */
    @PostMapping("/v1/update/all-done/status")
    public String updateAllDoneStatus(final String date, final String documentId) {
        final Result result = edinetService.updateAllDoneStatus(IdInputData.of(documentId));
        if (Result.OK.equals(result)) {
            return REDIRECT + UriComponentsBuilder.fromUri(V2_EDINET_DETAIL_PATH)
                    .queryParam("submitDate", date).build().encode().toUriString();
        } else {
            return REDIRECT + UriComponentsBuilder.fromUri(V2_EDINET_DETAIL_PATH)
                    .queryParam("submitDate", date)
                    .queryParam("error", "登録に失敗しました。").build().encode().toUriString();
        }
    }

    /**
     * 財務諸表を分析する
     *
     * @param date 提出日
     * @return EdinetDetail
     */
    @PostMapping("/v1/analyze/date")
    public String analyzeByDate(final String date) {
        analysisService.analyzeByDate(DateInputData.of(LocalDate.parse(date)));
        return REDIRECT + UriComponentsBuilder.fromUri(V2_EDINET_DETAIL_PATH)
                .queryParam("submitDate", date).build().encode().toUriString();
    }

    /**
     * 指定書類IDをスクレイピング/分析する
     *
     * @param date       提出日
     * @param documentId 書類ID
     * @return EdinetDetail
     */
    @PostMapping("/v2/scrape/id")
    public String scrapeById(final String date, final String documentId) {
        analysisService.executeById(IdInputData.of(documentId));
        return REDIRECT + UriComponentsBuilder.fromUri(V2_EDINET_DETAIL_PATH)
                .queryParam("submitDate", date).build().encode().toUriString();
    }

    /**
     * 対象書類IDを処理対象外にする
     *
     * @param submitDate 対象提出日
     * @param documentId 書類ID
     * @return EdinetDetail
     */
    @PostMapping("/v1/remove/document")
    public String removeDocument(final String submitDate, final String documentId) {
        edinetService.removeDocument(IdInputData.of(documentId));
        return REDIRECT + UriComponentsBuilder.fromUri(V2_EDINET_DETAIL_PATH)
                .queryParam("submitDate", submitDate).build().encode().toUriString();
    }
}
