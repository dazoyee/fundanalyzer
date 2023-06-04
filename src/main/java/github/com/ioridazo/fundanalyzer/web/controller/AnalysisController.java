package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import jakarta.annotation.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Controller
public class AnalysisController {

    private static final String REDIRECT = "redirect:";
    private static final URI V2_INDEX_PATH = URI.create("/v2/index");
    private static final URI V2_CORPORATE_PATH = URI.create("/v2/corporate");
    private static final URI V2_VALUATION_PATH = URI.create("/v2/valuation");

    private static final String MESSAGE = "message";
    private static final String DATETIME_FORMAT = "MM/dd/uuuu";

    private final AnalysisService analysisService;
    private final ViewService viewService;
    private final MessageSource messageSource;

    public AnalysisController(
            final AnalysisService analysisService,
            final ViewService viewService,
            final MessageSource messageSource) {
        this.analysisService = analysisService;
        this.viewService = viewService;
        this.messageSource = messageSource;
    }

    /**
     * 指定提出日の書類をメインの一連処理をする
     *
     * @param fromToDate 提出日
     * @return Index
     */
    @PostMapping("/v1/document/analysis")
    public String doMain(final String fromToDate) {
        final LocalDate fromDate = LocalDate.parse(fromToDate.substring(0, 10), DateTimeFormatter.ofPattern(DATETIME_FORMAT));
        final LocalDate toDate = LocalDate.parse(fromToDate.substring(13, 23), DateTimeFormatter.ofPattern(DATETIME_FORMAT));

        analysisService.executePartOfMain(BetweenDateInputData.of(fromDate, toDate));

        return REDIRECT + UriComponentsBuilder.fromUri(V2_INDEX_PATH).toUriString();
    }

    /**
     * 表示をアップデートする
     *
     * @return Index
     */
    @GetMapping("/v1/update/corporate/view")
    public String updateCorporateView() {
        viewService.updateCorporateView();
        return REDIRECT + UriComponentsBuilder.fromUri(V2_INDEX_PATH)
                .queryParam(MESSAGE, "表示アップデート処理を要求しました。しばらく経ってから再度アクセスしてください。")
                .build().encode().toUriString();
    }

    /**
     * 指定提出日の書類をスクレイピング/分析する
     *
     * @param date 提出日
     * @return Index
     */
    @PostMapping("/v1/scrape/date")
    public String scrapeByDate(final String date) {
        analysisService.executeByDate(DateInputData.of(LocalDate.parse(date)));
        return REDIRECT + UriComponentsBuilder.fromUri(V2_INDEX_PATH).toUriString();
    }

    /**
     * 指定書類IDをスクレイピング/分析する
     *
     * @param documentId         書類ID（CSVで複数可能）
     * @param redirectAttributes redirectAttributes
     * @return Index
     */
    @PostMapping("/v1/scrape/id")
    public String scrapeById(final String documentId, final RedirectAttributes redirectAttributes) {
        final List<String> idList = Arrays.stream(documentId.split(","))
                .filter(dId -> dId.length() == 8)
                .toList();
        if (idList.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    MESSAGE,
                    messageSource.getMessage("github.com.ioridazo.fundanalyzer.web.controller.parameter.invalid", new String[]{}, Locale.getDefault())
            );
        }

        idList.stream().map(IdInputData::of).forEach(inputData -> {
            try {
                analysisService.executeById(inputData);
            } catch (final FundanalyzerNotExistException e) {
                redirectAttributes.addFlashAttribute(
                        MESSAGE,
                        messageSource.getMessage("github.com.ioridazo.fundanalyzer.web.controller.parameter.invalid", new String[]{}, Locale.getDefault())
                );
            }
        });

        return REDIRECT + UriComponentsBuilder.fromUri(V2_INDEX_PATH).toUriString();
    }

    /**
     * 指定日に提出された企業の株価を取得する
     *
     * @param fromToDate 提出日
     * @return Valuation
     */
    @PostMapping("/v2/import/stock/date")
    public String importStockBySubmitDate(final String fromToDate, RedirectAttributes redirectAttributes) {
        final LocalDate fromDate = LocalDate.parse(fromToDate.substring(0, 10), DateTimeFormatter.ofPattern(DATETIME_FORMAT));
        final LocalDate toDate = LocalDate.parse(fromToDate.substring(13, 23), DateTimeFormatter.ofPattern(DATETIME_FORMAT));

        analysisService.importStock(BetweenDateInputData.of(fromDate, toDate));

        redirectAttributes.addFlashAttribute(
                MESSAGE,
                messageSource.getMessage(
                        "github.com.ioridazo.fundanalyzer.web.controller.import.stock.success",
                        new String[]{fromDate.toString(), toDate.toString()},
                        Locale.getDefault())
        );
        return REDIRECT + UriComponentsBuilder.fromUri(V2_VALUATION_PATH).toUriString();
    }

    /**
     * 企業の株価を取得する
     *
     * @param code 会社コード
     * @return BrandDetail
     */
    @PostMapping("/v1/import/stock/code")
    public String importStockByCode(final String code) {
        analysisService.importStock(CodeInputData.of(code));
        analysisService.indicate(CodeInputData.of(code));
        return REDIRECT + UriComponentsBuilder.fromUri(V2_CORPORATE_PATH)
                .queryParam("code", code.substring(0, 4)).toUriString();
    }

    /**
     * 企業をお気に入りに登録する
     *
     * @param code               会社コード
     * @param redirectAttributes redirectAttributes
     * @return BrandDetail
     */
    @PostMapping("/v2/favorite/company")
    public String updateFavoriteCompany(final String code, final RedirectAttributes redirectAttributes) {
        final boolean isFavorite = analysisService.updateFavoriteCompany(CodeInputData.of(code));
        redirectAttributes.addFlashAttribute("isFavorite", isFavorite);
        return REDIRECT + UriComponentsBuilder.fromUri(V2_CORPORATE_PATH)
                .queryParam("code", code.substring(0, 4)).toUriString();
    }

    /**
     * 株価を評価する
     *
     * @param code               会社コード
     * @param redirectAttributes redirectAttributes
     * @return BrandDetail
     */
    @PostMapping("/v2/evaluate")
    public String evaluate(@Nullable final String code, final RedirectAttributes redirectAttributes) {
        if (Objects.nonNull(code)) {
            final boolean isEvaluated = analysisService.evaluate(CodeInputData.of(code));
            if (isEvaluated) {
                viewService.updateValuationView(CodeInputData.of(code));
            }
            redirectAttributes.addFlashAttribute("isEvaluated", isEvaluated);
            return REDIRECT + UriComponentsBuilder.fromUri(V2_CORPORATE_PATH)
                    .queryParam("code", code.substring(0, 4)).toUriString();
        } else {
            final int countValuation = analysisService.evaluate();
            viewService.updateValuationView();
            redirectAttributes.addFlashAttribute(
                    MESSAGE,
                    messageSource.getMessage(
                            "github.com.ioridazo.fundanalyzer.web.controller.valuation.success",
                            new String[]{String.valueOf(countValuation)},
                            Locale.getDefault())
            );
            return REDIRECT + UriComponentsBuilder.fromUri(V2_VALUATION_PATH).toUriString();
        }
    }
}
