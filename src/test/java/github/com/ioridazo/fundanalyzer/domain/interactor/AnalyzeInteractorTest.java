package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.InvestmentIndicatorSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.AverageInfo;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyzeInteractorTest {

    private DocumentSpecification documentSpecification;
    private FinancialStatementSpecification financialStatementSpecification;
    private AnalysisResultSpecification analysisResultSpecification;
    private StockSpecification stockSpecification;
    private InvestmentIndicatorSpecification investmentIndicatorSpecification;

    private AnalyzeInteractor analyzeInteractor;

    @BeforeEach
    void setUp() {
        documentSpecification = Mockito.mock(DocumentSpecification.class);
        financialStatementSpecification = Mockito.mock(FinancialStatementSpecification.class);
        analysisResultSpecification = Mockito.mock(AnalysisResultSpecification.class);
        stockSpecification = mock(StockSpecification.class);
        investmentIndicatorSpecification = mock(InvestmentIndicatorSpecification.class);

        analyzeInteractor = Mockito.spy(new AnalyzeInteractor(
                Mockito.mock(CompanySpecification.class),
                documentSpecification,
                financialStatementSpecification,
                analysisResultSpecification,
                stockSpecification,
                investmentIndicatorSpecification
        ));
        analyzeInteractor.targetTypeCodes = List.of("120", "130");
    }

    @Nested
    class analyze {

        Document document = new Document(
                "documentId",
                null,
                null,
                "edinetCode",
                null,
                LocalDate.now(),
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
                false
        );

        AnalysisResultEntity analysisResult = analysisResultEntity();

        DateInputData inputData = DateInputData.of(LocalDate.parse("2021-05-15"));

        @DisplayName("analyze : 企業価値を算出する")
        @Test
        void document() {
            var financeValue = FinanceValue.of(
                    100L,
                    101L,
                    102L,
                    103L,
                    104L,
                    105L,
                    106L,
                    107L,
                    108L,
                    109L
            );

            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);
            assertDoesNotThrow(() -> analyzeInteractor.analyze(document));
            verify(analysisResultSpecification, times(1)).insert(any(), any());
        }

        @DisplayName("analyze : 分析時にエラーが発生したときの処理を確認する")
        @Test
        void exception() {
            var financeValue = FinanceValue.of(
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

            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);
            assertDoesNotThrow(() -> analyzeInteractor.analyze(document));
            verify(analysisResultSpecification, times(0)).insert(any(), any());
            verify(documentSpecification, times(1)).updateFsToHalfWay(any(), any());
        }

        @DisplayName("analyze : 提出日の企業価値を算出する")
        @Test
        void inputData_present() {
            when(documentSpecification.analysisTargetList(inputData)).thenReturn(List.of(document));
            doNothing().when(analyzeInteractor).analyze(document);
            assertDoesNotThrow(() -> analyzeInteractor.analyze(inputData));
            verify(analyzeInteractor, times(1)).analyze(document);
        }

        @DisplayName("analyze : 対象が存在しないときはなにもしない")
        @Test
        void inputData_empty() {
            when(documentSpecification.analysisTargetList(inputData)).thenReturn(List.of());
            doNothing().when(analyzeInteractor).analyze(document);
            assertDoesNotThrow(() -> analyzeInteractor.analyze(inputData));
            verify(analyzeInteractor, times(0)).analyze(document);
        }

        @DisplayName("analyze : 投資指標を算出する")
        @Test
        void indicate_ok() {
            var financeValue = FinanceValue.of(
                    100L,
                    101L,
                    102L,
                    103L,
                    104L,
                    105L,
                    106L,
                    107L,
                    108L,
                    109L
            );

            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);
            when(documentSpecification.findLatestDocument("edinetCode")).thenReturn(Optional.of(document));
            when(analysisResultSpecification.findAnalysisResult("documentId"))
                    .thenReturn(Optional.of(analysisResult));

            assertDoesNotThrow(() -> analyzeInteractor.analyze(document));
            verify(analysisResultSpecification, times(1)).insert(any(), any());
            verify(analyzeInteractor, times(1)).indicate(analysisResult);
        }

        @DisplayName("analyze : 最新ドキュメントが存在しないときは投資指標を算出しない")
        @Test
        void indicate_latest_isEmpty() {
            var financeValue = FinanceValue.of(
                    100L,
                    101L,
                    102L,
                    103L,
                    104L,
                    105L,
                    106L,
                    107L,
                    108L,
                    109L
            );

            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);
            when(documentSpecification.findLatestDocument("edinetCode")).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> analyzeInteractor.analyze(document));
            verify(analysisResultSpecification, times(1)).insert(any(), any());
            verify(analyzeInteractor, times(0)).indicate((AnalysisResultEntity) any());
        }

        @DisplayName("analyze : 処理対象が最新でないときは投資指標を算出しない")
        @Test
        void indicate_noLatest() {
            var financeValue = FinanceValue.of(
                    100L,
                    101L,
                    102L,
                    103L,
                    104L,
                    105L,
                    106L,
                    107L,
                    108L,
                    109L
            );

            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);
            when(documentSpecification.findLatestDocument("edinetCode"))
                    .thenReturn(Optional.of(new Document(
                            "documentId2",
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
                            false
                    )));

            assertDoesNotThrow(() -> analyzeInteractor.analyze(document));
            verify(analysisResultSpecification, times(1)).insert(any(), any());
            verify(analyzeInteractor, times(0)).indicate((AnalysisResultEntity) any());
        }
    }

    @Nested
    class calculateCorporateValue {

        Company company = new Company(
                "code",
                null,
                null,
                "edinetCode",
                null,
                null,
                null,
                null,
                null,
                false,
                true
        );

        AnalysisResultEntity analysisResult = analysisResultEntity();

        @DisplayName("calculateCorporateValue : 最新企業価値が存在しないとき")
        @Test
        void latestCorporateValue_isEmpty() {
            when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.empty());
            var actual = analyzeInteractor.calculateCorporateValue(company);

            assertAll(
                    () -> assertNull(actual.getLatestCorporateValue().orElse(null)),
                    () -> assertEquals(List.of(), actual.getAverageInfoList()),
                    () -> assertNull(actual.getCountYear().orElse(null))
            );
        }

        @DisplayName("calculateCorporateValue : 平均企業価値が存在しないとき")
        @ParameterizedTest
        @CsvSource({"0,3", "1,5", "2,10"})
        void averageCorporateValue_year_isEmpty(int index, int year) {
            when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.of(analysisResult));
            when(analysisResultSpecification.yearAverageCorporateValue(company, year)).thenReturn(Optional.empty());
            var actual = analyzeInteractor.calculateCorporateValue(company);

            assertAll(
                    () -> assertEquals(BigDecimal.TEN, actual.getLatestCorporateValue().orElse(null)),
                    () -> assertAll(
                            () -> assertEquals(AverageInfo.yearFromValue(year), actual.getAverageInfoList().get(index).getYear()),
                            () -> assertNull(actual.getAverageInfoList().get(index).getAverageCorporateValue().orElse(null)),
                            () -> assertNull(actual.getAverageInfoList().get(index).getStandardDeviation().orElse(null)),
                            () -> assertNull(actual.getAverageInfoList().get(index).getCoefficientOfVariation().orElse(null))
                    ),
                    () -> assertNull(actual.getCountYear().orElse(null))
            );
        }

        @DisplayName("calculateCorporateValue : 平均企業価値が存在しないとき")
        @Test
        void averageCorporateValue_allYear_isEmpty() {
            when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.of(analysisResult));
            when(analysisResultSpecification.allYearAverageCorporateValue(company)).thenReturn(Optional.empty());
            var actual = analyzeInteractor.calculateCorporateValue(company);

            assertAll(
                    () -> assertEquals(BigDecimal.TEN, actual.getLatestCorporateValue().orElse(null)),
                    () -> assertAll(
                            () -> assertEquals(AverageInfo.Year.ALL, actual.getAverageInfoList().get(3).getYear()),
                            () -> assertNull(actual.getAverageInfoList().get(3).getAverageCorporateValue().orElse(null)),
                            () -> assertNull(actual.getAverageInfoList().get(3).getStandardDeviation().orElse(null)),
                            () -> assertNull(actual.getAverageInfoList().get(3).getCoefficientOfVariation().orElse(null))
                    ),
                    () -> assertNull(actual.getCountYear().orElse(null))
            );
        }

        @DisplayName("calculateCorporateValue : 標準偏差が存在しないとき")
        @ParameterizedTest
        @CsvSource({"0,3", "1,5", "2,10"})
        void standardDeviation_year_isEmpty(int index, int year) {
            when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.of(analysisResult));
            when(analysisResultSpecification.yearAverageCorporateValue(company, year)).thenReturn(Optional.of(BigDecimal.TEN));
            when(analysisResultSpecification.standardDeviation(company, BigDecimal.TEN)).thenReturn(Optional.empty());
            var actual = analyzeInteractor.calculateCorporateValue(company);

            assertAll(
                    () -> assertEquals(BigDecimal.TEN, actual.getLatestCorporateValue().orElse(null)),
                    () -> assertAll(
                            () -> assertEquals(AverageInfo.yearFromValue(year), actual.getAverageInfoList().get(index).getYear()),
                            () -> assertEquals(BigDecimal.TEN, actual.getAverageInfoList().get(index).getAverageCorporateValue().orElse(null)),
                            () -> assertNull(actual.getAverageInfoList().get(index).getStandardDeviation().orElse(null)),
                            () -> assertNull(actual.getAverageInfoList().get(index).getCoefficientOfVariation().orElse(null))
                    ),
                    () -> assertNull(actual.getCountYear().orElse(null))
            );
        }

        @DisplayName("calculateCorporateValue : 標準偏差が存在しないとき")
        @Test
        void standardDeviation_allYear_isEmpty() {
            when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.of(analysisResult));
            when(analysisResultSpecification.allYearAverageCorporateValue(company)).thenReturn(Optional.of(BigDecimal.TEN));
            when(analysisResultSpecification.standardDeviation(company, BigDecimal.TEN)).thenReturn(Optional.empty());
            var actual = analyzeInteractor.calculateCorporateValue(company);

            assertAll(
                    () -> assertEquals(BigDecimal.TEN, actual.getLatestCorporateValue().orElse(null)),
                    () -> assertAll(
                            () -> assertEquals(AverageInfo.Year.ALL, actual.getAverageInfoList().get(3).getYear()),
                            () -> assertEquals(BigDecimal.TEN, actual.getAverageInfoList().get(3).getAverageCorporateValue().orElse(null)),
                            () -> assertNull(actual.getAverageInfoList().get(3).getStandardDeviation().orElse(null)),
                            () -> assertNull(actual.getAverageInfoList().get(3).getCoefficientOfVariation().orElse(null))
                    ),
                    () -> assertNull(actual.getCountYear().orElse(null))
            );
        }

        @DisplayName("calculateCorporateValue : 変動係数が存在しないとき")
        @ParameterizedTest
        @CsvSource({"0,3", "1,5", "2,10"})
        void coefficientOfVariation_year_isEmpty(int index, int year) {
            when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.of(analysisResult));
            when(analysisResultSpecification.yearAverageCorporateValue(company, year)).thenReturn(Optional.of(BigDecimal.TEN));
            when(analysisResultSpecification.standardDeviation(company, BigDecimal.TEN)).thenReturn(Optional.of(BigDecimal.TEN));
            when(analysisResultSpecification.coefficientOfVariation(BigDecimal.TEN, BigDecimal.TEN)).thenReturn(Optional.empty());
            var actual = analyzeInteractor.calculateCorporateValue(company);

            assertAll(
                    () -> assertEquals(BigDecimal.TEN, actual.getLatestCorporateValue().orElse(null)),
                    () -> assertAll(
                            () -> assertEquals(AverageInfo.yearFromValue(year), actual.getAverageInfoList().get(index).getYear()),
                            () -> assertEquals(BigDecimal.TEN, actual.getAverageInfoList().get(index).getAverageCorporateValue().orElse(null)),
                            () -> assertEquals(BigDecimal.TEN, actual.getAverageInfoList().get(index).getStandardDeviation().orElse(null)),
                            () -> assertNull(actual.getAverageInfoList().get(index).getCoefficientOfVariation().orElse(null))
                    ),
                    () -> assertNull(actual.getCountYear().orElse(null))
            );
        }

        @DisplayName("calculateCorporateValue : 変動係数が存在しないとき")
        @Test
        void coefficientOfVariation_allYear_isEmpty() {
            when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.of(analysisResult));
            when(analysisResultSpecification.allYearAverageCorporateValue(company)).thenReturn(Optional.of(BigDecimal.TEN));
            when(analysisResultSpecification.standardDeviation(company, BigDecimal.TEN)).thenReturn(Optional.of(BigDecimal.TEN));
            when(analysisResultSpecification.coefficientOfVariation(BigDecimal.TEN, BigDecimal.TEN)).thenReturn(Optional.empty());
            var actual = analyzeInteractor.calculateCorporateValue(company);

            assertAll(
                    () -> assertEquals(BigDecimal.TEN, actual.getLatestCorporateValue().orElse(null)),
                    () -> assertAll(
                            () -> assertEquals(AverageInfo.Year.ALL, actual.getAverageInfoList().get(3).getYear()),
                            () -> assertEquals(BigDecimal.TEN, actual.getAverageInfoList().get(3).getAverageCorporateValue().orElse(null)),
                            () -> assertEquals(BigDecimal.TEN, actual.getAverageInfoList().get(3).getStandardDeviation().orElse(null)),
                            () -> assertNull(actual.getAverageInfoList().get(3).getCoefficientOfVariation().orElse(null))
                    ),
                    () -> assertNull(actual.getCountYear().orElse(null))
            );
        }

        @DisplayName("calculateCorporateValue : すべて存在する")
        @ParameterizedTest
        @CsvSource({"0,3", "1,5", "2,10"})
        void present_year(int index, int year) {
            when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.of(analysisResult));
            when(analysisResultSpecification.yearAverageCorporateValue(company, year)).thenReturn(Optional.of(BigDecimal.TEN));
            when(analysisResultSpecification.standardDeviation(company, BigDecimal.TEN)).thenReturn(Optional.of(BigDecimal.TEN));
            when(analysisResultSpecification.coefficientOfVariation(BigDecimal.TEN, BigDecimal.TEN)).thenReturn(Optional.of(BigDecimal.TEN));
            when(analysisResultSpecification.countYear(company)).thenReturn(BigDecimal.ONE);
            var actual = analyzeInteractor.calculateCorporateValue(company);

            assertAll(
                    () -> assertEquals(BigDecimal.TEN, actual.getLatestCorporateValue().orElse(null)),
                    () -> assertAll(
                            () -> assertEquals(AverageInfo.yearFromValue(year), actual.getAverageInfoList().get(index).getYear()),
                            () -> assertEquals(BigDecimal.TEN, actual.getAverageInfoList().get(index).getAverageCorporateValue().orElse(null)),
                            () -> assertEquals(BigDecimal.TEN, actual.getAverageInfoList().get(index).getStandardDeviation().orElse(null)),
                            () -> assertEquals(BigDecimal.TEN, actual.getAverageInfoList().get(index).getCoefficientOfVariation().orElse(null))
                    ),
                    () -> assertEquals(BigDecimal.ONE, actual.getCountYear().orElse(null))
            );
        }

        @DisplayName("calculateCorporateValue : すべて存在する")
        @Test
        void present_allYear() {
            when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.of(analysisResult));
            when(analysisResultSpecification.allYearAverageCorporateValue(company)).thenReturn(Optional.of(BigDecimal.TEN));
            when(analysisResultSpecification.standardDeviation(company, BigDecimal.TEN)).thenReturn(Optional.of(BigDecimal.TEN));
            when(analysisResultSpecification.coefficientOfVariation(BigDecimal.TEN, BigDecimal.TEN)).thenReturn(Optional.of(BigDecimal.TEN));
            when(analysisResultSpecification.countYear(company)).thenReturn(BigDecimal.ONE);
            var actual = analyzeInteractor.calculateCorporateValue(company);

            assertAll(
                    () -> assertEquals(BigDecimal.TEN, actual.getLatestCorporateValue().orElse(null)),
                    () -> assertAll(
                            () -> assertEquals(AverageInfo.Year.ALL, actual.getAverageInfoList().get(3).getYear()),
                            () -> assertEquals(BigDecimal.TEN, actual.getAverageInfoList().get(3).getAverageCorporateValue().orElse(null)),
                            () -> assertEquals(BigDecimal.TEN, actual.getAverageInfoList().get(3).getStandardDeviation().orElse(null)),
                            () -> assertEquals(BigDecimal.TEN, actual.getAverageInfoList().get(3).getCoefficientOfVariation().orElse(null))
                    ),
                    () -> assertEquals(BigDecimal.ONE, actual.getCountYear().orElse(null))
            );
        }
    }

    @Nested
    class indicate {

        private AnalysisResultEntity analysisResultEntity(LocalDate submitDate) {
            return new AnalysisResultEntity(
                    1,
                    "code",
                    null,
                    null,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    null,
                    null,
                    "120",
                    null,
                    submitDate,
                    "documentId",
                    null
            );
        }

        private IndicatorValue indicatorValue(LocalDate targetDate) {
            return new IndicatorValue(
                    null,
                    null,
                    null,
                    null,
                    targetDate
            );
        }

        private StockPriceEntity stockPriceEntity(LocalDate targetDate) {
            return new StockPriceEntity(
                    null,
                    null,
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

        @BeforeEach
        void setUp() {
            when(stockSpecification.findStock(eq("code"), any()))
                    .thenReturn(Optional.of(stockPriceEntity(null)));

        }

        @DisplayName("indicate : 企業コードから投資指標を算出する")
        @Test
        void inputData_code() {
            var analysisResultEntity = analysisResultEntity(null);

            when(analysisResultSpecification.findLatestAnalysisResult("code"))
                    .thenReturn(Optional.of(analysisResultEntity));
            assertDoesNotThrow(() -> analyzeInteractor.indicate(CodeInputData.of("code")));
            verify(analyzeInteractor, times(1)).indicate(analysisResultEntity);

        }

        @DisplayName("indicate : 投資指標を算出する")
        @Test
        void present() {
            when(investmentIndicatorSpecification.findIndicatorValueList(1))
                    .thenReturn(List.of(indicatorValue(LocalDate.parse("2022-11-19"))));
            when(stockSpecification.findLatestStock("code"))
                    .thenReturn(Optional.of(stockPriceEntity(LocalDate.parse("2022-11-26"))));

            assertDoesNotThrow(() -> analyzeInteractor.indicate(analysisResultEntity(LocalDate.parse("2022-11-01"))));
            verify(investmentIndicatorSpecification, times(7)).insert(any(), any());
        }

        @DisplayName("indicate : 書類種別コードが対象外のとき")
        @Test
        void documentTypeCode_isNotTarget() {
            var analysisResultEntity = new AnalysisResultEntity(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "140",
                    null,
                    null,
                    null,
                    null
            );
            assertDoesNotThrow(() -> analyzeInteractor.indicate(analysisResultEntity));
            verify(investmentIndicatorSpecification, times(0)).insert(any(), any());
        }

        @DisplayName("indicate : 株価が存在しないとき")
        @Test
        void stockPrice_isEmpty() {
            assertDoesNotThrow(() -> analyzeInteractor.indicate(analysisResultEntity(LocalDate.parse("2022-11-01"))));
            verify(investmentIndicatorSpecification, times(0)).insert(any(), any());
        }

        @DisplayName("indicate : 投資指標が存在しないとき")
        @Test
        void indicatorValue_isEmpty() {
            when(stockSpecification.findLatestStock("code"))
                    .thenReturn(Optional.of(stockPriceEntity(LocalDate.parse("2022-11-26"))));

            assertDoesNotThrow(() -> analyzeInteractor.indicate(analysisResultEntity(LocalDate.parse("2022-11-01"))));
            verify(investmentIndicatorSpecification, times(26)).insert(any(), any());
        }

        @DisplayName("indicate : 処理対象日付が正しいとき")
        @ParameterizedTest
        @CsvSource({
                "2022-12-01, 2022-12-01",
                "2022-12-01, 2022-12-02",
                "2021-12-01, 2022-12-01"
        })
        void date_ok(String submitDate, String latestDate) {
            when(stockSpecification.findLatestStock("code"))
                    .thenReturn(Optional.of(stockPriceEntity(LocalDate.parse(latestDate))));

            assertDoesNotThrow(() -> analyzeInteractor.indicate(analysisResultEntity(LocalDate.parse(submitDate))));
            verify(investmentIndicatorSpecification, Mockito.atLeastOnce()).insert(any(), any());
            verify(investmentIndicatorSpecification, Mockito.atMost(366)).insert(any(), any());
        }

        @DisplayName("indicate : 処理対象日付が正しくないとき")
        @ParameterizedTest
        @CsvSource({
                "2022-11-01, 2022-10-31",
                "2021-11-01, 2022-11-02"
        })
        void date_ng(String submitDate, String latestDate) {
            when(investmentIndicatorSpecification.findIndicatorValueList(1))
                    .thenReturn(List.of(indicatorValue(LocalDate.parse("2022-11-19"))));
            when(stockSpecification.findLatestStock("code"))
                    .thenReturn(Optional.of(stockPriceEntity(LocalDate.parse(latestDate))));

            assertDoesNotThrow(() -> analyzeInteractor.indicate(analysisResultEntity(LocalDate.parse(submitDate))));
            verify(investmentIndicatorSpecification, times(0)).insert(any(), any());
        }
    }

    private AnalysisResultEntity analysisResultEntity() {
        return new AnalysisResultEntity(
                null,
                null,
                null,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                "120",
                null,
                null,
                "documentId",
                null
        );
    }
}