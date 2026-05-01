package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.AnalysisResultViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CompanyViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.FinancialStatementViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.IndicatorViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.MinkabuViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.StockPriceViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorporatePresenterTest {

    private ViewService viewService;
    private CorporatePresenter presenter;

    @BeforeEach
    void setUp() {
        viewService = Mockito.mock(ViewService.class);
        presenter = new CorporatePresenter(viewService);
        presenter.targetTypeCodes = List.of("120", "130");
    }

    /**
     * シンプルな CorporateDetailViewModel を生成する。
     */
    private static CorporateDetailViewModel buildDetailView(
            final List<AnalysisResultViewModel> analysisResultList,
            final List<IndicatorViewModel> indicatorList,
            final List<MinkabuViewModel> minkabuList,
            final List<StockPriceViewModel> stockPriceList) {
        final CompanyViewModel company = new CompanyViewModel(
                "12345", "テスト会社", "情報・通信業", "E00001", Boolean.FALSE,
                100, "3月31日", "10.0", "1.0", "5.0",
                "1000000", "1000000000", "2.5", null
        );
        final CorporateViewModel corporate = new CorporateViewModel();
        final List<FinancialStatementViewModel> financial = List.of();
        return CorporateDetailViewModel.of(
                company,
                "0001",
                "0003",
                corporate,
                analysisResultList,
                indicatorList,
                financial,
                minkabuList,
                stockPriceList
        );
    }

    @Nested
    @DisplayName("corporateDetailView メソッド")
    class CorporateDetailView {

        @DisplayName("target が null の場合 → ViewService.getCorporateDetailView(CodeInputData) が呼ばれて corporate テンプレ名を返す")
        @Test
        void target_null_returnsCorporateTemplate() {
            final String code = "12345";
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), List.of(), List.of());
            final List<CompanyValuationViewModel> valuationList = List.of();

            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(valuationList);

            final Model model = Mockito.mock(Model.class);
            final String actual = presenter.corporateDetailView(code, null, model);

            assertEquals("corporate", actual);
            Mockito.verify(viewService, Mockito.times(1)).getCorporateDetailView(CodeInputData.of(code));
            Mockito.verify(viewService, Mockito.never()).getCorporateDetailView(
                    Mockito.any(CodeInputData.class), Mockito.any(Target.class));
            Mockito.verify(viewService, Mockito.times(1)).getValuationView(CodeInputData.of(code));
        }

        @DisplayName("target が指定された場合 → ViewService.getCorporateDetailView(CodeInputData, Target) が呼ばれて target Attribute が設定される")
        @Test
        void target_specified_setsTargetAttribute() {
            final String code = "12345";
            final String target = "all";
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), List.of(), List.of());

            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code), Target.ALL)).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            final String actual = presenter.corporateDetailView(code, target, model);

            assertEquals("corporate", actual);
            Mockito.verify(viewService, Mockito.times(1))
                    .getCorporateDetailView(CodeInputData.of(code), Target.ALL);
            Mockito.verify(viewService, Mockito.never()).getCorporateDetailView(Mockito.any(CodeInputData.class));
            Mockito.verify(model, Mockito.times(1)).addAttribute("target", "all");
        }

        @DisplayName("不明な target → Target.MAIN にフォールバックする")
        @Test
        void target_unknown_fallsBackToMain() {
            final String code = "12345";
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), List.of(), List.of());

            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code), Target.MAIN)).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            final String actual = presenter.corporateDetailView(code, "unknown-value", model);

            assertEquals("corporate", actual);
            Mockito.verify(viewService, Mockito.times(1))
                    .getCorporateDetailView(CodeInputData.of(code), Target.MAIN);
            Mockito.verify(model, Mockito.times(1)).addAttribute("target", "main");
        }

        @DisplayName("基本属性 (corporate / backwardCode / forwardCode / corporateView / financialStatements) が Model に設定される")
        @Test
        void basicAttributes_areSetOnModel() {
            final String code = "12345";
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), List.of(), List.of());

            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            presenter.corporateDetailView(code, null, model);

            Mockito.verify(model, Mockito.times(1)).addAttribute("corporate", view.getCompany());
            Mockito.verify(model, Mockito.times(1)).addAttribute("backwardCode", "0001");
            Mockito.verify(model, Mockito.times(1)).addAttribute("forwardCode", "0003");
            Mockito.verify(model, Mockito.times(1)).addAttribute("corporateView", view.getCorporate());
            Mockito.verify(model, Mockito.times(1)).addAttribute("financialStatements", view.getFinancialStatement());
        }

        @DisplayName("analysisResults と analysisLabelAll/analysisPointAll が Model に設定される")
        @Test
        void analysisAttributes_areSetOnModel() {
            final String code = "12345";
            final AnalysisResultViewModel matched = new AnalysisResultViewModel(
                    LocalDate.of(2023, 6, 30),
                    LocalDate.of(2023, 3, 31),
                    new BigDecimal("1000"),
                    "120",
                    "0"
            );
            final AnalysisResultViewModel unmatched = new AnalysisResultViewModel(
                    LocalDate.of(2023, 6, 30),
                    LocalDate.of(2023, 3, 31),
                    new BigDecimal("500"),
                    "999",
                    "0"
            );
            final List<AnalysisResultViewModel> analysisList = List.of(matched, unmatched);
            final CorporateDetailViewModel view = buildDetailView(analysisList, List.of(), List.of(), List.of());

            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            presenter.corporateDetailView(code, null, model);

            Mockito.verify(model, Mockito.times(1)).addAttribute("analysisResults", analysisList);

            final ArgumentCaptor<Object> labelCaptor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("analysisLabelAll"), labelCaptor.capture());
            assertEquals(List.of(LocalDate.of(2023, 3, 31)), labelCaptor.getValue());

            final ArgumentCaptor<Object> pointCaptor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("analysisPointAll"), pointCaptor.capture());
            assertEquals(List.of(new BigDecimal("1000")), pointCaptor.getValue());
        }

        @DisplayName("indicators と各期間 (30 / 180 / 365 / All) のラベル/ポイントが Model に設定される")
        @Test
        void indicatorAttributes_areSetOnModel() {
            final String code = "12345";
            final LocalDate today = LocalDate.now();
            final IndicatorViewModel recent = new IndicatorViewModel(
                    today.minusDays(10),
                    new BigDecimal("1.000"),
                    new BigDecimal("10.000"),
                    new BigDecimal("1.500"),
                    new BigDecimal("0.500")
            );
            final IndicatorViewModel old = new IndicatorViewModel(
                    today.minusDays(400),
                    new BigDecimal("2.000"),
                    new BigDecimal("20.000"),
                    new BigDecimal("2.500"),
                    new BigDecimal("1.500")
            );
            final List<IndicatorViewModel> indicatorList = List.of(old, recent);
            final CorporateDetailViewModel view = buildDetailView(List.of(), indicatorList, List.of(), List.of());

            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            presenter.corporateDetailView(code, null, model);

            Mockito.verify(model, Mockito.times(1)).addAttribute("indicators", indicatorList);
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("indicatorLabel30"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("indicatorPoint30"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("indicatorLabel180"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("indicatorPoint180"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("indicatorLabel365"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("indicatorPoint365"), Mockito.any());

            final ArgumentCaptor<Object> labelAllCaptor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("indicatorLabelAll"), labelAllCaptor.capture());
            final List<?> labelAll = (List<?>) labelAllCaptor.getValue();
            assertEquals(2, labelAll.size());
            assertEquals(today.minusDays(400), labelAll.get(0));
            assertEquals(today.minusDays(10), labelAll.get(1));

            final ArgumentCaptor<Object> point30Captor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("indicatorPoint30"), point30Captor.capture());
            assertEquals(List.of(new BigDecimal("0.500")), point30Captor.getValue());
        }

        @DisplayName("forecastStocks と各期間 (180 / 365 / All) のラベル/ポイントが Model に設定される")
        @Test
        void forecastStockAttributes_areSetOnModel() {
            final String code = "12345";
            final LocalDate today = LocalDate.now();
            final MinkabuViewModel recent = new MinkabuViewModel(today.minusDays(10), 1500.0, 1400.0);
            final MinkabuViewModel old = new MinkabuViewModel(today.minusDays(400), 2500.0, 2400.0);
            final List<MinkabuViewModel> minkabuList = List.of(old, recent);
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), minkabuList, List.of());

            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            presenter.corporateDetailView(code, null, model);

            Mockito.verify(model, Mockito.times(1)).addAttribute("forecastStocks", minkabuList);
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("forecastStockLabel180"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("forecastStockPoint180"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("forecastStockLabel365"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("forecastStockPoint365"), Mockito.any());

            final ArgumentCaptor<Object> labelAllCaptor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("forecastStockLabelAll"), labelAllCaptor.capture());
            final List<?> labelAll = (List<?>) labelAllCaptor.getValue();
            assertEquals(2, labelAll.size());

            final ArgumentCaptor<Object> point180Captor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("forecastStockPoint180"), point180Captor.capture());
            assertEquals(List.of(1500.0), point180Captor.getValue());
        }

        @DisplayName("stockPrices と各期間 (30 / 90 / 180 / 365 / All) のラベル/ポイントが Model に設定される")
        @Test
        void stockPriceAttributes_areSetOnModel() {
            final String code = "12345";
            final LocalDate today = LocalDate.now();
            final StockPriceViewModel veryRecent = new StockPriceViewModel(
                    today.minusDays(10), 100.0, 95.0, 110.0, 90.0);
            final StockPriceViewModel mid = new StockPriceViewModel(
                    today.minusDays(60), 200.0, 195.0, 210.0, 190.0);
            final StockPriceViewModel old = new StockPriceViewModel(
                    today.minusDays(400), 300.0, 295.0, 310.0, 290.0);
            final List<StockPriceViewModel> stockList = List.of(old, mid, veryRecent);
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), List.of(), stockList);

            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            presenter.corporateDetailView(code, null, model);

            Mockito.verify(model, Mockito.times(1)).addAttribute("stockPrices", stockList);

            final ArgumentCaptor<Object> point30Captor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("stockPoint30"), point30Captor.capture());
            assertEquals(List.of(100.0), point30Captor.getValue());

            final ArgumentCaptor<Object> point90Captor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("stockPoint90"), point90Captor.capture());
            assertEquals(List.of(200.0, 100.0), point90Captor.getValue());

            final ArgumentCaptor<Object> labelAllCaptor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("stockLabelAll"), labelAllCaptor.capture());
            final List<?> labelAll = (List<?>) labelAllCaptor.getValue();
            assertEquals(3, labelAll.size());
            assertEquals(today.minusDays(400), labelAll.get(0));
            assertEquals(today.minusDays(60), labelAll.get(1));
            assertEquals(today.minusDays(10), labelAll.get(2));

            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("stockLabel30"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("stockLabel90"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("stockLabel180"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("stockPoint180"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("stockLabel365"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("stockPoint365"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("stockPointAll"), Mockito.any());
        }

        @DisplayName("valuations と各期間 (180 / 365 / All) のラベル/ポイントが Model に設定される")
        @Test
        void valuationAttributes_areSetOnModel() {
            final String code = "12345";
            final LocalDate today = LocalDate.now();
            final CompanyValuationViewModel recent = new CompanyValuationViewModel(
                    code, "名前", today.minusDays(10),
                    new BigDecimal("100"), new BigDecimal("0.5"),
                    new BigDecimal("10"), new BigDecimal("0.1"),
                    today.minusDays(20), new BigDecimal("110"),
                    20L, new BigDecimal("-10"), new BigDecimal("0.9"),
                    new BigDecimal("0.6"), new BigDecimal("1000"), new BigDecimal("0.02")
            );
            final CompanyValuationViewModel old = new CompanyValuationViewModel(
                    code, "名前", today.minusDays(400),
                    new BigDecimal("80"), new BigDecimal("0.4"),
                    new BigDecimal("8"), new BigDecimal("0.1"),
                    today.minusDays(410), new BigDecimal("90"),
                    400L, new BigDecimal("-10"), new BigDecimal("0.88"),
                    new BigDecimal("0.5"), new BigDecimal("900"), new BigDecimal("0.02")
            );
            final List<CompanyValuationViewModel> valuationList = List.of(recent, old);
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), List.of(), List.of());

            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(valuationList);

            final Model model = Mockito.mock(Model.class);
            presenter.corporateDetailView(code, null, model);

            Mockito.verify(model, Mockito.times(1)).addAttribute("valuations", valuationList);
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("valuationLabel180"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("valuationPoint180"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("valuationLabel365"), Mockito.any());
            Mockito.verify(model, Mockito.times(1))
                    .addAttribute(Mockito.eq("valuationPoint365"), Mockito.any());

            final ArgumentCaptor<Object> labelAllCaptor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("valuationLabelAll"), labelAllCaptor.capture());
            final List<?> labelAll = (List<?>) labelAllCaptor.getValue();
            assertEquals(2, labelAll.size());
            assertEquals(today.minusDays(400), labelAll.get(0));
            assertEquals(today.minusDays(10), labelAll.get(1));

            final ArgumentCaptor<Object> point180Captor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("valuationPoint180"), point180Captor.capture());
            assertEquals(List.of(new BigDecimal("-10")), point180Captor.getValue());
        }

        @DisplayName("targetTypeCodes に該当する複数 documentPeriod があるとき → 期ごとに最新 submitDate のレコードが採用される")
        @Test
        void analysis_picksLatestSubmitDatePerPeriod() {
            final String code = "12345";
            final AnalysisResultViewModel olderSubmit = new AnalysisResultViewModel(
                    LocalDate.of(2023, 5, 1),
                    LocalDate.of(2023, 3, 31),
                    new BigDecimal("100"),
                    "120",
                    "0"
            );
            final AnalysisResultViewModel newerSubmit = new AnalysisResultViewModel(
                    LocalDate.of(2023, 6, 30),
                    LocalDate.of(2023, 3, 31),
                    new BigDecimal("200"),
                    "120",
                    "0"
            );
            final AnalysisResultViewModel otherPeriod = new AnalysisResultViewModel(
                    LocalDate.of(2022, 6, 30),
                    LocalDate.of(2022, 3, 31),
                    new BigDecimal("50"),
                    "130",
                    "0"
            );
            final List<AnalysisResultViewModel> analysisList = List.of(olderSubmit, newerSubmit, otherPeriod);
            final CorporateDetailViewModel view = buildDetailView(analysisList, List.of(), List.of(), List.of());

            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            presenter.corporateDetailView(code, null, model);

            final ArgumentCaptor<Object> labelCaptor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("analysisLabelAll"), labelCaptor.capture());
            final List<?> labels = (List<?>) labelCaptor.getValue();
            assertNotNull(labels);
            assertEquals(2, labels.size());
            assertEquals(LocalDate.of(2022, 3, 31), labels.get(0));
            assertEquals(LocalDate.of(2023, 3, 31), labels.get(1));

            final ArgumentCaptor<Object> pointCaptor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(model).addAttribute(Mockito.eq("analysisPointAll"), pointCaptor.capture());
            final List<?> points = (List<?>) pointCaptor.getValue();
            assertEquals(2, points.size());
            assertTrue(points.contains(new BigDecimal("200")));
            assertTrue(points.contains(new BigDecimal("50")));
        }
    }

    @Nested
    @DisplayName("corporateDetailViewV3 メソッド")
    class CorporateDetailViewV3 {

        @Test
        @DisplayName("呼び出された場合 → corporate-v2 view 名を返す")
        void returnsCorporateV2() {
            final String code = "9999";
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), List.of(), List.of());
            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            final String result = presenter.corporateDetailViewV3(code, null, model);

            assertEquals("corporate-v2", result);
            Mockito.verify(viewService).getCorporateDetailView(CodeInputData.of(code));
            Mockito.verify(model).addAttribute("corporate", view.getCompany());
        }

        @Test
        @DisplayName("target=quart → ViewService.getCorporateDetailView(CodeInputData, Target) が呼ばれて target Attribute が設定される")
        void targetQuart_callsTargetedView() {
            final String code = "9999";
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), List.of(), List.of());
            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code), Target.QUART)).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            final String result = presenter.corporateDetailViewV3(code, "quart", model);

            assertEquals("corporate-v2", result);
            Mockito.verify(model).addAttribute("target", "quart");
        }
    }
}
