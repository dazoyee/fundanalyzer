package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.MinkabuDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@SuppressWarnings("NewClassNamingConvention")
class StockSpecificationTest {

    private StockPriceDao stockPriceDao;
    private CompanySpecification companySpecification;
    private DocumentSpecification documentSpecification;

    private StockSpecification stockSpecification;

    @BeforeEach
    void setUp() {
        stockPriceDao = Mockito.mock(StockPriceDao.class);
        companySpecification = Mockito.mock(CompanySpecification.class);
        documentSpecification = Mockito.mock(DocumentSpecification.class);

        stockSpecification = Mockito.spy(new StockSpecification(
                stockPriceDao,
                Mockito.mock(MinkabuDao.class),
                companySpecification,
                documentSpecification
        ));
        stockSpecification.daysToAverageStockPrice = 30;
        stockSpecification.targetCompanyNumber = 1;
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
            when(companySpecification.allTargetCompanies()).thenReturn(List.of(
                    new Company(
                            "code1",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
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
                            null
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
}