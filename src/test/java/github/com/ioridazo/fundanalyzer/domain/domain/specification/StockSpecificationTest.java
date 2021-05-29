package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.MinkabuDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        stockSpecification.lastDays = 30;
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
}