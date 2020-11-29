package github.com.ioridazo.fundanalyzer.domain.scraping.jsoup;

import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class StockScrapingTest {

    private StockScraping stockScraping;

    @BeforeEach
    void setUp() {
        stockScraping = spy(new StockScraping());
    }

    private Document jsoupParser(final File file) throws IOException {
        return Jsoup.parse(file, "UTF-8");
    }

    @Nested
    class nikkei {

        @DisplayName("nikkei : 実際に日経の会社コードによる株価情報を取得する")
//        @Test
        void nikkei_test() {
            var code = "9434";

            var actual = assertDoesNotThrow(() -> stockScraping.nikkei(code));

            assertAll("NikkeiResultBean",
                    () -> assertNotNull(actual.getStockPrice()),
                    () -> assertNotNull(actual.getTargetDate()),
                    () -> assertNotNull(actual.getOpeningPrice()),
                    () -> assertNotNull(actual.getHighPrice()),
                    () -> assertNotNull(actual.getLowPrice()),
                    () -> assertNotNull(actual.getVolume()),
                    () -> assertNotNull(actual.getPer()),
                    () -> assertNotNull(actual.getPbr()),
                    () -> assertNotNull(actual.getRoe()),
                    () -> assertNotNull(actual.getNumberOfShares()),
                    () -> assertNotNull(actual.getMarketCapitalization()),
                    () -> assertNotNull(actual.getDividendYield()),
                    () -> assertNotNull(actual.getShareholderBenefit())
            );

            System.out.println(actual);
        }

        @DisplayName("nikkei : 日経の会社コードによる株価情報を取得する")
        @Test
        void nikkei_ok() throws IOException {
            var code = "9999";

            var htmlFile = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/scraping/jsoup/nikkei/nikkei.html");

            doReturn(jsoupParser(htmlFile)).when(stockScraping).jsoup(any());

            var actual = stockScraping.nikkei(code);

            assertAll("NikkeiResultBean",
                    () -> assertEquals("1,047 円", actual.getStockPrice()),
                    () -> assertEquals("2020/11/27", actual.getTargetDate()),
                    () -> assertEquals("始値 (15:00) 1,047 円", actual.getOpeningPrice()),
                    () -> assertEquals("高値 (15:00) 1,047 円", actual.getHighPrice()),
                    () -> assertEquals("安値 (15:00) 1,047 円", actual.getLowPrice()),
                    () -> assertEquals("売買高 134,400 株", actual.getVolume()),
                    () -> assertEquals("予想PER（解説） -- 倍", actual.getPer()),
                    () -> assertEquals("PBR（実績）（解説） 1.02 倍", actual.getPbr()),
                    () -> assertEquals("ROE（予想） （解説） N/A", actual.getRoe()),
                    () -> assertEquals("普通株式数（解説） 95,857,420 株", actual.getNumberOfShares()),
                    () -> assertEquals("時価総額（解説） 100,362 百万円", actual.getMarketCapitalization()),
                    () -> assertEquals("株式益回り（予想）（解説） N/A", actual.getDividendYield()),
                    () -> assertEquals("株主優待 ギフト券 レジャー 招待券", actual.getShareholderBenefit())
            );
        }

        @DisplayName("nikkei : 日経の株価情報を取得するときにタイムアウトエラーが発生したときの挙動を確認する")
        @Test
        void nikkei_SocketTimeoutException() throws IOException {
            var code = "9999";

            doThrow(SocketTimeoutException.class).when(stockScraping).jsoup(any());

            var actual = assertThrows(FundanalyzerRuntimeException.class, () -> stockScraping.nikkei(code));

            System.out.println(actual.getMessage());
        }

        @DisplayName("nikkei : 日経の株価情報を取得するときにIO系エラーが発生したときの挙動を確認する")
        @Test
        void nikkei_IOException() throws IOException {
            var code = "9999";

            doThrow(IOException.class).when(stockScraping).jsoup(any());

            var actual = assertThrows(FundanalyzerRuntimeException.class, () -> stockScraping.nikkei(code));

            System.out.println(actual.getMessage());
        }

        @DisplayName("nikkei : 日経の株価情報を取得するときに想定外エラーが発生したときの挙動を確認する")
        @Test
        void nikkei_RuntimeException() throws IOException {
            var code = "9999";

            doThrow(RuntimeException.class).when(stockScraping).jsoup(any());

            var actual = assertThrows(FundanalyzerRuntimeException.class, () -> stockScraping.nikkei(code));

            System.out.println(actual.getMessage());
        }
    }

    @Nested
    class kabuoji3 {

        @DisplayName("kabuoji3 : 実際にkabuoji3の会社コードによる株価情報を取得する")
//        @Test
        void kabuoji3_test() {
            var code = "9434";

            var actual = stockScraping.kabuoji3(code);

            assertNotNull(actual);
            assertEquals(300, actual.size());

            actual.forEach(System.out::println);
        }

        @DisplayName("kabuoji3 : kabuoji3の会社コードによる株価情報を取得する")
        @Test
        void kabuoji3_ok() throws IOException {
            var code = "9999";

            var htmlFile = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/scraping/jsoup/kabuoji3/kabuoji3.html");

            doReturn(jsoupParser(htmlFile)).when(stockScraping).jsoup(any());

            var actual = stockScraping.kabuoji3(code);

            assertAll("Kabuoji3ResultBean",
                    () -> assertAll(
                            () -> assertEquals("2020-11-27", actual.get(0).getTargetDate()),
                            () -> assertEquals("1284.5", actual.get(0).getOpeningPrice()),
                            () -> assertEquals("1290", actual.get(0).getHighPrice()),
                            () -> assertEquals("1274.5", actual.get(0).getLowPrice()),
                            () -> assertEquals("1287", actual.get(0).getClosingPrice()),
                            () -> assertEquals("18203600", actual.get(0).getVolume()),
                            () -> assertEquals("1287", actual.get(0).getClosingPriceAdjustment())
                    ),
                    () -> assertAll(
                            () -> assertEquals("2020-11-26", actual.get(1).getTargetDate()),
                            () -> assertEquals("1288.5", actual.get(1).getOpeningPrice()),
                            () -> assertEquals("1294.5", actual.get(1).getHighPrice()),
                            () -> assertEquals("1282", actual.get(1).getLowPrice()),
                            () -> assertEquals("1285", actual.get(1).getClosingPrice()),
                            () -> assertEquals("15760400", actual.get(1).getVolume()),
                            () -> assertEquals("1285", actual.get(1).getClosingPriceAdjustment())
                    ),
                    () -> assertAll(
                            () -> assertEquals("2019-08-16", actual.get(299).getTargetDate()),
                            () -> assertEquals("1499.5", actual.get(299).getOpeningPrice()),
                            () -> assertEquals("1505", actual.get(299).getHighPrice()),
                            () -> assertEquals("1498.5", actual.get(299).getLowPrice()),
                            () -> assertEquals("1501", actual.get(299).getClosingPrice()),
                            () -> assertEquals("9807700", actual.get(299).getVolume()),
                            () -> assertEquals("1501", actual.get(299).getClosingPriceAdjustment())
                    )
            );
            assertEquals(300, actual.size());
        }

        @DisplayName("kabuoji3 : kabuoji3の株価情報を取得するときにタイムアウトエラーが発生したときの挙動を確認する")
        @Test
        void kabuoji3_SocketTimeoutException() throws IOException {
            var code = "9999";

            doThrow(SocketTimeoutException.class).when(stockScraping).jsoup(any());

            var actual = assertThrows(FundanalyzerRuntimeException.class, () -> stockScraping.kabuoji3(code));

            System.out.println(actual.getMessage());
        }

        @DisplayName("kabuoji3 : kabuoji3の株価情報を取得するときにIO系エラーが発生したときの挙動を確認する")
        @Test
        void kabuoji3_IOException() throws IOException {
            var code = "9999";

            doThrow(IOException.class).when(stockScraping).jsoup(any());

            var actual = assertThrows(FundanalyzerRuntimeException.class, () -> stockScraping.kabuoji3(code));

            System.out.println(actual.getMessage());
        }

        @DisplayName("kabuoji3 : kabuoji3の株価情報を取得するときに想定外エラーが発生したときの挙動を確認する")
        @Test
        void kabuoji3_RuntimeException() throws IOException {
            var code = "9999";

            doThrow(RuntimeException.class).when(stockScraping).jsoup(any());

            var actual = assertThrows(FundanalyzerRuntimeException.class, () -> stockScraping.kabuoji3(code));

            System.out.println(actual.getMessage());
        }
    }
}