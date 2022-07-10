package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.ValuationDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ValuationSpecificationTest {

    private ValuationDao valuationDao;
    private CompanySpecification companySpecification;
    private StockSpecification stockSpecification;

    private ValuationSpecification valuationSpecification;

    @BeforeEach
    void setUp() {
        valuationDao = Mockito.mock(ValuationDao.class);
        companySpecification = Mockito.mock(CompanySpecification.class);
        stockSpecification = Mockito.mock(StockSpecification.class);

        valuationSpecification = new ValuationSpecification(
                valuationDao,
                companySpecification,
                stockSpecification
        );

        Mockito.when(stockSpecification.getAverageStockPriceOfLatestSubmitDate("code"))
                .thenReturn(Optional.of(BigDecimal.valueOf(600)));

    }

    @Nested
    class findAllValuationView {

        private ValuationEntity valuationEntity(LocalDate targetDate) {
            return new ValuationEntity(
                    null,
                    "code",
                    targetDate,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    (long) 10,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    LocalDate.EPOCH,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    null,
                    null
            );
        }

        @DisplayName("findAllValuationView : ")
        @Test
        void get() {
            Mockito.when(companySpecification.allTargetCompanies()).thenReturn(List.of(new Company(
                    "code",
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
            Mockito.when(valuationDao.selectByCode("code")).thenReturn(List.of(
                    valuationEntity(LocalDate.parse("2022-07-09")),
                    valuationEntity(LocalDate.parse("2022-07-10"))
            ));

            var actual = valuationSpecification.findAllValuationView();

            assertEquals(LocalDate.parse("2022-07-10"), actual.get(0).getTargetDate());
            assertEquals(1, actual.size());
        }
    }

    @Nested
    class evaluate {

        @DisplayName("evaluate : マッピングを確認する")
        @Test
        void mapping() {
            Mockito.when(stockSpecification.getAverageStockPriceOfLatestSubmitDate("code"))
                    .thenReturn(Optional.of(BigDecimal.valueOf(600)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(LocalDate.parse("2022-07-02"), 500.0),
                    new AnalysisResultEntity(
                            null,
                            "code",
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            LocalDate.parse("2022-06-12"),
                            "documentId",
                            null
                    ));

            assertAll(
                    () -> assertEquals("code", actual.getCompanyCode(), "companyCode"),
                    () -> assertEquals(LocalDate.parse("2022-07-02"), actual.getTargetDate(), "targetDate"),
                    () -> assertEquals(BigDecimal.valueOf(500.0), actual.getStockPrice(), "stockPrice"),
                    () -> assertNull(actual.getGoalsStock(), "goalsStock"),
                    () -> assertEquals(20, actual.getDaySinceSubmitDate(), "daySinceSubmitDate"),
                    () -> assertEquals(BigDecimal.valueOf(-100.0), actual.getDifferenceFromSubmitDate(), "differenceFromSubmitDate"),
                    () -> assertEquals(BigDecimal.valueOf(0.83), actual.getSubmitDateRatio(), "submitDateRatio"),
                    () -> assertEquals(BigDecimal.valueOf(1500.0), actual.getDiscountValue(), "discountValue"),
                    () -> assertEquals(BigDecimal.valueOf(400, 2), actual.getDiscountRate(), "discountRate"),
                    () -> assertEquals(LocalDate.parse("2022-06-12"), actual.getSubmitDate(), "submitDate"),
                    () -> assertEquals(BigDecimal.valueOf(2000), actual.getCorporateValue(), "corporateValue"),
                    () -> assertEquals(BigDecimal.valueOf(600), actual.getStockPriceOfSubmitDate(), "stockPriceOfSubmitDate"),
                    () -> assertEquals("documentId", actual.getDocumentId(), "documentId")
            );
        }

        @DisplayName("evaluate : daySinceSubmitDateを確認する")
        @ParameterizedTest
        @CsvSource({
                "2022-07-03, 2022-07-02, 1",
                "2022-07-02, 2022-07-02, 0",
                "2022-07-01, 2022-07-02, -1",
        })
        void daySinceSubmitDate(String targetDate, String submitDate, String day) {
            var actual = valuationSpecification.evaluate(
                    stockPrice(LocalDate.parse(targetDate), 500.0),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            LocalDate.parse(submitDate),
                            null,
                            null
                    )
            );

            assertEquals(Long.valueOf(day), actual.getDaySinceSubmitDate());
        }

        @DisplayName("evaluate : differenceFromSubmitDateを確認する")
        @ParameterizedTest
        @CsvSource({
                "100.0, 200.0, -100",
                "200.0, 200.0, 0",
                "300.0, 200.0, 100",
                "100.0, 100.5, -0.5",
        })
        void differenceFromSubmitDate(Double stockPrice, Double averageStockPrice, Double difference) {
            Mockito.when(stockSpecification.getAverageStockPriceOfLatestSubmitDate("code"))
                    .thenReturn(Optional.of(BigDecimal.valueOf(averageStockPrice)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(LocalDate.parse("2022-07-02"), stockPrice),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            LocalDate.parse("2022-06-12"),
                            null,
                            null
                    )
            );

            assertEquals(BigDecimal.valueOf(difference), actual.getDifferenceFromSubmitDate());
        }

        @DisplayName("evaluate : submitDateRatioを確認する")
        @ParameterizedTest
        @CsvSource({
                "100.0, 200.0, 50",
                "200.0, 200.0, 100",
                "300.0, 200.0, 150",
                "201.0, 120.5, 167",
        })
        void submitDateRatio(Double stockPrice, Double averageStockPrice, Long submitDateRatio) {
            Mockito.when(stockSpecification.getAverageStockPriceOfLatestSubmitDate("code"))
                    .thenReturn(Optional.of(BigDecimal.valueOf(averageStockPrice)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(LocalDate.parse("2022-07-02"), stockPrice),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            LocalDate.parse("2022-06-12"),
                            null,
                            null
                    )
            );

            assertEquals(BigDecimal.valueOf(submitDateRatio, 2), actual.getSubmitDateRatio());
        }

        @DisplayName("evaluate : discountValueを確認する")
        @ParameterizedTest
        @CsvSource({
                "100.0, 200.0, 100.0",
                "200.0, 200.0, 0.0",
                "300.0, 200.0, -100.0",
                "201.0, 120.5, -80.5",
        })
        void discountValue(Double stockPrice, Double latestCorporateValue, Double discountValue) {
            var actual = valuationSpecification.evaluate(
                    stockPrice(LocalDate.parse("2022-07-02"), stockPrice),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(latestCorporateValue),
                            null,
                            null,
                            LocalDate.parse("2022-06-12"),
                            null,
                            null
                    )
            );

            assertEquals(BigDecimal.valueOf(discountValue), actual.getDiscountValue());
        }

        @DisplayName("evaluate : discountRateを確認する")
        @ParameterizedTest
        @CsvSource({
                "100.0, 200.0, 200",
                "200.0, 200.0, 100",
                "300.0, 200.0, 67",
                "201.0, 120.5, 60",
        })
        void discountRate(Double stockPrice, Double latestCorporateValue, Long discountRate) {
            var actual = valuationSpecification.evaluate(
                    stockPrice(LocalDate.parse("2022-07-02"), stockPrice),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(latestCorporateValue),
                            null,
                            null,
                            LocalDate.parse("2022-06-12"),
                            null,
                            null
                    )
            );

            assertEquals(BigDecimal.valueOf(discountRate, 2), actual.getDiscountRate());
        }

        private StockPriceEntity stockPrice(LocalDate targetDate, Double stockPrice) {
            return new StockPriceEntity(
                    null,
                    "code",
                    targetDate,
                    stockPrice,
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
}
