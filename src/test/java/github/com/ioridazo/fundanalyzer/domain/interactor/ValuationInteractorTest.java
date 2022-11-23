package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.IndustrySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ValuationSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

class ValuationInteractorTest {

    private AnalysisResultSpecification analysisResultSpecification;
    private StockSpecification stockSpecification;
    private ValuationSpecification valuationSpecification;

    private ValuationInteractor valuationInteractor;

    @BeforeEach
    void setUp() {
        analysisResultSpecification = Mockito.mock(AnalysisResultSpecification.class);
        stockSpecification = Mockito.mock(StockSpecification.class);
        valuationSpecification = Mockito.mock(ValuationSpecification.class);

        valuationInteractor = Mockito.spy(new ValuationInteractor(
                Mockito.mock(IndustrySpecification.class),
                Mockito.mock(CompanySpecification.class),
                analysisResultSpecification,
                stockSpecification,
                valuationSpecification
        ));
        valuationInteractor.configDiscountRate = BigDecimal.valueOf(120);
    }

    @Nested
    class evaluate {

        private AnalysisResult analysisResult(LocalDate submitDate) {
            return new AnalysisResult(
                    null,
                    null,
                    null,
                    null,
                    null,
                    submitDate,
                    null
            );
        }

        private ValuationEntity valuationEntity(LocalDate targetDate, LocalDate submitDate) {
            return new ValuationEntity(
                    null,
                    null,
                    targetDate,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    submitDate,
                    null,
                    null,
                    null,
                    null
            );
        }

        @DisplayName("evaluate : 分析結果がないときはなにもしない")
        @Test
        void analysisResult_is_nothing() {
            Mockito.when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> valuationInteractor.evaluate(CodeInputData.of("code")));
            Mockito.verify(valuationSpecification, Mockito.times(0)).insert(any(), any());
        }

        @DisplayName("evaluate : 過去の評価が存在する かつ 株価が存在するとき は評価する")
        @Test
        void evaluate_when_past_evaluation_and_stock_is_present() {
            Mockito.when(analysisResultSpecification.findLatestAnalysisResult("code"))
                    .thenReturn(Optional.of(analysisResult(LocalDate.parse("2022-07-09"))));
            Mockito.when(valuationSpecification.findLatestValuation("code", LocalDate.parse("2022-07-09")))
                    .thenReturn(Optional.of(valuationEntity(LocalDate.parse("2022-07-10"), LocalDate.parse("2022-07-09"))));
            Mockito.doReturn(Optional.of(stockPriceEntity()))
                    .when(valuationInteractor).findPresentStock("code", LocalDate.parse("2022-08-09"));

            assertDoesNotThrow(() -> valuationInteractor.evaluate(CodeInputData.of("code")));
            Mockito.verify(valuationSpecification, Mockito.times(1)).insert(any(), any());
        }

        @DisplayName("evaluate : 過去の評価が存在する かつ 株価が存在しないとき は評価しない")
        @Test
        void evaluate_when_past_evaluation_is_present_but_stock_is_nothing() {
            Mockito.when(analysisResultSpecification.findLatestAnalysisResult("code"))
                    .thenReturn(Optional.of(analysisResult(LocalDate.parse("2022-07-09"))));
            Mockito.when(valuationSpecification.findLatestValuation("code", LocalDate.parse("2022-07-09")))
                    .thenReturn(Optional.of(valuationEntity(LocalDate.parse("2022-07-10"), LocalDate.parse("2022-07-09"))));
            Mockito.doReturn(Optional.empty())
                    .when(valuationInteractor).findPresentStock("code", LocalDate.parse("2022-08-09"));

            assertDoesNotThrow(() -> valuationInteractor.evaluate(CodeInputData.of("code")));
            Mockito.verify(valuationSpecification, Mockito.times(0)).insert(any(), any());
        }

        @DisplayName("evaluate : 過去の評価が存在しない かつ 株価が存在するとき は評価する")
        @Test
        void evaluate_when_past_evaluation_is_nothing_but_stock_is_present() {
            Mockito.when(analysisResultSpecification.findLatestAnalysisResult("code"))
                    .thenReturn(Optional.of(analysisResult(LocalDate.parse("2022-07-09"))));
            Mockito.when(valuationSpecification.findLatestValuation("code", LocalDate.parse("2022-07-09"))).thenReturn(Optional.empty());
            Mockito.doReturn(Optional.of(stockPriceEntity()))
                    .when(valuationInteractor).findPresentStock("code", LocalDate.parse("2022-07-09"));

            assertDoesNotThrow(() -> valuationInteractor.evaluate(CodeInputData.of("code")));
            Mockito.verify(valuationSpecification, Mockito.times(1)).insert(any(), any());
        }

        @DisplayName("evaluate : 過去の評価が存在しない かつ 株価が存在しないとき は評価しない")
        @Test
        void evaluate_when_past_evaluation_and_stock_is_nothing() {
            Mockito.when(analysisResultSpecification.findLatestAnalysisResult("code"))
                    .thenReturn(Optional.of(analysisResult(LocalDate.parse("2022-07-09"))));
            Mockito.when(valuationSpecification.findLatestValuation("code", LocalDate.parse("2022-07-09"))).thenReturn(Optional.empty());
            Mockito.doReturn(Optional.empty())
                    .when(valuationInteractor).findPresentStock("code", LocalDate.parse("2022-08-09"));

            assertDoesNotThrow(() -> valuationInteractor.evaluate(CodeInputData.of("code")));
            Mockito.verify(valuationSpecification, Mockito.times(0)).insert(any(), any());
        }
    }

    @Nested
    class generateValuationDate {

        @DisplayName("generateValuationDate : 株価取得日に近似した評価日付を生成する")
        @ParameterizedTest
        @CsvSource({
                "2022-07-24, 2022-07-24, 2022-07-24",
                "2022-07-24, 2022-07-31, 2022-07-31",
                "2022-04-24, 2022-07-29, 2022-04-29",
                "2022-04-24, 2022-07-30, 2022-04-30",
                "2022-04-24, 2022-07-31, 2022-04-30",
                "2022-06-24, 2022-07-31, 2022-06-30",
                "2022-09-24, 2022-07-31, 2022-09-30",
                "2022-11-24, 2022-07-31, 2022-11-30",
                "2022-02-24, 2022-07-27, 2022-02-27",
                "2022-02-24, 2022-07-28, 2022-02-28",
                "2022-02-24, 2022-07-29, 2022-02-28",
                "2022-02-24, 2022-07-30, 2022-02-28",
                "2022-02-24, 2022-07-31, 2022-02-28",
        })
        void generate(String targetDate, String submitDate, String expected) {
            assertEquals(
                    LocalDate.parse(expected),
                    valuationInteractor.generateValuationDate(LocalDate.parse(targetDate), LocalDate.parse(submitDate)));
        }
    }

    @Nested
    class findPresentStock {

        @DisplayName("findPresentStock : 1回目で取得する")
        @Test
        void present_1() {
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-09")))
                    .thenReturn(Optional.of(stockPriceEntity()));

            var actual = valuationInteractor.findPresentStock("code", LocalDate.parse("2022-07-09"));
            assertTrue(actual.isPresent());
            Mockito.verify(stockSpecification, Mockito.times(1)).findStock(any(), any());
        }

        @DisplayName("findPresentStock : 5回目で取得する")
        @Test
        void present_5() {
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-09"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-10"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-11"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-12"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-13"))).thenReturn(Optional.of(stockPriceEntity()));

            var actual = valuationInteractor.findPresentStock("code", LocalDate.parse("2022-07-09"));
            assertTrue(actual.isPresent());
            Mockito.verify(stockSpecification, Mockito.times(5)).findStock(any(), any());
        }

        @DisplayName("findPresentStock : 6回目は取得できない")
        @Test
        void nothing_6() {
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-09"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-10"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-11"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-12"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-13"))).thenReturn(Optional.empty());

            var actual = valuationInteractor.findPresentStock("code", LocalDate.parse("2022-07-09"));
            assertTrue(actual.isEmpty());
            Mockito.verify(stockSpecification, Mockito.times(5)).findStock(any(), any());
        }

        @DisplayName("findPresentStock : 月を跨がないように取得する")
        @Test
        void present_minus() {
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-31"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-30"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-29"))).thenReturn(Optional.of(stockPriceEntity()));

            var actual = valuationInteractor.findPresentStock("code", LocalDate.parse("2022-07-31"));
            assertTrue(actual.isPresent());
            Mockito.verify(stockSpecification, Mockito.times(3)).findStock(any(), any());
        }
    }

    @Nested
    class viewValuation {

        private CompanyValuationViewModel valuationEntity(BigDecimal disCountRate) {
            return CompanyValuationViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    disCountRate
            );
        }

        @DisplayName("メインビューのフィルターを確認する")
        @Test
        void filter() {
            Mockito.when(valuationSpecification.inquiryAllValuationView())
                    .thenReturn(List.of(
                            valuationEntity(BigDecimal.valueOf(1.1)),
                            valuationEntity(BigDecimal.valueOf(1.2)),
                            valuationEntity(BigDecimal.valueOf(1001))
                    ));

            var actual = valuationInteractor.viewValuation();

            assertEquals(BigDecimal.valueOf(1.2), actual.get(0).getDiscountRate());
            assertEquals(1, actual.size());
        }
    }

    private StockPriceEntity stockPriceEntity() {
        return new StockPriceEntity(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}