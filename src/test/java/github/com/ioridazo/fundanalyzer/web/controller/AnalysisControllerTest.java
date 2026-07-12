package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.config.AnalysisCoefficient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.IndustrySpecification;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
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
    private AnalysisResultSpecification analysisResultSpecification;
    private DocumentSpecification documentSpecification;
    private FinancialStatementSpecification financialStatementSpecification;
    private CompanySpecification companySpecification;
    private IndustrySpecification industrySpecification;

    private AnalysisController controller;

    @BeforeEach
    void setUp() {
        analysisService = Mockito.mock(AnalysisService.class);
        viewService = Mockito.mock(ViewService.class);
        analysisResultSpecification = Mockito.mock(AnalysisResultSpecification.class);
        documentSpecification = Mockito.mock(DocumentSpecification.class);
        financialStatementSpecification = Mockito.mock(FinancialStatementSpecification.class);
        companySpecification = Mockito.mock(CompanySpecification.class);
        industrySpecification = Mockito.mock(IndustrySpecification.class);

        controller = new AnalysisController(
                analysisService,
                viewService,
                Mockito.mock(MessageSource.class),
                analysisResultSpecification,
                documentSpecification,
                financialStatementSpecification,
                companySpecification,
                industrySpecification
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

    @DisplayName("previewIndicatorBackfill : 指標バックフィル対象件数を表示する")
    @Test
    void previewIndicatorBackfill() {
        Mockito.when(analysisResultSpecification.findIndicatorBackfillTargets())
                .thenReturn(java.util.List.of(
                        new AnalysisResultEntity(1, "code", LocalDate.parse("2020-06-30"), null, null, null, null, null, null, "120", "4", LocalDate.parse("2020-09-30"), "doc-1", null),
                        new AnalysisResultEntity(2, "code", LocalDate.parse("2021-06-30"), null, null, null, null, null, null, "130", "4", LocalDate.parse("2021-09-30"), "doc-2", null)
                ));

        var actual = UriComponentsBuilder.fromUriString(controller.previewIndicatorBackfill()).build();

        assertEquals("/v3/index", actual.getPath());
        assertEquals("指標バックフィル対象件数: 2件", UriUtils.decode(Objects.requireNonNull(actual.getQueryParams().getFirst("message")), "UTF-8"));
    }

    @DisplayName("backfillIndicator : 対象書類を再計算して upsert する")
    @Test
    void backfillIndicator() {
        final AnalysisResultEntity target = new AnalysisResultEntity(
                1,
                "1234",
                LocalDate.parse("2020-06-30"),
                java.math.BigDecimal.valueOf(100),
                null,
                null,
                null,
                null,
                null,
                "120",
                "4",
                LocalDate.parse("2020-09-30"),
                "doc-1",
                null
        );
        final Document document = new Document(
                "doc-1",
                DocumentTypeCode.DTC_120,
                QuarterType.QT_OTHER,
                "edinetCode",
                LocalDate.parse("2020-06-30"),
                LocalDate.parse("2020-09-30"),
                null,
                null,
                DocumentStatus.DONE,
                DocumentStatus.DONE,
                DocumentStatus.DONE,
                null,
                DocumentStatus.DONE,
                null,
                DocumentStatus.DONE,
                null,
                false
        );
        final Company company = new Company(
                "1234",
                "company",
                10,
                "industry",
                "edinetCode",
                null,
                null,
                null,
                null,
                false,
                false,
                true
        );
        final FinanceValue financeValue = FinanceValue.of(
                1000L,
                100L,
                1200L,
                200L,
                50L,
                0L,
                800L,
                300L,
                150L,
                10L
        );
        final AnalysisCoefficient coefficient = new AnalysisCoefficient(
                java.math.BigDecimal.TEN,
                java.math.BigDecimal.valueOf(1.2),
                java.math.BigDecimal.valueOf(0.08)
        );

        Mockito.when(analysisResultSpecification.findIndicatorBackfillTargets()).thenReturn(java.util.List.of(target));
        Mockito.when(documentSpecification.findDocument("doc-1")).thenReturn(document);
        Mockito.when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(java.util.Optional.of(company));
        Mockito.when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);
        Mockito.when(industrySpecification.resolveCoefficient(10)).thenReturn(coefficient);

        assertEquals("redirect:/v3/index?message=%E6%8C%87%E6%A8%99%E3%83%90%E3%83%83%E3%82%AF%E3%83%95%E3%82%A3%E3%83%AB%E5%AE%8C%E4%BA%86:%20success%3D1,%20skipped%3D0", controller.backfillIndicator());
        Mockito.verify(analysisResultSpecification, Mockito.times(1)).upsert(Mockito.eq(document), Mockito.any(AnalysisResult.class));
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
}
