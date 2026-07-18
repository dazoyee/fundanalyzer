package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.usecase.AnalyzeUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ValuationUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.RecalculationPreview;
import github.com.ioridazo.fundanalyzer.domain.value.RecalculationResult;
import github.com.ioridazo.fundanalyzer.domain.value.ValuationCatchUpPreview;
import github.com.ioridazo.fundanalyzer.domain.value.ValuationCatchUpResult;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.time.LocalDate;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalysisControllerTest {

    private AnalysisService analysisService;
    private ViewService viewService;
    private AnalyzeUseCase analyzeUseCase;
    private ValuationUseCase valuationUseCase;

    private AnalysisController controller;

    @BeforeEach
    void setUp() {
        analysisService = Mockito.mock(AnalysisService.class);
        viewService = Mockito.mock(ViewService.class);
        analyzeUseCase = Mockito.mock(AnalyzeUseCase.class);
        valuationUseCase = Mockito.mock(ValuationUseCase.class);

        controller = new AnalysisController(
                analysisService,
                viewService,
                Mockito.mock(MessageSource.class),
                analyzeUseCase,
                valuationUseCase
        );
    }

    @DisplayName("doMain : 指定提出日の書類を一部メインの一連処理をする")
    @Test
    void doMain() {
        assertEquals("redirect:/v3/index", controller.doMain("05/29/2021 - 05/29/2021"));
        Mockito.verify(analysisService, Mockito.times(1))
                .executePartOfMain(BetweenDateInputData.of(LocalDate.parse("2021-05-29"), LocalDate.parse("2021-05-29")));
    }

    @DisplayName("updateCorporateView : 表示をアップデートする")
    @Test
    void updateCorporateView() {
        var actual = UriComponentsBuilder.fromUriString(controller.updateCorporateView()).build();

        assertEquals("/v3/index", actual.getPath());
        assertEquals(
                "表示アップデート処理を要求しました。しばらく経ってから再度アクセスしてください。",
                UriUtils.decode(Objects.requireNonNull(actual.getQueryParams().getFirst("message")), "UTF-8"));
        Mockito.verify(viewService, Mockito.times(1)).updateCorporateView();
    }

    @DisplayName("scrapeByDate : 指定提出日の書類を分析する")
    @Test
    void scrapeByDate() {
        assertEquals("redirect:/v3/index", controller.scrapeByDate("2021-05-29"));
        Mockito.verify(analysisService, Mockito.times(1))
                .executeByDate(DateInputData.of(LocalDate.parse("2021-05-29")));
    }

    @DisplayName("scrapeById : 指定書類IDを分析する")
    @Test
    void scrapeById() {
        assertEquals("redirect:/v3/index", controller.scrapeById("test1234,test5678", new RedirectAttributesModelMap()));
        Mockito.verify(analysisService, Mockito.times(1)).executeById(IdInputData.of("test1234"));
        Mockito.verify(analysisService, Mockito.times(1)).executeById(IdInputData.of("test5678"));
    }

    @DisplayName("importStock : 指定日に提出した企業の株価を取得する")
    @Test
    void importStock1() {
        assertEquals(
                "redirect:/v3/index",
                controller.importStockBySubmitDate("07/10/2022 - 07/10/2022", new RedirectAttributesModelMap()));
        Mockito.verify(analysisService, Mockito.times(1))
                .importStock(BetweenDateInputData.of(LocalDate.parse("2022-07-10"), LocalDate.parse("2022-07-10")));
    }

    @DisplayName("evaluate : 全企業の株価評価を再計算する")
    @Test
    void evaluateAll() {
        assertEquals("redirect:/v3/index", controller.evaluate(null, new RedirectAttributesModelMap()));
        Mockito.verify(analysisService, Mockito.times(1)).evaluate();
        Mockito.verify(viewService, Mockito.times(1)).updateValuationView();
    }

    @DisplayName("importStock : 企業の株価を取得する")
    @Test
    void importStock2() {
        assertEquals("redirect:/v3/corporate?code=1234", controller.importStockByCode("12345"));
        Mockito.verify(analysisService, Mockito.times(1)).importStock(CodeInputData.of("12345"));
    }

    @DisplayName("previewRecalculate : 係数一括再計算の対象件数を確認する")
    @Test
    void previewRecalculate() {
        Mockito.when(analyzeUseCase.previewRecalculation()).thenReturn(new RecalculationPreview(10, 20));

        var actual = UriComponentsBuilder.fromUriString(controller.previewRecalculate()).build();

        assertEquals("/v3/index", actual.getPath());
        assertEquals(
                "係数一括再計算対象件数: analysis_result=10件, valuation=20件",
                UriUtils.decode(Objects.requireNonNull(actual.getQueryParams().getFirst("message")), "UTF-8"));
    }

    @DisplayName("recalculate : 係数一括再計算を実行する")
    @Test
    void recalculate() {
        Mockito.when(analyzeUseCase.recalculate()).thenReturn(new RecalculationResult(10, 3, 6, 1, 4));

        var actual = UriComponentsBuilder.fromUriString(controller.recalculate()).build();

        assertEquals("/v3/index", actual.getPath());
        assertEquals(
                "係数一括再計算完了: target=10, updated=3, skipped=6, failed=1, valuationUpdated=4",
                UriUtils.decode(Objects.requireNonNull(actual.getQueryParams().getFirst("message")), "UTF-8"));
        Mockito.verify(viewService, Mockito.times(1)).updateCorporateView();
        Mockito.verify(viewService, Mockito.times(1)).updateValuationView();
    }

    @DisplayName("previewValuationCatchUp : catch-up 対象会社数を確認する")
    @Test
    void previewValuationCatchUp() {
        Mockito.when(valuationUseCase.previewCatchUp()).thenReturn(new ValuationCatchUpPreview(7));

        var actual = UriComponentsBuilder.fromUriString(controller.previewValuationCatchUp()).build();

        assertEquals("/v3/index", actual.getPath());
        assertEquals(
                "valuation catch-up 対象会社数: 7件",
                UriUtils.decode(Objects.requireNonNull(actual.getQueryParams().getFirst("message")), "UTF-8"));
    }

    @DisplayName("catchUpValuation : catch-up を実行する")
    @Test
    void catchUpValuation() {
        Mockito.when(valuationUseCase.catchUp()).thenReturn(new ValuationCatchUpResult(7, 12, 1));

        var actual = UriComponentsBuilder.fromUriString(controller.catchUpValuation()).build();

        assertEquals("/v3/index", actual.getPath());
        assertEquals(
                "valuation catch-up 完了: targetCompany=7, advanced=12, unresolvedCompany=1",
                UriUtils.decode(Objects.requireNonNull(actual.getQueryParams().getFirst("message")), "UTF-8"));
        Mockito.verify(viewService, Mockito.times(1)).updateValuationView();
    }
}
