package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.AverageInfo;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyzeInteractorTest {

    private DocumentSpecification documentSpecification;
    private FinancialStatementSpecification financialStatementSpecification;
    private AnalysisResultSpecification analysisResultSpecification;

    private AnalyzeInteractor analyzeInteractor;

    @BeforeEach
    void setUp() {
        documentSpecification = Mockito.mock(DocumentSpecification.class);
        financialStatementSpecification = Mockito.mock(FinancialStatementSpecification.class);
        analysisResultSpecification = Mockito.mock(AnalysisResultSpecification.class);

        analyzeInteractor = Mockito.spy(new AnalyzeInteractor(
                Mockito.mock(CompanySpecification.class),
                documentSpecification,
                financialStatementSpecification,
                analysisResultSpecification
        ));
    }

    @Nested
    class analyze {

        Document document = defaultDocument();
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
    }

    @Nested
    class calculateCorporateValue {

        Company company = defaultCompany();

        @DisplayName("calculateCorporateValue : 最新企業価値が存在しないとき")
        @Test
        void latestCorporateValue_isEmpty() {
            when(analysisResultSpecification.latestCorporateValue(company)).thenReturn(Optional.empty());
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
            when(analysisResultSpecification.latestCorporateValue(company)).thenReturn(Optional.of(BigDecimal.TEN));
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
            when(analysisResultSpecification.latestCorporateValue(company)).thenReturn(Optional.of(BigDecimal.TEN));
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
            when(analysisResultSpecification.latestCorporateValue(company)).thenReturn(Optional.of(BigDecimal.TEN));
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
            when(analysisResultSpecification.latestCorporateValue(company)).thenReturn(Optional.of(BigDecimal.TEN));
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
            when(analysisResultSpecification.latestCorporateValue(company)).thenReturn(Optional.of(BigDecimal.TEN));
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
            when(analysisResultSpecification.latestCorporateValue(company)).thenReturn(Optional.of(BigDecimal.TEN));
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
            when(analysisResultSpecification.latestCorporateValue(company)).thenReturn(Optional.of(BigDecimal.TEN));
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
            when(analysisResultSpecification.latestCorporateValue(company)).thenReturn(Optional.of(BigDecimal.TEN));
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


    private Document defaultDocument() {
        return new Document(
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
    }

    private Company defaultCompany() {
        return new Company(
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
    }
}