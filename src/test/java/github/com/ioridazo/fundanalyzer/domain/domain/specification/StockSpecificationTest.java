package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.jsoup.result.MinkabuResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.NikkeiResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.StockPriceResultBean;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.MinkabuDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.MinkabuEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SourceOfStockPrice;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerAlreadyExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerScrapingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockSpecificationTest {

    private StockPriceDao stockPriceDao;
    private MinkabuDao minkabuDao;
    private CompanySpecification companySpecification;
    private DocumentSpecification documentSpecification;

    private StockSpecification stockSpecification;

    @BeforeEach
    void setUp() {
        stockPriceDao = Mockito.mock(StockPriceDao.class);
        minkabuDao = Mockito.mock(MinkabuDao.class);
        companySpecification = Mockito.mock(CompanySpecification.class);
        documentSpecification = Mockito.mock(DocumentSpecification.class);

        stockSpecification = Mockito.spy(new StockSpecification(
                stockPriceDao,
                minkabuDao,
                companySpecification,
                documentSpecification
        ));
        stockSpecification.daysToAverageStockPrice = 30;
        stockSpecification.targetCompanyNumber = 1;
        stockSpecification.daysToStoreStockPrice = 365;
    }

    @Nested
    class findStock {

        private final Company company = company();

        @BeforeEach
        void setUp() {
            var stockPrice1 = new StockPriceEntity(
                    null,
                    null,
                    LocalDate.parse("2020-10-06"),
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
                    null,
                    null
            );
            var stockPrice2 = new StockPriceEntity(
                    null,
                    null,
                    LocalDate.parse("2020-10-07"),
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
                    null,
                    null
            );
            var stockPrice3 = new StockPriceEntity(
                    null,
                    null,
                    LocalDate.parse("2020-10-08"),
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
                    null,
                    null
            );
            var latestStockPrice = new StockPriceEntity(
                    null,
                    null,
                    LocalDate.parse("2020-10-09"),
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
                    null,
                    null
            );

            when(stockPriceDao.selectByCode("code")).thenReturn(List.of(stockPrice1, stockPrice2, stockPrice3, latestStockPrice));
        }

        @DisplayName("importDate : 株価取得日を取得する")
        @Test
        void importDate() {
            when(documentSpecification.findLatestDocument(company)).thenReturn(Optional.of(new Document(
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDate.parse("2020-10-08"),
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

            var actual = stockSpecification.findStock(company);

            assertEquals(LocalDate.parse("2020-10-09"), actual.getImportDate().orElseThrow());

        }

        @DisplayName("latestStock : 株価取得日を取得する")
        @Test
        void latestStock() {
            when(documentSpecification.findLatestDocument(company)).thenReturn(Optional.of(new Document(
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDate.parse("2020-10-08"),
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

            var actual = stockSpecification.findStock(company);

            assertEquals(BigDecimal.valueOf(1000.0), actual.getLatestStockPrice().orElseThrow());

        }

        @DisplayName("averageStockPrice : 特定期間における平均の株価を取得する")
        @Test
        void averageStockPrice() {
            when(documentSpecification.findLatestDocument(company)).thenReturn(Optional.of(new Document(
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDate.parse("2020-10-08"),
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

            var actual = stockSpecification.findStock(company);

            assertEquals(BigDecimal.valueOf(75000, 2), actual.getAverageStockPrice().orElseThrow());
        }

        @DisplayName("averageStockPrice : 特定期間がないときは空で返す")
        @Test
        void averageStockPrice_isNull() {
            when(documentSpecification.findLatestDocument(company)).thenReturn(Optional.of(new Document(
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDate.parse("2021-05-08"),
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

            var actual = stockSpecification.findStock(company);

            assertNull(actual.getAverageStockPrice().orElse(null));
        }
    }

    @Nested
    class findTargetCodeForStockScheduler {

        @DisplayName("findTargetCodeForStockScheduler : 最新登録日が古い会社コードを取得する")
        @Test
        void getCode() {
            when(companySpecification.inquiryAllTargetCompanies()).thenReturn(List.of(
                    new Company(
                            "code1",
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
                    ),
                    new Company(
                            "code2",
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
            when(stockPriceDao.selectByCode("code1")).thenReturn(List.of(
                    new StockPriceEntity(
                            null,
                            "code1",
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
                            LocalDateTime.of(2021, 4, 29, 0, 0),
                            LocalDateTime.of(2021, 4, 29, 0, 0)
                    ),
                    new StockPriceEntity(
                            null,
                            "code1",
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
                            LocalDateTime.of(2022, 4, 29, 0, 0),
                            LocalDateTime.of(2022, 4, 29, 0, 0)
                    )));
            when(stockPriceDao.selectByCode("code2")).thenReturn(List.of(
                    new StockPriceEntity(
                            null,
                            "code2",
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
                            LocalDateTime.of(2022, 4, 30, 0, 0),
                            LocalDateTime.of(2022, 4, 30, 0, 0)
                    )));

            var actual = stockSpecification.findTargetCodeForStockScheduler();

            assertEquals("code1", actual.get(0));
            assertEquals(1, actual.size());
        }
    }

    @Nested
    class findTargetDateToDelete {

        @BeforeEach
        void setUp() {
            doReturn(LocalDate.parse("2021-06-06")).when(stockSpecification).nowLocalDate();
        }

        @DisplayName("findTargetDateToDelete : 削除対象の日付を取得する")
        @Test
        void targetDate() {
            when(stockPriceDao.selectDistinctTargetDate()).thenReturn(List.of(
                    LocalDate.parse("2019-06-06"),
                    LocalDate.parse("2020-06-06"),
                    LocalDate.parse("2021-06-06")
            ));

            var actual = stockSpecification.findTargetDateToDelete();
            assertEquals(LocalDate.parse("2019-06-06"), actual.get(0));
            assertEquals(1, actual.size());
        }
    }

    @Nested
    class insert_stock {

        LocalDateTime nowLocalDateTime = LocalDateTime.of(2023, 3, 26, 11, 0);

        @BeforeEach
        void setUp() {
            when(stockSpecification.nowLocalDateTime()).thenReturn(nowLocalDateTime);
        }

        @DisplayName("insert : 日経から取得した株価情報を登録する")
        @Test
        void nikkei_ok() {
            var code = "code";
            var nikkei = new NikkeiResultBean(
                    "100.0",
                    "2023/3/26",
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

            assertDoesNotThrow(() -> stockSpecification.insert(code, nikkei));
            verify(stockPriceDao, times(1)).insert(any());
        }

        @DisplayName("insert : 日経から取得した株価情報に対象日がないときはエラーにする")
        @Test
        void nikkei_targetDate_isNull() {
            var code = "code";
            var nikkei = new NikkeiResultBean(
                    "100.0",
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

            assertThrows(FundanalyzerScrapingException.class, () -> stockSpecification.insert(code, nikkei));
        }

        @DisplayName("insert : 日経から取得した株価情報に株価終値が存在しないときはDBから取得する")
        @Test
        void nikkei_stockPrice_isNull() {
            var code = "code";
            var nikkei = new NikkeiResultBean(
                    "-- 円",
                    "2023/3/26",
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

            when(stockPriceDao.selectByCodeAndDate(code, LocalDate.parse("2023-03-26"))).thenReturn(List.of(stockPriceEntity()));
            assertDoesNotThrow(() -> stockSpecification.insert(code, nikkei));
            verify(stockPriceDao, times(1)).insert(any());
        }

        @DisplayName("insert : 日経から取得した株価情報に株価終値が存在しないかつDBにも存在しないときはエラーにする")
        @Test
        void nikkei_stockPrice_isNull2() {
            var code = "code";
            var nikkei = new NikkeiResultBean(
                    null,
                    "2023/3/26",
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

            assertThrows(FundanalyzerScrapingException.class, () -> stockSpecification.insert(code, nikkei));
        }

        @DisplayName("insert : 日経から取得した株価情報がすでに存在するときはエラーにする")
        @Test
        void nikkei_isPresent() {
            var code = "code";
            var nikkei = new NikkeiResultBean(
                    "100.0",
                    "2023/3/26",
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

            when(stockPriceDao.selectByUniqueKey(code, LocalDate.parse("2023-03-26"), "1")).thenReturn(Optional.of(stockPriceEntity()));
            assertThrows(FundanalyzerAlreadyExistException.class, () -> stockSpecification.insert(code, nikkei));
        }

        @DisplayName("insert : 取得した株価情報を登録する")
        @ParameterizedTest
        @EnumSource(SourceOfStockPrice.class)
        void insert(SourceOfStockPrice place) {
            var code = "code";
            var targetDate = switch (place) {
                case KABUOJI3 -> "2023-03-26";
                case YAHOO_FINANCE -> "2023年3月26日";
                case MINKABU -> "2023/03/26";
                default -> "";
            };
            var resultBean = new StockPriceResultBean(
                    targetDate,
                    null,
                    null,
                    null,
                    "100.0",
                    null,
                    null
            );

            switch (place) {
                case NIKKEI ->
                        assertThrows(FundanalyzerRuntimeException.class, () -> stockSpecification.insert(code, resultBean, place));
                case KABUOJI3, MINKABU, YAHOO_FINANCE -> {
                    assertDoesNotThrow(() -> stockSpecification.insert(code, resultBean, place));
                    verify(stockPriceDao, times(1)).insert(any());
                }
            }
        }

        @DisplayName("insert : 取得した株価情報に対象日がないときはエラーにする")
        @ParameterizedTest
        @EnumSource(SourceOfStockPrice.class)
        void insert_targetDate_isNull(SourceOfStockPrice place) {
            var code = "code";
            var resultBean = new StockPriceResultBean(
                    null,
                    null,
                    null,
                    null,
                    "100.0",
                    null,
                    null
            );

            assertThrows(FundanalyzerScrapingException.class, () -> stockSpecification.insert(code, resultBean, place));
        }

        @DisplayName("insert : 取得した株価情報に株価終値がないときはエラーにする")
        @ParameterizedTest
        @EnumSource(SourceOfStockPrice.class)
        void insert_stockPrice_isNull(SourceOfStockPrice place) {
            var code = "code";
            var resultBean = new StockPriceResultBean(
                    "targetDate",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            assertThrows(FundanalyzerScrapingException.class, () -> stockSpecification.insert(code, resultBean, place));
        }

        @DisplayName("insert : 取得した株価情報に株価終値がないときはエラーにする")
        @ParameterizedTest
        @EnumSource(SourceOfStockPrice.class)
        void insert_isPresent(SourceOfStockPrice place) {
            var code = "code";
            var targetDate = switch (place) {
                case KABUOJI3 -> "2023-03-26";
                case YAHOO_FINANCE -> "2023年3月26日";
                case MINKABU -> "2023/03/26";
                default -> "";
            };
            var resultBean = new StockPriceResultBean(
                    targetDate,
                    null,
                    null,
                    null,
                    "100.0",
                    null,
                    null
            );

            when(stockPriceDao.selectByCodeAndDate(code, LocalDate.parse("2023-03-26"))).thenReturn(List.of(stockPriceEntity()));
            switch (place) {
                case NIKKEI ->
                        assertThrows(FundanalyzerRuntimeException.class, () -> stockSpecification.insert(code, resultBean, place));
                case KABUOJI3, MINKABU, YAHOO_FINANCE ->
                        assertThrows(FundanalyzerAlreadyExistException.class, () -> stockSpecification.insert(code, resultBean, place));
            }
        }
    }

    @Nested
    class insert_minkabu {

        LocalDateTime nowLocalDateTime = LocalDateTime.of(2022, 12, 26, 19, 0);

        @BeforeEach
        void setUp() {
            when(stockSpecification.nowLocalDateTime()).thenReturn(nowLocalDateTime);
        }

        @ParameterizedTest
        @ValueSource(strings = {"01/01", "12/26", "12/31"})
        void targetDate_MMdd_ok(String targetDate) {
            var code = "code";
            var minkabuResultBean = new MinkabuResultBean(
                    null,
                    targetDate,
                    new MinkabuResultBean.ExpectedStockPrice(
                            null,
                            null,
                            null,
                            null
                    )
            );

            var nowLocalDate = MonthDay.parse(targetDate, DateTimeFormatter.ofPattern("MM/dd")).atYear(2022);
            doReturn(nowLocalDate).when(stockSpecification).nowLocalDate();
            when(minkabuDao.selectByCodeAndDate(code, nowLocalDate)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> stockSpecification.insert(code, minkabuResultBean));
            verify(minkabuDao, times(1))
                    .insert(MinkabuEntity.ofMinkabuResultBean(code, nowLocalDate, minkabuResultBean, nowLocalDateTime));
        }

        @ParameterizedTest
        @ValueSource(strings = {"00:00", "15:00", "23:59"})
        void targetDate_HHmm_ok(String targetDate) {
            var code = "code";
            var minkabuResultBean = new MinkabuResultBean(
                    null,
                    targetDate,
                    new MinkabuResultBean.ExpectedStockPrice(
                            null,
                            null,
                            null,
                            null
                    )
            );

            var nowLocalDate = LocalDate.parse("2022-12-26");
            doReturn(nowLocalDate).when(stockSpecification).nowLocalDate();
            when(minkabuDao.selectByCodeAndDate(code, nowLocalDate)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> stockSpecification.insert(code, minkabuResultBean));
            verify(minkabuDao, times(1))
                    .insert(MinkabuEntity.ofMinkabuResultBean(code, nowLocalDate, minkabuResultBean, nowLocalDateTime));
        }

        @ParameterizedTest
        @ValueSource(strings = {"1/", "22/26", "12/131"})
        void targetDate_MMdd_ng(String targetDate) {
            var code = "code";
            var minkabuResultBean = new MinkabuResultBean(
                    null,
                    targetDate,
                    new MinkabuResultBean.ExpectedStockPrice(
                            null,
                            null,
                            null,
                            null
                    )
            );

            when(minkabuDao.selectByCodeAndDate(any(), any())).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> stockSpecification.insert(code, minkabuResultBean));
            verify(minkabuDao, times(0)).insert(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"0:0", "25:00", "23/59"})
        void targetDate_HHmm_ng(String targetDate) {
            var code = "code";
            var minkabuResultBean = new MinkabuResultBean(
                    null,
                    targetDate,
                    new MinkabuResultBean.ExpectedStockPrice(
                            null,
                            null,
                            null,
                            null
                    )
            );

            when(minkabuDao.selectByCodeAndDate(any(), any())).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> stockSpecification.insert(code, minkabuResultBean));
            verify(minkabuDao, times(0)).insert(any());
        }
    }

    @Nested
    class getAverageStockPrice {

        private final String companyCode = "code";

        @BeforeEach
        void setUp() {
            when(companySpecification.findCompanyByCode(companyCode)).thenReturn(Optional.of(company()));
        }

        @DisplayName("getAverageStockPrice : 特定期間における平均の株価を取得する")
        @Test
        void averageStockPrice() {
            var stockPrice1 = new StockPriceEntity(
                    null,
                    null,
                    LocalDate.parse("2020-10-06"),
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
                    null,
                    null
            );
            var stockPrice2 = new StockPriceEntity(
                    null,
                    null,
                    LocalDate.parse("2020-10-07"),
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
                    null,
                    null
            );
            var stockPrice3 = new StockPriceEntity(
                    null,
                    null,
                    LocalDate.parse("2020-10-08"),
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
                    null,
                    null
            );
            var latestStockPrice = new StockPriceEntity(
                    null,
                    null,
                    LocalDate.parse("2020-10-09"),
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
                    null,
                    null
            );

            when(stockPriceDao.selectByCode(companyCode)).thenReturn(List.of(stockPrice1, stockPrice2, stockPrice3, latestStockPrice));

            var actual = stockSpecification.getAverageStockPrice(companyCode, LocalDate.parse("2020-10-08"));
            assertEquals(BigDecimal.valueOf(75000, 2), actual.orElseThrow());
        }

        @DisplayName("getAverageStockPrice : 特定期間がないときは空で返す")
        @Test
        void averageStockPrice_isNull() {
            var actual = stockSpecification.getAverageStockPrice(companyCode, LocalDate.parse("2020-10-08"));
            assertNull(actual.orElse(null));
        }
    }

    private Company company() {
        return new Company(
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
                false
        );
    }

    private StockPriceEntity stockPriceEntity() {
        return new StockPriceEntity(
                null,
                null,
                null,
                100.0,
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