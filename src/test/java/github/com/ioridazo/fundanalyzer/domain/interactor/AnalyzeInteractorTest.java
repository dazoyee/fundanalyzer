package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.AverageInfo;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyzeInteractorTest {

    private CompanySpecification companySpecification;
    private DocumentSpecification documentSpecification;
    private FinancialStatementSpecification financialStatementSpecification;
    private AnalysisResultSpecification analysisResultSpecification;

    private AnalyzeInteractor analyzeInteractor;

    @BeforeEach
    void setUp() {
        companySpecification = Mockito.mock(CompanySpecification.class);
        documentSpecification = Mockito.mock(DocumentSpecification.class);
        financialStatementSpecification = Mockito.mock(FinancialStatementSpecification.class);
        analysisResultSpecification = Mockito.mock(AnalysisResultSpecification.class);

        analyzeInteractor = Mockito.spy(new AnalyzeInteractor(
                companySpecification,
                documentSpecification,
                financialStatementSpecification,
                analysisResultSpecification
        ));
    }

    @Nested
    class analyze {

        Document document = defaultDocument();
        Company company = defaultCompany();
        DateInputData inputData = DateInputData.of(LocalDate.parse("2021-05-15"));

        @DisplayName("analyze : 企業価値を算出する")
        @Test
        void document() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            doReturn(BigDecimal.valueOf(1000)).when(analyzeInteractor).calculateFsValue(document);
            assertDoesNotThrow(() -> analyzeInteractor.analyze(document));
            verify(analysisResultSpecification, times(1)).insert(document, BigDecimal.valueOf(1000));
        }

        @DisplayName("analyze : 分析時にエラーが発生したときの処理を確認する")
        @Test
        void exception() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            doThrow(new FundanalyzerNotExistException()).when(analyzeInteractor).calculateFsValue(document);
            assertDoesNotThrow(() -> analyzeInteractor.analyze(document));
            verify(analysisResultSpecification, times(0)).insert(document, BigDecimal.valueOf(1000));
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

    @Nested
    class calculateFsValue {

        Document document = defaultDocument();

        @DisplayName("calculateFsValue : 各値を取得して計算する")
        @Test
        void present() {
            var financeValue = FinanceValue.of(
                    1000L,
                    1000L,
                    1000L,
                    1000L,
                    10000L,
                    1000L
            );
            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);

            var expected = BigDecimal.valueOf((10000L * 10 + 1000 - (1000 * 1.2) + 1000 - 1000) / 1000);
            var actual = analyzeInteractor.calculateFsValue(document);
            assertEquals(expected, actual);
        }

        @DisplayName("calculateFsValue : 四半期報告書の各値を取得して計算する")
        @Test
        void quarter() {
            var document = new Document(
                    null,
                    null,
                    QuarterType.QT_3,
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
            var financeValue = FinanceValue.of(
                    1000L,
                    1000L,
                    1000L,
                    1000L,
                    10000L,
                    1000L
            );
            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);

            var expected = BigDecimal.valueOf((((10000L * 10 + 1000 - (1000 * 1.2) + 1000 - 1000) / 3) * 4) / 1000);
            var actual = analyzeInteractor.calculateFsValue(document);
            assertEquals(expected, actual);
        }

        @DisplayName("calculateFsValue : 流動資産合計が存在しないとき")
        @Test
        void totalCurrentAssets_isEmpty() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);

            assertThrows(FundanalyzerNotExistException.class, () -> analyzeInteractor.calculateFsValue(document));
            verify(documentSpecification, times(1)).updateFsToHalfWay(document, FinancialStatementEnum.BALANCE_SHEET);
        }

        @DisplayName("calculateFsValue : 投資その他の資産合計が存在しないとき")
        @Test
        void totalInvestmentsAndOtherAssets_isEmpty() {
            var financeValue = FinanceValue.of(
                    1000L,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);

            assertThrows(FundanalyzerNotExistException.class, () -> analyzeInteractor.calculateFsValue(document));
            verify(documentSpecification, times(1)).updateFsToHalfWay(document, FinancialStatementEnum.BALANCE_SHEET);
        }


        @DisplayName("calculateFsValue : 流動負債合計が存在しないとき")
        @Test
        void totalCurrentLiabilities_isEmpty() {
            var financeValue = FinanceValue.of(
                    1000L,
                    1000L,
                    null,
                    null,
                    null,
                    null
            );
            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);

            assertThrows(FundanalyzerNotExistException.class, () -> analyzeInteractor.calculateFsValue(document));
            verify(documentSpecification, times(1)).updateFsToHalfWay(document, FinancialStatementEnum.BALANCE_SHEET);
        }

        @DisplayName("calculateFsValue : 固定負債合計が存在しないとき")
        @Test
        void totalFixedLiabilities_isEmpty() {
            var financeValue = FinanceValue.of(
                    1000L,
                    1000L,
                    1000L,
                    null,
                    null,
                    null
            );
            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);

            assertThrows(FundanalyzerNotExistException.class, () -> analyzeInteractor.calculateFsValue(document));
            verify(documentSpecification, times(1)).updateFsToHalfWay(document, FinancialStatementEnum.BALANCE_SHEET);
        }

        @DisplayName("calculateFsValue : 営業利益が存在しないとき")
        @Test
        void operatingProfit_isEmpty() {
            var financeValue = FinanceValue.of(
                    1000L,
                    1000L,
                    1000L,
                    1000L,
                    null,
                    null
            );
            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);

            assertThrows(FundanalyzerNotExistException.class, () -> analyzeInteractor.calculateFsValue(document));
            verify(documentSpecification, times(1)).updateFsToHalfWay(document, FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT);
        }

        @DisplayName("calculateFsValue : 株式総数が存在しないとき")
        @Test
        void numberOfShares_isEmpty() {
            var financeValue = FinanceValue.of(
                    1000L,
                    1000L,
                    1000L,
                    1000L,
                    1000L,
                    null
            );
            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);

            assertThrows(FundanalyzerNotExistException.class, () -> analyzeInteractor.calculateFsValue(document));
            verify(documentSpecification, times(1)).updateFsToHalfWay(document, FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES);
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