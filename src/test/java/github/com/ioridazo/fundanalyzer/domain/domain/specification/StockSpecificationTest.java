package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.MinkabuDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean.Kabuoji3ResultBean;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockSpecificationTest {

    private StockPriceDao stockPriceDao;
    private DocumentSpecification documentSpecification;

    private StockSpecification stockSpecification;

    @BeforeEach
    void setUp() {
        stockPriceDao = Mockito.mock(StockPriceDao.class);
        documentSpecification = Mockito.mock(DocumentSpecification.class);

        stockSpecification = Mockito.spy(new StockSpecification(
                stockPriceDao,
                Mockito.mock(MinkabuDao.class),
                documentSpecification
        ));
        stockSpecification.daysToViewEdinetList = 30;
        stockSpecification.daysToStoreStockPrice = 365;
    }

    @Nested
    class findStock {

        Company company = new Company(
                "code",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

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
                    null
            );

            when(stockPriceDao.selectByCode("code")).thenReturn(List.of(stockPrice1, stockPrice2, stockPrice3, latestStockPrice));
        }

        @DisplayName("importDate : 株価取得日を取得する")
        @Test
        void importDate() {
            when(documentSpecification.latestDocument(company)).thenReturn(Optional.of(new Document(
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
            when(documentSpecification.latestDocument(company)).thenReturn(Optional.of(new Document(
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
            when(documentSpecification.latestDocument(company)).thenReturn(Optional.of(new Document(
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
            when(documentSpecification.latestDocument(company)).thenReturn(Optional.of(new Document(
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
    class insert {

        @BeforeEach
        void setUp() {
            doReturn(LocalDate.parse("2021-06-06")).when(stockSpecification).nowLocalDate();
        }

        @DisplayName("insert : kabuoji3から取得した株価情報の保存する期間を絞る")
        @Test
        void kabuoji3() {
            assertDoesNotThrow(() -> stockSpecification.insert("code", List.of(
                    Kabuoji3ResultBean.of(
                            "2019-06-05",
                            "1000",
                            "1000",
                            "1000",
                            "1000",
                            "1000",
                            "1000"
                    ),
                    Kabuoji3ResultBean.of(
                            "2020-06-05",
                            "1000",
                            "1000",
                            "1000",
                            "1000",
                            "1000",
                            "1000"
                    ),
                    Kabuoji3ResultBean.of(
                            "2021-06-05",
                            "1000",
                            "1000",
                            "1000",
                            "1000",
                            "1000",
                            "1000"
                    )
            )));
            verify(stockPriceDao, times(1)).insert(any());
        }
    }
}