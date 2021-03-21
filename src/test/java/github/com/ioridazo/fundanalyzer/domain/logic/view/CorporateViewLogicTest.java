package github.com.ioridazo.fundanalyzer.domain.logic.view;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.MinkabuDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Minkabu;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class CorporateViewLogicTest {

    private DocumentDao documentDao;
    private AnalysisResultDao analysisResultDao;
    private StockPriceDao stockPriceDao;
    private MinkabuDao minkabuDao;

    private CorporateViewLogic logic;

    @BeforeEach
    void setUp() {
        documentDao = Mockito.mock(DocumentDao.class);
        analysisResultDao = Mockito.mock(AnalysisResultDao.class);
        stockPriceDao = Mockito.mock(StockPriceDao.class);
        minkabuDao = Mockito.mock(MinkabuDao.class);

        logic = Mockito.spy(new CorporateViewLogic(
                documentDao,
                analysisResultDao,
                stockPriceDao,
                minkabuDao
        ));
    }

    @DisplayName("corporateViewOf : CorporateViewBeanを生成する")
    @Test
    void corporateViewOf_ok() {
        var company = new Company(
                "code",
                "会社名",
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        var submitDate = LocalDate.parse("2019-10-11");
        var createdAt = LocalDateTime.of(2020, 11, 8, 18, 15);

        doReturn(Optional.of(submitDate)).when(logic).latestSubmitDate(company);
        doReturn(CorporateViewLogic.CorporateValue.of(
                BigDecimal.valueOf(2100), BigDecimal.valueOf(2000), BigDecimal.valueOf(10), BigDecimal.valueOf(0.1), BigDecimal.valueOf(3)
        )).when(logic).corporateValue(company);
        doReturn(CorporateViewLogic.StockPriceValue.of(
                BigDecimal.valueOf(900), LocalDate.parse("2020-10-11"), BigDecimal.valueOf(1000))
        ).when(logic).stockPrice(company, submitDate);
        doReturn(Pair.of(Optional.of(BigDecimal.valueOf(1000)), Optional.of(BigDecimal.valueOf(200))))
                .when(logic).discountValue(any(), any());
        doReturn(Optional.of(BigDecimal.valueOf(2000))).when(logic).forecastStock(company);
        when(logic.nowLocalDateTime()).thenReturn(createdAt);

        var actual = logic.corporateViewOf(company);

        assertAll("CorporateViewBean",
                () -> assertEquals("code", actual.getCode()),
                () -> assertEquals("会社名", actual.getName()),
                () -> assertEquals(LocalDate.parse("2019-10-11"), actual.getSubmitDate()),
                () -> assertEquals(BigDecimal.valueOf(2100), actual.getLatestCorporateValue()),
                () -> assertEquals(BigDecimal.valueOf(2000), actual.getAverageCorporateValue()),
                () -> assertEquals(BigDecimal.valueOf(10), actual.getStandardDeviation()),
                () -> assertEquals(BigDecimal.valueOf(0.1), actual.getCoefficientOfVariation()),
                () -> assertEquals(BigDecimal.valueOf(900), actual.getAverageStockPrice()),
                () -> assertEquals(LocalDate.parse("2020-10-11"), actual.getImportDate()),
                () -> assertEquals(BigDecimal.valueOf(1000), actual.getLatestStockPrice()),
                () -> assertEquals(BigDecimal.valueOf(1000), actual.getDiscountValue()),
                () -> assertEquals(BigDecimal.valueOf(200), actual.getDiscountRate()),
                () -> assertEquals(BigDecimal.valueOf(3), actual.getCountYear()),
                () -> assertEquals(BigDecimal.valueOf(2000), actual.getForecastStock()),
                () -> assertEquals(LocalDateTime.of(2020, 11, 8, 18, 15), actual.getCreatedAt()),
                () -> assertEquals(LocalDateTime.of(2020, 11, 8, 18, 15), actual.getUpdatedAt())
        );
    }

    @Nested
    class latestSubmitDate {
        @DisplayName("latestSubmitDate : 対象の書類が存在したときの処理を確認する")
        @Test
        void latestSubmitDate_ok() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var document = Document.builder()
                    .edinetCode("edinetCode")
                    .submitDate(LocalDate.parse("2020-10-11"))
                    .build();

            when(documentDao.selectByDocumentTypeCode("120")).thenReturn(List.of(document));

            assertEquals(LocalDate.parse("2020-10-11"), logic.latestSubmitDate(company).orElseThrow());
        }

        @DisplayName("latestSubmitDate : 対象の書類が存在しないときの処理を確認する")
        @Test
        void latestSubmitDate_nothing() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var document = Document.builder()
                    .edinetCode("edinetCode2")
                    .submitDate(LocalDate.parse("2020-10-11"))
                    .build();

            when(documentDao.selectByDocumentTypeCode("120")).thenReturn(List.of(document));

            assertNull(logic.latestSubmitDate(company).orElse(null));
        }
    }

    @Nested
    class corporateValue {

        @DisplayName("corporateValue : 企業価値等が算出されることを確認する")
        @Test
        void corporateValue_ok() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var analysisResult1 = new AnalysisResult(1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(1100), "120", null, null, null);
            var analysisResult2 = new AnalysisResult(2, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(900), "120", null, null, null);

            when(analysisResultDao.selectByCompanyCode("code")).thenReturn(List.of(analysisResult1, analysisResult2));

            var actual = logic.corporateValue(company);

            assertAll("CorporateValue",
                    () -> assertEquals(BigDecimal.valueOf(110000, 2), actual.getLatestCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(100000, 2), actual.getAverageCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(100.0), actual.getStandardDeviation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(100, 3), actual.getCoefficientOfVariation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(2), actual.getCountYear().orElseThrow())
            );
        }

        @DisplayName("corporateValue : 企業価値に関する値が存在しないときの処理を確認する")
        @Test
        void corporateValue_nothing() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            when(analysisResultDao.selectByCompanyCode("code")).thenReturn(List.of());

            var actual = logic.corporateValue(company);

            assertAll("CorporateValue",
                    () -> assertNull(actual.getLatestCorporateValue().orElse(null)),
                    () -> assertNull(actual.getAverageCorporateValue().orElse(null)),
                    () -> assertNull(actual.getStandardDeviation().orElse(null)),
                    () -> assertNull(actual.getCountYear().orElse(null))
            );
        }

        @DisplayName("corporateValue : 企業価値を算出するロジックを確認する")
        @Test
        void corporateValue_scale() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var analysisResult1 = new AnalysisResult(
                    1,
                    "code",
                    LocalDate.parse("2020-06-30"),
                    BigDecimal.valueOf(500.250515),
                    DocTypeCode.ANNUAL_SECURITIES_REPORT.toValue(),
                    LocalDate.parse("2020-09-30"),
                    null,
                    null
            );

            when(analysisResultDao.selectByCompanyCode("code")).thenReturn(List.of(analysisResult1));

            var actual = logic.corporateValue(company);

            assertAll("CorporateValue",
                    () -> assertEquals(BigDecimal.valueOf(500.25), actual.getLatestCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(500.25), actual.getAverageCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(0, 1), actual.getStandardDeviation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(0, 3), actual.getCoefficientOfVariation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(1), actual.getCountYear().orElseThrow())
            );
        }

        @DisplayName("corporateValue : 企業価値を算出するロジックを確認する")
        @Test
        void corporateValue_sd() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var analysisResult1 = new AnalysisResult(1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(500.250515), "120", null, null, null);
            var analysisResult2 = new AnalysisResult(2, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(418.02101), "120", null, null, null);

            when(analysisResultDao.selectByCompanyCode("code")).thenReturn(List.of(analysisResult1, analysisResult2));

            var actual = logic.corporateValue(company);

            assertAll("CorporateValue",
                    () -> assertEquals(BigDecimal.valueOf(500.25), actual.getLatestCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(459.14), actual.getAverageCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(41.115), actual.getStandardDeviation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(90, 3), actual.getCoefficientOfVariation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(2), actual.getCountYear().orElseThrow())
            );
        }

        @DisplayName("corporateValue : 最新企業価値が複数存在するときに訂正版を取得する")
        @Test
        void corporateValue_latest_multiple() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var analysisResult1 = new AnalysisResult(
                    1,
                    "code",
                    LocalDate.parse("2021-03-31"),
                    BigDecimal.valueOf(1100),
                    DocTypeCode.ANNUAL_SECURITIES_REPORT.toValue(),
                    LocalDate.parse("2021-05-01"),
                    null,
                    null
            );
            var analysisResult2 = new AnalysisResult(
                    2,
                    "code",
                    LocalDate.parse("2021-03-31"),
                    BigDecimal.valueOf(900),
                    DocTypeCode.AMENDED_SECURITIES_REPORT.toValue(),
                    LocalDate.parse("2021-06-01"),
                    null,
                    null
            );

            when(analysisResultDao.selectByCompanyCode("code")).thenReturn(List.of(analysisResult1, analysisResult2));

            var actual = logic.corporateValue(company);

            assertEquals(BigDecimal.valueOf(90000, 2), actual.getLatestCorporateValue().orElseThrow());
        }

        @DisplayName("corporateValue : 企業価値が重複して存在するときに訂正版を基に算出する")
        @Test
        void corporateValue_average_multiple() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var analysisResult1 = new AnalysisResult(
                    1,
                    "code",
                    LocalDate.parse("2020-06-30"),
                    BigDecimal.valueOf(1000),
                    DocTypeCode.ANNUAL_SECURITIES_REPORT.toValue(),
                    LocalDate.parse("2020-09-30"),
                    null,
                    null
            );
            var analysisResult2 = new AnalysisResult(
                    2,
                    "code",
                    LocalDate.parse("2020-06-30"),
                    BigDecimal.valueOf(1100),
                    DocTypeCode.AMENDED_SECURITIES_REPORT.toValue(),
                    LocalDate.parse("2020-11-30"),
                    null,
                    null
            );
            var analysisResult3 = new AnalysisResult(
                    3,
                    "code",
                    LocalDate.parse("2019-06-30"),
                    BigDecimal.valueOf(900),
                    DocTypeCode.ANNUAL_SECURITIES_REPORT.toValue(),
                    LocalDate.parse("2019-09-30"),
                    null,
                    null
            );

            when(analysisResultDao.selectByCompanyCode("code")).thenReturn(List.of(analysisResult1, analysisResult2, analysisResult3));

            var actual = logic.corporateValue(company);

            assertAll("CorporateValue",
                    () -> assertEquals(BigDecimal.valueOf(110000, 2), actual.getLatestCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(100000, 2), actual.getAverageCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(100.0), actual.getStandardDeviation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(100, 3), actual.getCoefficientOfVariation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(2), actual.getCountYear().orElseThrow())
            );
        }

        @DisplayName("corporateValue : 企業価値が重複して存在するときに訂正版を基に算出する")
        @Test
        void corporateValue_sd_multiple() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var analysisResult1 = new AnalysisResult(
                    1,
                    "code",
                    LocalDate.parse("2020-06-30"),
                    BigDecimal.valueOf(1500.250515),
                    DocTypeCode.ANNUAL_SECURITIES_REPORT.toValue(),
                    LocalDate.parse("2020-09-30"),
                    null,
                    null
            );
            var analysisResult2 = new AnalysisResult(
                    2,
                    "code",
                    LocalDate.parse("2020-06-30"),
                    BigDecimal.valueOf(500.250515),
                    DocTypeCode.AMENDED_SECURITIES_REPORT.toValue(),
                    LocalDate.parse("2020-11-30"),
                    null,
                    null
            );
            var analysisResult3 = new AnalysisResult(
                    3,
                    "code",
                    LocalDate.parse("2019-06-30"),
                    BigDecimal.valueOf(418.02101),
                    DocTypeCode.ANNUAL_SECURITIES_REPORT.toValue(),
                    LocalDate.parse("2019-09-30"),
                    null,
                    null
            );

            when(analysisResultDao.selectByCompanyCode("code")).thenReturn(List.of(analysisResult1, analysisResult2, analysisResult3));

            var actual = logic.corporateValue(company);

            assertAll("CorporateValue",
                    () -> assertEquals(BigDecimal.valueOf(500.25), actual.getLatestCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(459.14), actual.getAverageCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(41.115), actual.getStandardDeviation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(90, 3), actual.getCoefficientOfVariation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(2), actual.getCountYear().orElseThrow())
            );
        }
    }

    @Nested
    class stockPrice {

        @DisplayName("stockPrice : 株価取得できることを確認する")
        @Test
        void stockPrice_ok() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var submitDate = LocalDate.parse("2020-10-08");
            var stockPrice1 = new StockPrice(
                    null,
                    null,
                    LocalDate.parse("2020-10-07"),
                    (double) 700,
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
            var stockPrice2 = new StockPrice(
                    null,
                    null,
                    LocalDate.parse("2020-10-08"),
                    (double) 800,
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
            var stockPrice3 = new StockPrice(
                    null,
                    null,
                    LocalDate.parse("2020-10-09"),
                    (double) 900,
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
            var latestStockPrice = new StockPrice(
                    null,
                    null,
                    LocalDate.parse("2020-10-10"),
                    (double) 1000,
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

            when(stockPriceDao.selectByCode("code")).thenReturn(List.of(stockPrice1, stockPrice2, stockPrice3, latestStockPrice));

            var actual = logic.stockPrice(company, submitDate);

            assertAll("StockPriceValue",
                    () -> assertEquals(BigDecimal.valueOf(75000, 2), actual.getAverageStockPrice().orElseThrow()),
                    () -> assertEquals(LocalDate.parse("2020-10-10"), actual.getImportDate().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(1000.0), actual.getLatestStockPrice().orElseThrow())
            );
        }


        @DisplayName("stockPrice : 株価取得できないときの処理を確認する")
        @Test
        void stockPrice_nothing() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var submitDate = LocalDate.parse("2020-10-08");

            when(stockPriceDao.selectByCode("code")).thenReturn(List.of());

            var actual = logic.stockPrice(company, submitDate);

            assertAll("StockPriceValue",
                    () -> assertNull(actual.getAverageStockPrice().orElse(null)),
                    () -> assertNull(actual.getImportDate().orElse(null)),
                    () -> assertNull(actual.getLatestStockPrice().orElse(null))
            );
        }
    }

    @Nested
    class discountValue {

        @DisplayName("discountValue : 割安値が算出されることを確認する")
        @Test
        void discountValue_ok() {
            var corporateValue = BigDecimal.valueOf(2132.0512495);
            var latestStockPrice = BigDecimal.valueOf(1000.0);

            var actual = logic.discountValue(corporateValue, latestStockPrice);

            assertEquals(BigDecimal.valueOf(1132.05), actual.getFirst().orElseThrow());
            assertEquals(BigDecimal.valueOf(213.205), actual.getSecond().orElseThrow());
        }

        @DisplayName("discountValue : 割安値を算出しないときの処理を確認する")
        @Test
        void discountValue_nothing() {
            var corporateValue = nullable(BigDecimal.class);
            var latestStockPrice = nullable(BigDecimal.class);

            var actual = logic.discountValue(corporateValue, latestStockPrice);

            assertNull(actual.getFirst().orElse(null));
            assertNull(actual.getSecond().orElse(null));
        }
    }

    @Nested
    class forecastStock {

        @DisplayName("forecastStock : 最新のみんかぶ株価予想を取得する")
        @Test
        void forecastStock_ok() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            when(minkabuDao.selectByCode("code")).thenReturn(List.of(
                    new Minkabu(
                            null,
                            "code",
                            LocalDate.parse("2020-12-12"),
                            null,
                            2000.0,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Minkabu(
                            null,
                            "code",
                            LocalDate.parse("2020-10-12"),
                            null,
                            1500.0,
                            null,
                            null,
                            null,
                            null
                    )));

            var actual = logic.forecastStock(company);

            assertEquals(BigDecimal.valueOf(2000), actual.orElseThrow());
        }

        @DisplayName("forecastStock : みんかぶ株価予想が存在しないときはnullを返却する")
        @Test
        void forecastStock_null() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            when(minkabuDao.selectByCode("code")).thenReturn(List.of());

            var actual = logic.forecastStock(company);

            assertNull(actual.orElse(null));
        }
    }
}