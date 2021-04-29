package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.MinkabuDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Minkabu;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPrice;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.StockScraping;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.bean.Kabuoji3ResultBean;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.bean.MinkabuResultBean;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.bean.NikkeiResultBean;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerScrapingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockServiceTest {

    private StockScraping stockScraping;
    private CompanyDao companyDao;
    private DocumentDao documentDao;
    private StockPriceDao stockPriceDao;
    private MinkabuDao minkabuDao;

    private StockService service;

    @BeforeEach
    void setUp() {
        stockScraping = Mockito.mock(StockScraping.class);
        companyDao = Mockito.mock(CompanyDao.class);
        documentDao = Mockito.mock(DocumentDao.class);
        stockPriceDao = Mockito.mock(StockPriceDao.class);
        minkabuDao = Mockito.mock(MinkabuDao.class);

        service = Mockito.spy(new StockService(
                stockScraping,
                companyDao,
                documentDao,
                stockPriceDao,
                minkabuDao
        ));
    }

    @Nested
    class importStockPrice {

        @DisplayName("importStockPrice : 指定日付に提出された企業の株価を取得する")
        @Test
        void importStockPrice_ok_submitDate() {
            final var submitDate = LocalDate.parse("2020-11-14");
            var docTypeCodes = List.of(DocTypeCode.ANNUAL_SECURITIES_REPORT);

            when(documentDao.selectByTypeAndSubmitDate(List.of("120"), submitDate)).thenReturn(List.of(
                    Document.builder()
                            .edinetCode("edinetCode")
                            .build()
            ));
            when(companyDao.selectAll()).thenReturn(List.of(new Company(
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
            )));
            doNothing().when(service).importStockPrice("code");

            assertDoesNotThrow(() -> service.importStockPrice(submitDate, docTypeCodes));
        }

        @DisplayName("importStockPrice : 指定企業の株価をnikkeiから取得する")
        @Test
        void importStockPrice_ok_code_nikkei_insert() {
            var code = "code";
            var createdAt = LocalDateTime.of(2020, 11, 14, 18, 21);

            when(stockScraping.nikkei(code)).thenReturn(new NikkeiResultBean(
                    "1000",
                    "2020/11/1",
                    "1000",
                    "1000",
                    "1000",
                    "1000",
                    "10",
                    "10",
                    "10",
                    "1000株",
                    "1000",
                    "1%",
                    "優待券"
            ));
            when(stockScraping.kabuoji3(code)).thenReturn(List.of(generateKabuoji3ResultBean()));
            when(stockPriceDao.selectByCode(code)).thenReturn(List.of());
            when(stockScraping.minkabu(code)).thenReturn(generateMinkabuResultBean());
            doReturn(createdAt).when(service).nowLocalDateTime();

            assertDoesNotThrow(() -> service.importStockPrice(code));

            verify(stockPriceDao, times(1)).insert(new StockPrice(
                    null,
                    "code",
                    LocalDate.parse("2020-11-01"),
                    1000.0,
                    1000.0,
                    1000.0,
                    1000.0,
                    1000,
                    "10",
                    "10",
                    "10",
                    "1000株",
                    "1000",
                    "1%",
                    "優待券",
                    "1",
                    createdAt
            ));
        }

        @DisplayName("importStockPrice : 指定企業の株価をkabuoji3から取得する")
        @Test
        void importStockPrice_ok_code_kabuoji3_insert() {
            var code = "code";
            var createdAt = LocalDateTime.of(2020, 11, 14, 18, 21);

            when(stockScraping.nikkei(code)).thenReturn(generateNikkeiResultBean());
            when(stockScraping.kabuoji3(code)).thenReturn(List.of(new Kabuoji3ResultBean(
                    "2020-11-14",
                    "1000",
                    "1000",
                    "1000",
                    "1000",
                    "1000",
                    "1000"
            )));
            when(stockPriceDao.selectByCode(code)).thenReturn(List.of(new StockPrice(
                    1,
                    "code",
                    LocalDate.parse("2020-11-01"),
                    1000.0,
                    1000.0,
                    1000.0,
                    1000.0,
                    1000,
                    "10",
                    "10",
                    "10",
                    "1000株",
                    "1000",
                    "1%",
                    "優待券",
                    "1",
                    createdAt
            )));
            when(stockScraping.minkabu(code)).thenReturn(generateMinkabuResultBean());
            doReturn(createdAt).when(service).nowLocalDateTime();

            assertDoesNotThrow(() -> service.importStockPrice(code));

            verify(stockPriceDao, times(0)).insert(new StockPrice(
                    null,
                    "code",
                    LocalDate.parse("2020-11-01"),
                    1000.0,
                    1000.0,
                    1000.0,
                    1000.0,
                    1000,
                    "10",
                    "10",
                    "10",
                    "1000株",
                    "1000",
                    "1%",
                    "優待券",
                    "1",
                    createdAt
            ));
            verify(stockPriceDao, times(1)).insert(new StockPrice(
                    null,
                    "code",
                    LocalDate.parse("2020-11-14"),
                    1000.0,
                    1000.0,
                    1000.0,
                    1000.0,
                    1000,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "2",
                    createdAt
            ));
        }

        @DisplayName("importStockPrice : 指定企業の株価予想をみんかぶから取得する")
        @Test
        void importStockPrice_ok_code_minkabu_insert() {
            var code = "code";
            var createdAt = LocalDateTime.of(2020, 11, 14, 18, 21);

            when(stockScraping.nikkei(code)).thenReturn(generateNikkeiResultBean());
            when(stockScraping.kabuoji3(code)).thenReturn(List.of(generateKabuoji3ResultBean()));
            when(stockScraping.minkabu(code)).thenReturn(new MinkabuResultBean(
                    "408. 0 円",
                    "11/27",
                    new MinkabuResultBean.ExpectedStockPrice(
                            "636",
                            "638",
                            "649",
                            "630"
                    )
            ));
            doReturn(createdAt).when(service).nowLocalDateTime();

            assertDoesNotThrow(() -> service.importStockPrice(code));

            verify(minkabuDao, times(1)).insert(new Minkabu(
                    null,
                    "code",
                    MonthDay.parse("--11-27").atYear(LocalDate.now().getYear()),
                    408.0,
                    636.0,
                    638.0,
                    649.0,
                    630.0,
                    createdAt
            ));
        }

        @DisplayName("importStockPrice : 指定企業の株価が取得済のときは何もしない")
        @Test
        void importStockPrice_ok_code_not_insert() {
            var code = "code";
            var createdAt = LocalDateTime.of(2020, 11, 14, 18, 21);

            when(stockScraping.nikkei(code)).thenReturn(new NikkeiResultBean(
                    "1000",
                    "2020/11/1",
                    "1000",
                    "1000",
                    "1000",
                    "1000",
                    "10",
                    "10",
                    "10",
                    "1000株",
                    "1000",
                    "1%",
                    "優待券"
            ));
            when(stockScraping.kabuoji3(code)).thenReturn(List.of(new Kabuoji3ResultBean(
                    "2020-11-14",
                    "1000",
                    "1000",
                    "1000",
                    "1000",
                    "1000",
                    "1000"
            )));
            when(stockPriceDao.selectByCode(code)).thenReturn(List.of(
                    new StockPrice(
                            1,
                            "code",
                            LocalDate.parse("2020-11-01"),
                            1000.0,
                            1000.0,
                            1000.0,
                            1000.0,
                            1000,
                            "10",
                            "10",
                            "10",
                            "1000株",
                            "1000",
                            "1%",
                            "優待券",
                            "1",
                            createdAt
                    ),
                    new StockPrice(
                            2,
                            "code",
                            LocalDate.parse("2020-11-14"),
                            1000.0,
                            1000.0,
                            1000.0,
                            1000.0,
                            1000,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "2",
                            createdAt
                    )
            ));
            when(stockScraping.minkabu(code)).thenReturn(new MinkabuResultBean(
                    "408. 0 円",
                    "11/27",
                    new MinkabuResultBean.ExpectedStockPrice(
                            "636",
                            "638",
                            "649",
                            "630"
                    )
            ));
            doReturn(createdAt).when(service).nowLocalDateTime();

            assertDoesNotThrow(() -> service.importStockPrice(code));

            verify(stockPriceDao, times(0)).insert(new StockPrice(
                    null,
                    "code",
                    LocalDate.parse("2020-11-01"),
                    1000.0,
                    1000.0,
                    1000.0,
                    1000.0,
                    1000,
                    "10",
                    "10",
                    "10",
                    "1000株",
                    "1000",
                    "1%",
                    "優待券",
                    "1",
                    createdAt
            ));
            verify(stockPriceDao, times(0)).insert(new StockPrice(
                    null,
                    "code",
                    LocalDate.parse("2020-11-14"),
                    1000.0,
                    1000.0,
                    1000.0,
                    1000.0,
                    1000,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "2",
                    createdAt
            ));
        }

        @DisplayName("importStockPrice : エラーが発生したときの挙動を確認する")
        @Test
        void importStockPrice_FundanalyzerRuntimeException() {
            var code = "code";

            when(stockScraping.nikkei(code)).thenThrow(FundanalyzerScrapingException.class);

            assertDoesNotThrow(() -> service.importStockPrice(code));
        }

        private NikkeiResultBean generateNikkeiResultBean() {
            return new NikkeiResultBean(
                    "1000",
                    "2020/11/1",
                    "1000",
                    "1000",
                    "1000",
                    "1000",
                    "10",
                    "10",
                    "10",
                    "1000株",
                    "1000",
                    "1%",
                    "優待券"
            );
        }

        private Kabuoji3ResultBean generateKabuoji3ResultBean() {
            return new Kabuoji3ResultBean(
                    "2020-11-01",
                    "1000",
                    "1000",
                    "1000",
                    "1000",
                    "1000",
                    "1000"
            );
        }

        private MinkabuResultBean generateMinkabuResultBean() {
            return new MinkabuResultBean(
                    "408. 0 円",
                    "11/27",
                    new MinkabuResultBean.ExpectedStockPrice(
                            "636",
                            "638",
                            "649",
                            "630"
                    )
            );
        }
    }
}