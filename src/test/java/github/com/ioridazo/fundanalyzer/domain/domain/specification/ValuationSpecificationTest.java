package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.ValuationDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.InvestmentIndicatorEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValuationSpecificationTest {

    private ValuationDao valuationDao;
    private CompanySpecification companySpecification;
    private StockSpecification stockSpecification;
    private InvestmentIndicatorSpecification investmentIndicatorSpecification;

    private ValuationSpecification valuationSpecification;

    @BeforeEach
    void setUp() {
        valuationDao = mock(ValuationDao.class);
        companySpecification = mock(CompanySpecification.class);
        stockSpecification = mock(StockSpecification.class);
        investmentIndicatorSpecification = mock(InvestmentIndicatorSpecification.class);

        valuationSpecification = new ValuationSpecification(
                valuationDao,
                companySpecification,
                stockSpecification,
                investmentIndicatorSpecification
        );
    }

    @Nested
    class averageValuation {

        private Company company(String code) {
            return new Company(
                    code,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    true
            );
        }

        private ValuationEntity valuationEntity(
                LocalDate targetDate, BigDecimal differenceFromSubmitDate, BigDecimal submitDateRatio, BigDecimal grahamIndex) {
            return new ValuationEntity(
                    null,
                    "code",
                    null,
                    targetDate,
                    null,
                    null,
                    null,
                    grahamIndex,
                    null,
                    differenceFromSubmitDate,
                    submitDateRatio,
                    null,
                    null,
                    null
            );
        }

        @DisplayName("averageValuation : 業種による平均の評価結果を取得する")
        @Test
        void average() {
            when(valuationDao.selectByCode("code1")).thenReturn(List.of(
                    valuationEntity(LocalDate.parse("2022-07-09"), BigDecimal.valueOf(100), BigDecimal.valueOf(1.1), null),
                    valuationEntity(LocalDate.parse("2022-07-10"), BigDecimal.valueOf(-20), BigDecimal.valueOf(1.01), BigDecimal.valueOf(2.05))
            ));
            when(valuationDao.selectByCode("code2")).thenReturn(List.of(
                    valuationEntity(LocalDate.parse("2022-07-09"), BigDecimal.valueOf(-20), BigDecimal.valueOf(1.01), BigDecimal.valueOf(2.05)),
                    valuationEntity(LocalDate.parse("2022-07-10"), BigDecimal.valueOf(100), BigDecimal.valueOf(1.1), null)
            ));

            var actual = valuationSpecification.averageValuation("name", List.of(company("code1"), company("code2")));
            assertAll(
                    () -> assertEquals("name", actual.getName()),
                    () -> assertEquals(BigDecimal.valueOf(4000, 2), actual.getDifferenceFromSubmitDate()),
                    () -> assertEquals(BigDecimal.valueOf(106, 2), actual.getSubmitDateRatio()),
                    () -> assertEquals(BigDecimal.valueOf(205, 2), actual.getGrahamIndex()),
                    () -> assertEquals(2, actual.getCount())
            );
        }
    }

    @Nested
    class findAllValuationView {

        private ValuationEntity valuationEntity(LocalDate targetDate, LocalDate submitDate) {
            return new ValuationEntity(
                    null,
                    "code",
                    submitDate,
                    targetDate,
                    null,
                    BigDecimal.TEN,
                    null,
                    BigDecimal.TEN,
                    (long) 10,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    null
            );
        }

        @DisplayName("findAllValuationView : すべての評価結果を取得する")
        @Test
        void targetDate() {
            when(companySpecification.inquiryAllTargetCompanies()).thenReturn(List.of(new Company(
                    "code",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    true
            )));
            when(valuationDao.selectByCode("code")).thenReturn(List.of(
                    valuationEntity(LocalDate.parse("2022-07-09"), LocalDate.parse("2022-07-09")),
                    valuationEntity(LocalDate.parse("2022-07-10"), LocalDate.parse("2022-07-09"))
            ));

            var actual = valuationSpecification.findAllValuationView();

            assertEquals(LocalDate.parse("2022-07-10"), actual.get(0).getTargetDate());
            assertEquals(1, actual.size());
        }

        @DisplayName("findAllValuationView : すべての評価結果を取得する")
        @Test
        void submitDate() {
            when(companySpecification.inquiryAllTargetCompanies()).thenReturn(List.of(new Company(
                    "code",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    true
            )));
            when(valuationDao.selectByCode("code")).thenReturn(List.of(
                    valuationEntity(LocalDate.parse("2022-08-11"), LocalDate.parse("2021-08-11")),
                    valuationEntity(LocalDate.parse("2022-08-11"), LocalDate.parse("2022-08-11"))
            ));

            var actual = valuationSpecification.findAllValuationView();

            assertAll(
                    () -> assertEquals(LocalDate.parse("2022-08-11"), actual.get(0).getTargetDate()),
                    () -> assertEquals(LocalDate.parse("2022-08-11"), actual.get(0).getSubmitDate())
            );

            assertEquals(1, actual.size());
        }
    }

    @Nested
    class findValuationView {

        private ValuationEntity valuationEntity(LocalDate targetDate, LocalDate submitDate) {
            return new ValuationEntity(
                    null,
                    "code",
                    submitDate,
                    targetDate,
                    null,
                    BigDecimal.TEN,
                    null,
                    BigDecimal.TEN,
                    (long) 10,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    null
            );
        }

        @DisplayName("findAllValuationView : 企業ごとの評価結果を取得する")
        @Test
        void get() {
            when(companySpecification.findCompanyByCode("code")).thenReturn(Optional.of(new Company(
                    "code",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    true
            )));
            when(valuationDao.selectByCode("code")).thenReturn(List.of(
                    valuationEntity(LocalDate.parse("2022-07-09"), LocalDate.parse("2022-07-01")),
                    valuationEntity(LocalDate.parse("2022-07-10"), LocalDate.parse("2022-07-01")),
                    valuationEntity(LocalDate.parse("2022-08-11"), LocalDate.parse("2021-08-11")),
                    valuationEntity(LocalDate.parse("2022-08-11"), LocalDate.parse("2022-08-11"))
            ));

            var actual = valuationSpecification.findValuationView("code");

            assertAll(
                    () -> assertEquals(LocalDate.parse("2022-07-09"), actual.get(0).getTargetDate()),
                    () -> assertEquals(LocalDate.parse("2022-07-01"), actual.get(0).getSubmitDate()),
                    () -> assertEquals(LocalDate.parse("2022-07-10"), actual.get(1).getTargetDate()),
                    () -> assertEquals(LocalDate.parse("2022-07-01"), actual.get(1).getSubmitDate()),
                    () -> assertEquals(LocalDate.parse("2022-08-11"), actual.get(2).getTargetDate()),
                    () -> assertEquals(LocalDate.parse("2022-08-11"), actual.get(2).getSubmitDate())
            );
            assertEquals(3, actual.size());
        }
    }

    @Nested
    class evaluate {

        private final String companyCode = "code";
        private final LocalDate submitDate = LocalDate.parse("2022-06-12");
        private final LocalDate targetDate = LocalDate.parse("2022-07-02");

        @BeforeEach
        void setUp() {
            when(investmentIndicatorSpecification.findEntity(companyCode, targetDate))
                    .thenReturn(Optional.of(new InvestmentIndicatorEntity(
                            2,
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
                    )));
        }

        @DisplayName("evaluate : マッピングを確認する")
        @Test
        void mapping() {
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, 600.0)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, 500.0),
                    new AnalysisResultEntity(
                            4,
                            companyCode,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            submitDate,
                            "documentId",
                            null
                    ));

            assertAll(
                    () -> assertEquals("code", actual.getCompanyCode(), "companyCode"),
                    () -> assertEquals(LocalDate.parse("2022-06-12"), actual.getSubmitDate(), "submitDate"),
                    () -> assertEquals(LocalDate.parse("2022-07-02"), actual.getTargetDate(), "targetDate"),
                    () -> assertEquals(1, actual.getStockPriceId().orElse(null), "stockPriceId"),
                    () -> assertEquals(BigDecimal.valueOf(500.0), actual.getStockPrice(), "stockPrice"),
                    () -> assertEquals(2, actual.getInvestmentIndicatorId().orElse(null), "investmentIndicatorId"),
                    () -> assertNull(actual.getGrahamIndex().orElse(null), "grahamIndex"),
                    () -> assertEquals(20, actual.getDaySinceSubmitDate(), "daySinceSubmitDate"),
                    () -> assertEquals(BigDecimal.valueOf(-100.0), actual.getDifferenceFromSubmitDate(), "differenceFromSubmitDate"),
                    () -> assertEquals(BigDecimal.valueOf(0.83), actual.getSubmitDateRatio(), "submitDateRatio"),
                    () -> assertEquals(BigDecimal.valueOf(1500.0), actual.getDiscountValue(), "discountValue"),
                    () -> assertEquals(BigDecimal.valueOf(400, 2), actual.getDiscountRate(), "discountRate")
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
            when(stockSpecification.findStock(companyCode, LocalDate.parse(submitDate)))
                    .thenReturn(Optional.of(stockPrice(LocalDate.parse(submitDate), 600.0)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(LocalDate.parse(targetDate), 500.0),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            null,
                            null,
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
        void differenceFromSubmitDate(Double stockPrice, Double stockPriceOfSubmitDate, Double difference) {
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, stockPriceOfSubmitDate)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, stockPrice),
                    new AnalysisResultEntity(
                            null,
                            companyCode,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            null,
                            null,
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
        void submitDateRatio(Double stockPrice, Double stockPriceOfSubmitDate, Long submitDateRatio) {
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, stockPriceOfSubmitDate)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, stockPrice),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            null,
                            null,
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
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, 600.0)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, stockPrice),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(latestCorporateValue),
                            null,
                            null,
                            null,
                            null,
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
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, 600.0)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, stockPrice),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(latestCorporateValue),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            LocalDate.parse("2022-06-12"),
                            null,
                            null
                    )
            );

            assertEquals(BigDecimal.valueOf(discountRate, 2), actual.getDiscountRate());
        }

        //TODO
        @Disabled
        @DisplayName("evaluate : 予想配当利回りを数値に変換できないとき")
        @Test
        void dividendYield() {
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, 600.0)));

            Executable executable = () -> valuationSpecification.evaluate(
                    new StockPriceEntity(
                            1,
                            "code",
                            targetDate,
                            600.0,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "(4.01%)",
                            null,
                            null,
                            null
                    ),
                    new AnalysisResultEntity(
                            4,
                            companyCode,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            submitDate,
                            "documentId",
                            null
                    ));
            assertThrows(NumberFormatException.class, executable);
        }

        private StockPriceEntity stockPrice(LocalDate targetDate, Double stockPrice) {
            return new StockPriceEntity(
                    1,
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
                    "4.01%",
                    null,
                    null,
                    null
            );
        }
    }
}
