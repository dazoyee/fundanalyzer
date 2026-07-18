package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ValuationSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.ValuationCatchUpResult;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ValuationInteractorTest {

    private CompanySpecification companySpecification;
    private AnalysisResultSpecification analysisResultSpecification;
    private StockSpecification stockSpecification;
    private ValuationSpecification valuationSpecification;

    private ValuationInteractor valuationInteractor;

    @BeforeEach
    void setUp() {
        companySpecification = Mockito.mock(CompanySpecification.class);
        analysisResultSpecification = Mockito.mock(AnalysisResultSpecification.class);
        stockSpecification = Mockito.mock(StockSpecification.class);
        valuationSpecification = Mockito.mock(ValuationSpecification.class);

        valuationInteractor = Mockito.spy(new ValuationInteractor(
                companySpecification,
                analysisResultSpecification,
                stockSpecification,
                valuationSpecification
        ));
        valuationInteractor.forwardSnapLimitDays = 45;
        valuationInteractor.catchUpMaxIterations = 60;
    }

    private AnalysisResultEntity analysisResult(final LocalDate submitDate) {
        return new AnalysisResultEntity(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                submitDate,
                null,
                null
        );
    }

    private ValuationEntity valuationEntity(final LocalDate targetDate, final LocalDate submitDate) {
        return new ValuationEntity(
                null,
                null,
                submitDate,
                targetDate,
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

    @Nested
    class evaluate {

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
                    .when(valuationInteractor).findPresentStock("code", LocalDate.parse("2022-07-09"));

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
            Mockito.when(stockSpecification.findFirstStockFromDate(
                    "code",
                    LocalDate.parse("2022-07-09"),
                    LocalDate.parse("2022-08-23"))).thenReturn(Optional.empty());

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

        @DisplayName("findPresentStock : 近傍で見つからないときは45日以内の最初の株価へ前方スナップする")
        @Test
        void forwardSnap() {
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-09"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-10"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-11"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-12"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-13"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findFirstStockFromDate(
                    "code",
                    LocalDate.parse("2022-07-09"),
                    LocalDate.parse("2022-08-23"))).thenReturn(Optional.of(stockPriceEntity()));

            var actual = valuationInteractor.findPresentStock("code", LocalDate.parse("2022-07-09"));

            assertTrue(actual.isPresent());
            Mockito.verify(stockSpecification, Mockito.times(1)).findFirstStockFromDate(
                    "code",
                    LocalDate.parse("2022-07-09"),
                    LocalDate.parse("2022-08-23"));
        }

        @DisplayName("findPresentStock : 前方スナップ上限を超えると取得しない")
        @Test
        void forwardSnapOverLimit() {
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-09"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-10"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-11"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-12"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findStock("code", LocalDate.parse("2022-07-13"))).thenReturn(Optional.empty());
            Mockito.when(stockSpecification.findFirstStockFromDate(
                    "code",
                    LocalDate.parse("2022-07-09"),
                    LocalDate.parse("2022-08-23"))).thenReturn(Optional.empty());

            var actual = valuationInteractor.findPresentStock("code", LocalDate.parse("2022-07-09"));

            assertTrue(actual.isEmpty());
        }
    }

    @Nested
    class catchUp {

        private Company company(final String code) {
            return new Company(code, null, null, null, null, null, null, null, null, false, false, true);
        }

        @DisplayName("catchUp : 進捗しなくなった時点で会社ごとの反復を停止する")
        @Test
        void stopsWhenNoLongerProgresses() {
            Mockito.when(companySpecification.inquiryAllTargetCompanies())
                    .thenReturn(List.of(company("1111")));
            Mockito.when(analysisResultSpecification.findLatestAnalysisResult("1111"))
                    .thenReturn(Optional.of(analysisResult(LocalDate.parse("2022-07-09"))));
            Mockito.when(valuationSpecification.findLatestValuation("1111", LocalDate.parse("2022-07-09")))
                    .thenReturn(Optional.of(valuationEntity(LocalDate.parse("2022-07-09"), LocalDate.parse("2022-07-09"))));
            Mockito.when(stockSpecification.findLatestStock("1111"))
                    .thenReturn(Optional.of(stockPriceEntity(LocalDate.parse("2022-09-09"))));
            Mockito.doReturn(true, true, false).when(valuationInteractor).evaluate(CodeInputData.of("1111"));
            Mockito.doReturn(Optional.empty()).when(valuationInteractor).findPresentStock(eq("1111"), any());

            final ValuationCatchUpResult actual = valuationInteractor.catchUp();

            assertEquals(1, actual.targetCompanyCount());
            assertEquals(2, actual.advancedCount());
            assertEquals(1, actual.unresolvedCompanyCount());
        }

        @DisplayName("previewCatchUp : 対象会社数のみ返す")
        @Test
        void preview() {
            Mockito.when(companySpecification.inquiryAllTargetCompanies())
                    .thenReturn(List.of(company("1111"), company("2222")));
            Mockito.when(analysisResultSpecification.findLatestAnalysisResult("1111"))
                    .thenReturn(Optional.of(analysisResult(LocalDate.parse("2022-07-09"))));
            Mockito.when(analysisResultSpecification.findLatestAnalysisResult("2222"))
                    .thenReturn(Optional.of(analysisResult(LocalDate.parse("2022-07-09"))));
            Mockito.when(valuationSpecification.findLatestValuation(any(), eq(LocalDate.parse("2022-07-09"))))
                    .thenReturn(Optional.of(valuationEntity(LocalDate.parse("2022-07-09"), LocalDate.parse("2022-07-09"))));
            Mockito.when(stockSpecification.findLatestStock("1111"))
                    .thenReturn(Optional.of(stockPriceEntity(LocalDate.parse("2022-08-10"))));
            Mockito.when(stockSpecification.findLatestStock("2222"))
                    .thenReturn(Optional.of(stockPriceEntity(LocalDate.parse("2022-07-01"))));
            Mockito.doReturn(Optional.empty()).when(valuationInteractor).findPresentStock(any(), any());

            final var actual = valuationInteractor.previewCatchUp();

            assertEquals(1, actual.targetCompanyCount());
        }
    }

    private StockPriceEntity stockPriceEntity() {
        return stockPriceEntity(LocalDate.parse("2023-03-25"));
    }

    private StockPriceEntity stockPriceEntity(final LocalDate targetDate) {
        return new StockPriceEntity(
                null,
                "code",
                targetDate,
                1000.0,
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
