package github.com.ioridazo.fundanalyzer.web.presenter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase.SummaryChartData;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.AnalysisResultViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.StockPriceViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AnalysisPresenterのテスト")
class AnalysisPresenterTest {

    private ViewService viewService;
    private ObjectMapper objectMapper;
    private AnalysisPresenter presenter;

    @BeforeEach
    void setUp() {
        this.viewService = mock(ViewService.class);
        this.objectMapper = mock(ObjectMapper.class);
        this.presenter = new AnalysisPresenter(viewService, objectMapper);
        ReflectionTestUtils.setField(presenter, "targetTypeCodes", List.of("120", "130"));
        ReflectionTestUtils.setField(presenter, "rankingSize", 2);
    }

    @Nested
    @DisplayName("analysisView メソッド")
    class AnalysisView {

        @Test
        @DisplayName("ランキング上位を model に設定して analysis-v2 を返す")
        void returnsAnalysisViewWithRanking() {
            final Model model = mock(Model.class);
            when(viewService.getAllValuationView()).thenReturn(List.of(
                    companyValuation("1111", "A", new BigDecimal("10")),
                    companyValuation("2222", "B", null),
                    companyValuation("3333", "C", new BigDecimal("30")),
                    companyValuation("4444", "D", new BigDecimal("20"))
            ));

            final String result = presenter.analysisView(model);

            assertEquals("analysis-v2", result);
            final ArgumentCaptor<Object> rankingCaptor = ArgumentCaptor.forClass(Object.class);
            verify(model).addAttribute(eq("ranking"), rankingCaptor.capture());
            assertIterableEquals(
                    List.of(
                            companyValuation("3333", "C", new BigDecimal("30")),
                            companyValuation("4444", "D", new BigDecimal("20"))
                    ),
                    castList(rankingCaptor.getValue()));
        }
    }

    @Nested
    @DisplayName("analysisChart メソッド")
    class AnalysisChart {

        @Test
        @DisplayName("チャート用 JSON 属性を設定して fragment 名を返す")
        void returnsChartFragmentWithJsonAttributes() throws JsonProcessingException {
            final String code = "1234";
            final Model model = mock(Model.class);
            when(viewService.getSummaryChartView(any())).thenReturn(new SummaryChartData(
                    List.of(
                            new AnalysisResultViewModel(
                                    LocalDate.of(2024, 5, 15),
                                    LocalDate.of(2024, 3, 31),
                                    new BigDecimal("1000"),
                                    null,
                                    "120",
                                    "FY"),
                            new AnalysisResultViewModel(
                                    LocalDate.of(2024, 4, 15),
                                    LocalDate.of(2024, 3, 31),
                                    new BigDecimal("900"),
                                    null,
                                    "120",
                                    "FY"),
                            new AnalysisResultViewModel(
                                    LocalDate.of(2025, 5, 15),
                                    LocalDate.of(2025, 3, 31),
                                    new BigDecimal("1100"),
                                    null,
                                    "130",
                                    "FY"),
                            new AnalysisResultViewModel(
                                    LocalDate.of(2026, 5, 15),
                                    LocalDate.of(2026, 3, 31),
                                    new BigDecimal("1200"),
                                    null,
                                    "140",
                                    "FY")
                    ),
                    List.of(
                            new StockPriceViewModel(LocalDate.of(2024, 5, 1), 90.0, null, null, null),
                            new StockPriceViewModel(LocalDate.of(2025, 5, 1), 120.0, null, null, null)
                    )));
            when(viewService.getValuationView(any())).thenReturn(List.of(
                    companyValuation(code, "AAA", LocalDate.of(2024, 5, 1), new BigDecimal("10"), new BigDecimal("1.5"), new BigDecimal("1.1")),
                    companyValuation(code, "AAA", LocalDate.of(2024, 4, 1), new BigDecimal("5"), new BigDecimal("1.2"), new BigDecimal("0.9"))
            ));
            when(objectMapper.writeValueAsString(any())).thenReturn(
                    "[\"2024-03-31\",\"2025-03-31\"]",
                    "[1000,1100]",
                    "[90.0,120.0]",
                    "[\"2024-04-01\",\"2024-05-01\"]",
                    "[5,10]",
                    "[1.2,1.5]",
                    "[0.9,1.1]"
            );

            final String result = presenter.analysisChart(code, model);

            assertEquals("fragments/analysis-chart :: chart", result);
            verify(model).addAttribute("chartId", "analysisChart-" + code);
            verify(model).addAttribute("labelsJson", "[\"2024-03-31\",\"2025-03-31\"]");
            verify(model).addAttribute("cvJson", "[1000,1100]");
            verify(model).addAttribute("stJson", "[90.0,120.0]");
            verify(model).addAttribute("trendLabelsJson", "[\"2024-04-01\",\"2024-05-01\"]");
            verify(model).addAttribute("discJson", "[5,10]");
            verify(model).addAttribute("grahamJson", "[1.2,1.5]");
            verify(model).addAttribute("ratioJson", "[0.9,1.1]");

            final ArgumentCaptor<Object> jsonCaptor = ArgumentCaptor.forClass(Object.class);
            verify(objectMapper, org.mockito.Mockito.times(7)).writeValueAsString(jsonCaptor.capture());
            assertIterableEquals(
                    List.of("2024-03-31", "2025-03-31"),
                    castList(jsonCaptor.getAllValues().get(0)));
            assertIterableEquals(
                    List.of(new BigDecimal("1000"), new BigDecimal("1100")),
                    castList(jsonCaptor.getAllValues().get(1)));
            assertIterableEquals(
                    List.of(90.0, 120.0),
                    castList(jsonCaptor.getAllValues().get(2)));
            assertIterableEquals(
                    List.of("2024-04-01", "2024-05-01"),
                    castList(jsonCaptor.getAllValues().get(3)));
            assertIterableEquals(
                    List.of(new BigDecimal("5"), new BigDecimal("10")),
                    castList(jsonCaptor.getAllValues().get(4)));
            assertIterableEquals(
                    List.of(new BigDecimal("1.2"), new BigDecimal("1.5")),
                    castList(jsonCaptor.getAllValues().get(5)));
            assertIterableEquals(
                    List.of(new BigDecimal("0.9"), new BigDecimal("1.1")),
                    castList(jsonCaptor.getAllValues().get(6)));
        }

        @Test
        @DisplayName("存在しないコードの場合は空 JSON を設定する")
        void returnsEmptyJsonWhenCodeDoesNotExist() throws JsonProcessingException {
            final String code = "9999";
            final Model model = mock(Model.class);
            when(viewService.getSummaryChartView(any()))
                    .thenThrow(new FundanalyzerNotExistException("not found"));

            final String result = presenter.analysisChart(code, model);

            assertEquals("fragments/analysis-chart :: chart", result);
            verify(model).addAttribute("chartId", "analysisChart-" + code);
            verify(model).addAttribute("labelsJson", "[]");
            verify(model).addAttribute("cvJson", "[]");
            verify(model).addAttribute("stJson", "[]");
            verify(model).addAttribute("trendLabelsJson", "[]");
            verify(model).addAttribute("discJson", "[]");
            verify(model).addAttribute("grahamJson", "[]");
            verify(model).addAttribute("ratioJson", "[]");
            verify(objectMapper, never()).writeValueAsString(any());
        }
    }

    private CompanyValuationViewModel companyValuation(
            final String code,
            final String name,
            final BigDecimal discountRate) {
        return companyValuation(code, name, LocalDate.of(2025, 1, 1), discountRate, BigDecimal.ONE, BigDecimal.ONE);
    }

    private CompanyValuationViewModel companyValuation(
            final String code,
            final String name,
            final LocalDate targetDate,
            final BigDecimal discountRate,
            final BigDecimal grahamIndex,
            final BigDecimal submitDateRatio) {
        return new CompanyValuationViewModel(
                code,
                name,
                targetDate,
                new BigDecimal("1000"),
                grahamIndex,
                BigDecimal.ZERO,
                discountRate,
                LocalDate.of(2024, 12, 1),
                new BigDecimal("900"),
                30L,
                new BigDecimal("100"),
                submitDateRatio,
                new BigDecimal("0.5"),
                new BigDecimal("2000"),
                new BigDecimal("0.03")
        );
    }

    @SuppressWarnings("unchecked")
    private List<Object> castList(final Object value) {
        return (List<Object>) value;
    }
}
