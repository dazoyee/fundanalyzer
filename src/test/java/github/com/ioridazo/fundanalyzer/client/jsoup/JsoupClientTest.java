package github.com.ioridazo.fundanalyzer.client.jsoup;

import github.com.ioridazo.fundanalyzer.config.AppConfig;
import github.com.ioridazo.fundanalyzer.config.RestClientProperties;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCircuitBreakerRecordException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRateLimiterException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerScrapingException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerShortCircuitException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Timeout(10)
class JsoupClientTest {

    private static MockWebServer server;
    private RestTemplate restTemplate;
    private RetryTemplate retryTemplate;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RateLimiterRegistry rateLimiterRegistry;
    private JsoupClient client;

    private static RestClientProperties properties() {
        var jsoup = new RestClientProperties.Settings();
        jsoup.setConnectTimeout(Duration.ofMillis(100));
        jsoup.setReadTimeout(Duration.ofMillis(100));
        jsoup.setMaxAttempts(2);
        jsoup.setBackOff(Duration.ofMillis(1));
        var nikkei = new RestClientProperties.Settings();
        nikkei.setBaseUri(String.format("http://localhost:%s", server.getPort()));
        var kabuoji3 = new RestClientProperties.Settings();
        kabuoji3.setBaseUri(String.format("http://localhost:%s", server.getPort()));
        var minkabu = new RestClientProperties.Settings();
        minkabu.setBaseUri(String.format("http://localhost:%s", server.getPort()));
        var yahooFinance = new RestClientProperties.Settings();
        yahooFinance.setBaseUri(String.format("http://localhost:%s", server.getPort()));

        return new RestClientProperties(Map.of(
                "jsoup", jsoup,
                "nikkei", nikkei,
                "kabuoji3", kabuoji3,
                "minkabu", minkabu,
                "yahoo-finance", yahooFinance
        ));
    }

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        this.restTemplate = Mockito.spy(new AppConfig().restTemplateJsoup(properties()));
        this.retryTemplate = new AppConfig().retryTemplateJsoup(properties());
        this.circuitBreakerRegistry = new CircuitBreakerRegistry.Builder().build();
        this.rateLimiterRegistry = new RateLimiterRegistry.Builder().build();
        this.client = spy(new JsoupClient(
                properties(),
                restTemplate,
                retryTemplate,
                circuitBreakerRegistry,
                rateLimiterRegistry
        ));
        this.client.yahooPages = 1;

        Mockito.clearInvocations(client);
        Mockito.reset(client);
    }

    @AfterEach
    void after() throws IOException {
        server.shutdown();
    }

    private Document jsoupParser(final File file) throws IOException {
        return Jsoup.parse(file, "UTF-8");
    }

    @Nested
    class nikkei {

        @DisplayName("nikkei : 実際に日経の会社コードによる株価情報を取得する")
            // @Test
        void nikkei_test() {
            var jsoup = new RestClientProperties.Settings();
            jsoup.setConnectTimeout(Duration.ofMillis(10000));
            jsoup.setReadTimeout(Duration.ofMillis(10000));
            jsoup.setMaxAttempts(2);
            jsoup.setBackOff(Duration.ofMillis(0));
            var nikkei = new RestClientProperties.Settings();
            nikkei.setBaseUri("https://www.nikkei.com");

            var properties = new RestClientProperties(Map.of("jsoup", jsoup, "nikkei", nikkei));
            client = spy(new JsoupClient(
                    properties,
                    Mockito.spy(new AppConfig().restTemplateJsoup(properties)),
                    retryTemplate,
                    circuitBreakerRegistry,
                    rateLimiterRegistry));

            var code = "9434";
            var actual = assertDoesNotThrow(() -> client.nikkei(code));

            assertAll("NikkeiResultBean",
                    () -> assertNotNull(actual.stockPrice()),
                    () -> assertNotNull(actual.targetDate()),
                    () -> assertNotNull(actual.openingPrice()),
                    () -> assertNotNull(actual.highPrice()),
                    () -> assertNotNull(actual.lowPrice()),
                    () -> assertNotNull(actual.volume()),
                    () -> assertNotNull(actual.per()),
                    () -> assertNotNull(actual.pbr()),
                    () -> assertNotNull(actual.roe()),
                    () -> assertNotNull(actual.numberOfShares()),
                    () -> assertNotNull(actual.marketCapitalization()),
                    () -> assertNotNull(actual.dividendYield()),
                    () -> assertNotNull(actual.shareholderBenefit())
            );
            System.out.println(actual);
        }

        @DisplayName("nikkei : 日経の会社コードによる株価情報を取得する")
        @Test
        void nikkei_ok() throws IOException {
            var code = "9999";
            var htmlFile = new File("src/test/resources/github/com/ioridazo/fundanalyzer/client/jsoup/nikkei/nikkei.html");
            doReturn(jsoupParser(htmlFile)).when(client).getForHtml(any(), any(), any());
            var actual = client.nikkei(code);

            assertAll("NikkeiResultBean",
                    () -> assertEquals("1,047 円", actual.stockPrice()),
                    () -> assertEquals("2020/11/27", actual.targetDate()),
                    () -> assertEquals("始値 (15:00) 1,047 円", actual.openingPrice()),
                    () -> assertEquals("高値 (15:00) 1,047 円", actual.highPrice()),
                    () -> assertEquals("安値 (15:00) 1,047 円", actual.lowPrice()),
                    () -> assertEquals("売買高 134,400 株", actual.volume()),
                    () -> assertEquals("予想PER（解説） -- 倍", actual.per()),
                    () -> assertEquals("PBR（実績）（解説） 1.02 倍", actual.pbr()),
                    () -> assertEquals("ROE（予想） （解説） N/A", actual.roe()),
                    () -> assertEquals("普通株式数（解説） 95,857,420 株", actual.numberOfShares()),
                    () -> assertEquals("時価総額（解説） 100,362 百万円", actual.marketCapitalization()),
                    () -> assertEquals("予想配当利回り（解説） 1.14 ％", actual.dividendYield()),
                    () -> assertEquals("株主優待 ギフト券 レジャー 招待券", actual.shareholderBenefit())
            );
        }

        @DisplayName("nikkei : 日経の会社コードによる株価情報を取得できないときはnullにする")
            // @Test
        void nikkei_null() {
            var jsoup = new RestClientProperties.Settings();
            jsoup.setConnectTimeout(Duration.ofMillis(10000));
            jsoup.setReadTimeout(Duration.ofMillis(10000));
            jsoup.setMaxAttempts(2);
            jsoup.setBackOff(Duration.ofMillis(0));
            var nikkei = new RestClientProperties.Settings();
            nikkei.setBaseUri("https://www.nikkei.com");
            client = spy(new JsoupClient(
                    new RestClientProperties(Map.of("jsoup", jsoup, "nikkei", nikkei)),
                    restTemplate,
                    retryTemplate,
                    circuitBreakerRegistry,
                    rateLimiterRegistry
            ));

            var code = "9999";
            var actual = client.nikkei(code);

            assertAll("NikkeiResultBean",
                    () -> assertNull(actual.stockPrice()),
                    () -> assertNull(actual.targetDate()),
                    () -> assertNull(actual.openingPrice()),
                    () -> assertNull(actual.highPrice()),
                    () -> assertNull(actual.lowPrice()),
                    () -> assertNull(actual.volume()),
                    () -> assertNull(actual.per()),
                    () -> assertNull(actual.pbr()),
                    () -> assertNull(actual.roe()),
                    () -> assertNull(actual.numberOfShares()),
                    () -> assertNull(actual.marketCapitalization()),
                    () -> assertNull(actual.dividendYield()),
                    () -> assertNull(actual.shareholderBenefit())
            );
        }
    }

    @Nested
    class kabuoji3 {

        @DisplayName("kabuoji3 : 実際にkabuoji3の会社コードによる株価情報を取得する")
//        @Test
        void kabuoji3_test() {
            var jsoup = new RestClientProperties.Settings();
            jsoup.setConnectTimeout(Duration.ofMillis(10000));
            jsoup.setReadTimeout(Duration.ofMillis(10000));
            jsoup.setMaxAttempts(2);
            jsoup.setBackOff(Duration.ofMillis(0));
            var kabuoji3 = new RestClientProperties.Settings();
            kabuoji3.setBaseUri("https://kabuoji3.com");

            var properties = new RestClientProperties(Map.of("jsoup", jsoup, "kabuoji3", kabuoji3));
            client = spy(new JsoupClient(
                    properties,
                    Mockito.spy(new AppConfig().restTemplateJsoup(properties)),
                    retryTemplate,
                    circuitBreakerRegistry,
                    rateLimiterRegistry
            ));

            var code = "9434";
            var actual = client.kabuoji3(code);

            assertNotNull(actual);
            assertEquals(300, actual.size());
            actual.forEach(System.out::println);
        }

        @DisplayName("kabuoji3 : kabuoji3の会社コードによる株価情報を取得する")
        @Test
        void kabuoji3_ok() throws IOException {
            var code = "9999";
            var htmlFile = new File("src/test/resources/github/com/ioridazo/fundanalyzer/client/jsoup/kabuoji3/kabuoji3.html");
            doReturn(jsoupParser(htmlFile)).when(client).getForHtml(any(), any(), any());
            var actual = client.kabuoji3(code);

            assertAll("Kabuoji3ResultBean",
                    () -> assertAll(
                            () -> assertEquals("2020-11-27", actual.get(0).targetDate()),
                            () -> assertEquals("1284.5", actual.get(0).openingPrice()),
                            () -> assertEquals("1290", actual.get(0).highPrice()),
                            () -> assertEquals("1274.5", actual.get(0).lowPrice()),
                            () -> assertEquals("1287", actual.get(0).closingPrice()),
                            () -> assertEquals("18203600", actual.get(0).volume()),
                            () -> assertEquals("1287", actual.get(0).closingPriceAdjustment())
                    ),
                    () -> assertAll(
                            () -> assertEquals("2020-11-26", actual.get(1).targetDate()),
                            () -> assertEquals("1288.5", actual.get(1).openingPrice()),
                            () -> assertEquals("1294.5", actual.get(1).highPrice()),
                            () -> assertEquals("1282", actual.get(1).lowPrice()),
                            () -> assertEquals("1285", actual.get(1).closingPrice()),
                            () -> assertEquals("15760400", actual.get(1).volume()),
                            () -> assertEquals("1285", actual.get(1).closingPriceAdjustment())
                    ),
                    () -> assertAll(
                            () -> assertEquals("2019-08-16", actual.get(299).targetDate()),
                            () -> assertEquals("1499.5", actual.get(299).openingPrice()),
                            () -> assertEquals("1505", actual.get(299).highPrice()),
                            () -> assertEquals("1498.5", actual.get(299).lowPrice()),
                            () -> assertEquals("1501", actual.get(299).closingPrice()),
                            () -> assertEquals("9807700", actual.get(299).volume()),
                            () -> assertEquals("1501", actual.get(299).closingPriceAdjustment())
                    )
            );
            assertEquals(300, actual.size());
        }
    }

    @Nested
    class minkabu {

        @DisplayName("minkabu : 実際にみんかぶの会社コードによる株価情報予想を取得する")
            // @Test
        void minkabu_test() {
            var jsoup = new RestClientProperties.Settings();
            jsoup.setConnectTimeout(Duration.ofMillis(10000));
            jsoup.setReadTimeout(Duration.ofMillis(10000));
            jsoup.setMaxAttempts(2);
            jsoup.setBackOff(Duration.ofMillis(0));
            var minkabu = new RestClientProperties.Settings();
            minkabu.setBaseUri("https://minkabu.jp");

            var properties = new RestClientProperties(Map.of("jsoup", jsoup, "minkabu", minkabu));
            client = spy(new JsoupClient(
                    properties,
                    Mockito.spy(new AppConfig().restTemplateJsoup(properties)),
                    retryTemplate,
                    circuitBreakerRegistry,
                    rateLimiterRegistry
            ));

            var code = "9434";
            var actual = client.minkabu(code);

            assertAll("MinkabuResultBean",
                    () -> assertNotNull(actual.stockPrice()),
                    () -> assertNotNull(actual.targetDate()),
                    () -> assertNotNull(actual.expectedStockPrice().goals()),
                    () -> assertNotNull(actual.expectedStockPrice().theoretical()),
                    () -> assertNotNull(actual.expectedStockPrice().individualInvestors()),
                    () -> assertNotNull(actual.expectedStockPrice().securitiesAnalyst())
            );
            System.out.println(actual);
        }

        @DisplayName("minkabu : みんかぶの会社コードによる株価情報予想を取得する")
        @Test
        void minkabu_ok() throws IOException {
            var code = "9999";
            var htmlFile = new File("src/test/resources/github/com/ioridazo/fundanalyzer/client/jsoup/minkabu/minkabu.html");
            doReturn(jsoupParser(htmlFile)).when(client).getForHtml(any(), any(), any());
            var actual = client.minkabu(code);

            assertAll("MinkabuResultBean",
                    () -> assertEquals("1,450.0 円", actual.stockPrice()),
                    () -> assertEquals("10/21", actual.targetDate()),
                    () -> assertEquals("1,606", actual.expectedStockPrice().goals()),
                    () -> assertEquals("1,538", actual.expectedStockPrice().theoretical()),
                    () -> assertEquals("1,742", actual.expectedStockPrice().individualInvestors()),
                    () -> assertEquals("1,593", actual.expectedStockPrice().securitiesAnalyst())
            );
        }

        @DisplayName("minkabu : [検証]みんかぶの会社コードによる株価情報予想を取得する")
        @Test
        void minkabu_verify() throws IOException {
            var code = "9903";
            var htmlFile = new File("src/test/resources/github/com/ioridazo/fundanalyzer/client/jsoup/minkabu/minkabu_9903.html");
            doReturn(jsoupParser(htmlFile)).when(client).getForHtml(any(), any(), any());
            var actual = client.minkabu(code);

            assertAll("MinkabuResultBean",
                    () -> assertEquals("1,682.0 円", actual.stockPrice()),
                    () -> assertEquals("10/21", actual.targetDate()),
                    () -> assertEquals("2,071", actual.expectedStockPrice().goals()),
                    () -> assertEquals("2,486", actual.expectedStockPrice().theoretical()),
                    () -> assertEquals("1,448", actual.expectedStockPrice().individualInvestors()),
                    () -> assertNull(actual.expectedStockPrice().securitiesAnalyst())
            );
        }

        @DisplayName("minkabu : みんかぶの会社コードによる株価情報予想を取得できないときはnullにする")
        @Test
        void minkabu_null() throws IOException {
            var code = "9999";
            var htmlFile = new File("src/test/resources/github/com/ioridazo/fundanalyzer/client/jsoup/minkabu/minkabu_null.html");
            doReturn(jsoupParser(htmlFile)).when(client).getForHtml(any(), any(), any());
            var actual = client.minkabu(code);


            assertAll("MinkabuResultBean",
                    () -> assertEquals("408. 0 円", actual.stockPrice()),
                    () -> assertEquals("11/27", actual.targetDate()),
                    () -> assertEquals("636", actual.expectedStockPrice().goals()),
                    () -> assertNull(actual.expectedStockPrice().theoretical()),
                    () -> assertNull(actual.expectedStockPrice().individualInvestors()),
                    () -> assertNull(actual.expectedStockPrice().securitiesAnalyst())
            );
        }

        // @Test
        @SuppressWarnings("unused")
        void minkabu_url() throws IOException {
            var code = "9903";
            final var url = UriComponentsBuilder
                    .newInstance()
                    .scheme("https").host("minkabu.jp")
                    .path("/stock/{code}")
                    .buildAndExpand(code.substring(0, 4))
                    .toUriString();

            System.out.println(Jsoup.connect(url).get());
        }
    }

    @Nested
    class isLivedCompanyFromMinkabu {

        @DisplayName("isLivedCompanyFromMinkabu : 実際にみんかぶの会社コードによる上場状況を取得する")
//        @Test
        void isLivedCompanyFromMinkabu_test() {
            var jsoup = new RestClientProperties.Settings();
            jsoup.setConnectTimeout(Duration.ofMillis(10000));
            jsoup.setReadTimeout(Duration.ofMillis(10000));
            jsoup.setMaxAttempts(2);
            jsoup.setBackOff(Duration.ofMillis(0));
            var minkabu = new RestClientProperties.Settings();
            minkabu.setBaseUri("https://minkabu.jp");

            var properties = new RestClientProperties(Map.of("jsoup", jsoup, "minkabu", minkabu));
            client = spy(new JsoupClient(
                    properties,
                    Mockito.spy(new AppConfig().restTemplateJsoup(properties)),
                    retryTemplate,
                    circuitBreakerRegistry,
                    rateLimiterRegistry
            ));

            var code = "9434";
            var actual = client.isLivedCompanyFromMinkabu(code);

            assertTrue(actual);
        }

        @DisplayName("isLivedCompanyFromMinkabu : みんかぶの会社コードによる上場状況を取得する")
        @Test
        void isLivedCompanyFromMinkabu_ok() throws IOException {
            var code = "9999";
            var htmlFile = new File("src/test/resources/github/com/ioridazo/fundanalyzer/client/jsoup/isLivedCompanyFromMinkabu/minkabu_ok.html");
            doReturn(jsoupParser(htmlFile)).when(client).getForHtml(any(), any(), any());

            assertTrue(client.isLivedCompanyFromMinkabu(code));
        }

        @DisplayName("isLivedCompanyFromMinkabu : みんかぶの会社コードによる上場状況を取得する")
        @Test
        void isLivedCompanyFromMinkabu_ng() throws IOException {
            var code = "9999";
            var htmlFile = new File("src/test/resources/github/com/ioridazo/fundanalyzer/client/jsoup/isLivedCompanyFromMinkabu/minkabu_ng.html");
            doReturn(jsoupParser(htmlFile)).when(client).getForHtml(any(), any(), any());

            assertFalse(client.isLivedCompanyFromMinkabu(code));
        }
    }

    @Nested
    class minkabuForStock {

        @DisplayName("minkabuForStock : 実際にminkabuの会社コードによる株価情報を取得する")
            // @Test
        void minkabuForStock_test() {
            var jsoup = new RestClientProperties.Settings();
            jsoup.setConnectTimeout(Duration.ofMillis(10000));
            jsoup.setReadTimeout(Duration.ofMillis(10000));
            jsoup.setMaxAttempts(2);
            jsoup.setBackOff(Duration.ofMillis(0));
            var minkabu = new RestClientProperties.Settings();
            minkabu.setBaseUri("https://minkabu.jp");

            var properties = new RestClientProperties(Map.of("jsoup", jsoup, "minkabu", minkabu));
            client = spy(new JsoupClient(
                    properties,
                    Mockito.spy(new AppConfig().restTemplateJsoup(properties)),
                    retryTemplate,
                    circuitBreakerRegistry,
                    rateLimiterRegistry
            ));

            var code = "9434";
            var actual = client.minkabuForStock(code);

            assertNotNull(actual);
            actual.forEach(System.out::println);
            assertEquals(20, actual.size());
        }

        @DisplayName("minkabuForStock : minkabuの会社コードによる株価情報を取得する")
        @Test
        void minkabuForStock_ok() throws IOException {
            var code = "9999";
            var htmlFile = new File("src/test/resources/github/com/ioridazo/fundanalyzer/client/jsoup/minkabu/minkabuForStock.html");
            doReturn(jsoupParser(htmlFile)).when(client).getForHtml(any(), any(), any());
            var actual = client.minkabuForStock(code);

            assertAll(
                    () -> assertAll(
                            () -> assertEquals("2022/12/09", actual.get(0).targetDate()),
                            () -> assertEquals("1,468.0", actual.get(0).openingPrice()),
                            () -> assertEquals("1,487.5", actual.get(0).highPrice()),
                            () -> assertEquals("1,467.5", actual.get(0).lowPrice()),
                            () -> assertEquals("1,481.5", actual.get(0).closingPrice()),
                            () -> assertEquals("7,594,600", actual.get(0).volume()),
                            () -> assertEquals("1,481.5", actual.get(0).closingPriceAdjustment())
                    ),
                    () -> assertAll(
                            () -> assertEquals("2022/12/08", actual.get(1).targetDate()),
                            () -> assertEquals("1,469.5", actual.get(1).openingPrice()),
                            () -> assertEquals("1,476.5", actual.get(1).highPrice()),
                            () -> assertEquals("1,466.0", actual.get(1).lowPrice()),
                            () -> assertEquals("1,473.0", actual.get(1).closingPrice()),
                            () -> assertEquals("7,023,000", actual.get(1).volume()),
                            () -> assertEquals("1,473.0", actual.get(1).closingPriceAdjustment())
                    ),
                    () -> assertAll(
                            () -> assertEquals("2022/11/11", actual.get(19).targetDate()),
                            () -> assertEquals("1,492.5", actual.get(19).openingPrice()),
                            () -> assertEquals("1,493.0", actual.get(19).highPrice()),
                            () -> assertEquals("1,480.5", actual.get(19).lowPrice()),
                            () -> assertEquals("1,486.0", actual.get(19).closingPrice()),
                            () -> assertEquals("7,404,600", actual.get(19).volume()),
                            () -> assertEquals("1,486.0", actual.get(19).closingPriceAdjustment())
                    )
            );
            assertEquals(20, actual.size());
        }
    }

    @Nested
    class yahooFinance {

        @DisplayName("yahoo-finance : 実際にyahoo-financeの会社コードによる株価情報を取得する")
            // @Test
        void yahooFinance_test() {
            var jsoup = new RestClientProperties.Settings();
            jsoup.setConnectTimeout(Duration.ofMillis(1000));
            jsoup.setReadTimeout(Duration.ofMillis(1000));
            jsoup.setMaxAttempts(2);
            jsoup.setBackOff(Duration.ofMillis(1000));
            var yahooFinance = new RestClientProperties.Settings();
            yahooFinance.setBaseUri("https://finance.yahoo.co.jp");

            var properties = new RestClientProperties(Map.of("jsoup", jsoup, "yahoo-finance", yahooFinance));
            client = spy(new JsoupClient(
                    properties,
                    Mockito.spy(new AppConfig().restTemplateJsoup(properties)),
                    retryTemplate,
                    circuitBreakerRegistry,
                    rateLimiterRegistry
            ));

            var code = "9434";
            var actual = client.yahooFinance(code);

            assertNotNull(actual);
            actual.forEach(System.out::println);
        }

        @DisplayName("yahoo-finance : yahoo-financeの会社コードによる株価情報を取得する")
        @Test
        void yahooFinance_ok() throws IOException {
            var code = "9999";
            var htmlFile = new File("src/test/resources/github/com/ioridazo/fundanalyzer/client/jsoup/yahoo-finance/yahoo-finance.html");
            doReturn(jsoupParser(htmlFile)).when(client).getForHtml(any(), any(), any());
            var actual = client.yahooFinance(code);

            actual.forEach(System.out::println);
            assertAll("YahooFinanceResultBean",
                    () -> assertAll(
                            () -> assertEquals("2022年4月27日", actual.get(0).targetDate()),
                            () -> assertEquals("1,484.5", actual.get(0).openingPrice()),
                            () -> assertEquals("1,503", actual.get(0).highPrice()),
                            () -> assertEquals("1,474", actual.get(0).lowPrice()),
                            () -> assertEquals("1,501", actual.get(0).closingPrice()),
                            () -> assertEquals("17,847,600", actual.get(0).volume()),
                            () -> assertEquals("1,501", actual.get(0).closingPriceAdjustment())
                    ),
                    () -> assertAll(
                            () -> assertEquals("2022年4月26日", actual.get(1).targetDate()),
                            () -> assertEquals("1,487.5", actual.get(1).openingPrice()),
                            () -> assertEquals("1,489", actual.get(1).highPrice()),
                            () -> assertEquals("1,479", actual.get(1).lowPrice()),
                            () -> assertEquals("1,480.5", actual.get(1).closingPrice()),
                            () -> assertEquals("6,474,600", actual.get(1).volume()),
                            () -> assertEquals("1,480.5", actual.get(1).closingPriceAdjustment())
                    ),
                    () -> assertAll(
                            () -> assertEquals("2022年3月31日", actual.get(19).targetDate()),
                            () -> assertEquals("1,432", actual.get(19).openingPrice()),
                            () -> assertEquals("1,440.5", actual.get(19).highPrice()),
                            () -> assertEquals("1,425.5", actual.get(19).lowPrice()),
                            () -> assertEquals("1,428", actual.get(19).closingPrice()),
                            () -> assertEquals("13,433,600", actual.get(19).volume()),
                            () -> assertEquals("1,428", actual.get(19).closingPriceAdjustment())
                    )
            );
            assertEquals(20, actual.size());
        }
    }

    @Nested
    class getForHtml {

        @DisplayName("nikkei : リクエスト内容を確認する")
        @Test
        void nikkei() throws InterruptedException {
            var code = "9999";
            server.enqueue(new MockResponse().setResponseCode(200).setBody("body"));

            assertDoesNotThrow(() -> client.nikkei(code));
            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/nkd/company/?scode=9999", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );
        }

        @DisplayName("kabuoji3 : リクエスト内容を確認する")
        @Test
        void kabuoji3() throws InterruptedException {
            var code = "9999";
            server.enqueue(new MockResponse().setResponseCode(200).setBody("body"));

            assertDoesNotThrow(() -> client.kabuoji3(code));
            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/stock/9999/", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );
        }

        @DisplayName("minkabu : リクエスト内容を確認する")
        @Test
        void minkabu() throws InterruptedException {
            var code = "9999";
            server.enqueue(new MockResponse().setResponseCode(200).setBody("body"));

            assertDoesNotThrow(() -> client.minkabu(code));
            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/stock/9999/analysis", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );
        }

        @DisplayName("getForHtml : レスポンスが空のとき")
        @Test
        void response_null() {
            var code = "9999";
            server.enqueue(new MockResponse().setResponseCode(200));
            server.enqueue(new MockResponse().setResponseCode(200));
            assertThrows(FundanalyzerScrapingException.class, () -> client.minkabu(code));
        }

        @DisplayName("getForHtml : HTTPステータス200以外のとき")
        @ParameterizedTest
        @ValueSource(ints = {400, 404, 500, 503})
        void http_status(int httpStatus) {
            var code = "9999";
            server.enqueue(new MockResponse().setResponseCode(httpStatus));
            server.enqueue(new MockResponse().setResponseCode(httpStatus));

            var actual = assertThrows(FundanalyzerCircuitBreakerRecordException.class, () -> client.nikkei(code));
            assertTrue(actual.getMessage().contains("から200以外のHTTPステータスコードが返却されました。"));
        }

        @DisplayName("getForHtml : タイムアウトが発生したとき")
        @Test
        void timeout() {
            var code = "9999";
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
            assertThrows(FundanalyzerCircuitBreakerRecordException.class, () -> client.minkabu(code));
        }

        @DisplayName("getForHtml : サーキットブレーカーがオープンすること")
        @Test
        void circuitBreaker_open() {
            circuitBreakerRegistry = new CircuitBreakerRegistry.Builder()
                    .withCircuitBreakerConfig(
                            new CircuitBreakerConfig.Builder()
                                    .failureRateThreshold(100)
                                    .permittedNumberOfCallsInHalfOpenState(1)
                                    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                                    .slidingWindowSize(2)
                                    .recordException(new JsoupClient.RecordFailurePredicate())
                                    .build()
                    )
                    .build();
            client = spy(new JsoupClient(
                    properties(),
                    restTemplate,
                    retryTemplate,
                    circuitBreakerRegistry,
                    rateLimiterRegistry
            ));

            var code = "9999";

            server.enqueue(new MockResponse().setResponseCode(404));
            server.enqueue(new MockResponse().setResponseCode(404));

            var actual1 = assertThrows(FundanalyzerCircuitBreakerRecordException.class, () -> client.kabuoji3(code));
            assertTrue(actual1.getMessage().contains("から200以外のHTTPステータスコードが返却されました。"));
            assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker("kabuoji3").getState().name());

            var actual2 = assertThrows(FundanalyzerShortCircuitException.class, () -> client.kabuoji3(code));
            assertTrue(actual2.getMessage().contains("との通信でサーキットブレーカーがオープンしました。"));
            assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker("kabuoji3").getState().name());

            verify(restTemplate, times(2)).getForObject(anyString(), any());
        }

        @DisplayName("getForHtml : サーキットブレーカーがオープンしないこと")
        @Test
        void circuitBreaker_closed() {
            circuitBreakerRegistry = new CircuitBreakerRegistry.Builder()
                    .withCircuitBreakerConfig(
                            new CircuitBreakerConfig.Builder()
                                    .failureRateThreshold(100)
                                    .permittedNumberOfCallsInHalfOpenState(1)
                                    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                                    .slidingWindowSize(2)
                                    .recordException(new JsoupClient.RecordFailurePredicate())
                                    .build()
                    )
                    .build();
            client = spy(new JsoupClient(
                    properties(),
                    restTemplate,
                    retryTemplate,
                    circuitBreakerRegistry,
                    rateLimiterRegistry
            ));

            var code = "9999";

            server.enqueue(new MockResponse().setResponseCode(200));
            server.enqueue(new MockResponse().setResponseCode(200));
            server.enqueue(new MockResponse().setResponseCode(200));
            server.enqueue(new MockResponse().setResponseCode(200));

            var actual1 = assertThrows(FundanalyzerScrapingException.class, () -> client.kabuoji3(code));
            assertTrue(actual1.getMessage().contains("からHTMLを取得できませんでした。"));
            assertEquals("CLOSED", circuitBreakerRegistry.circuitBreaker("kabuoji3").getState().name());

            var actual2 = assertThrows(FundanalyzerScrapingException.class, () -> client.kabuoji3(code));
            assertTrue(actual2.getMessage().contains("からHTMLを取得できませんでした。"));
            assertEquals("CLOSED", circuitBreakerRegistry.circuitBreaker("kabuoji3").getState().name());

            verify(restTemplate, times(4)).getForObject(anyString(), any());
        }

        @DisplayName("getForHtml : レートリミッターが作動すること")
        @Test
        void rateLimiter_do() {
            rateLimiterRegistry = new RateLimiterRegistry.Builder()
                    .withRateLimiterConfig(
                            new RateLimiterConfig.Builder()
                                    .limitRefreshPeriod(Duration.ofMillis(1000))
                                    .limitForPeriod(1)
                                    .timeoutDuration(Duration.ofMillis(0))
                                    .build()
                    )
                    .build();
            client = spy(new JsoupClient(
                    properties(),
                    restTemplate,
                    retryTemplate,
                    circuitBreakerRegistry,
                    rateLimiterRegistry
            ));

            var code = "9999";

            server.enqueue(new MockResponse().setResponseCode(200));

            var actual = assertThrows(FundanalyzerRateLimiterException.class, () -> client.yahooFinance(code));
            assertTrue(actual.getMessage().contains("との通信でレートリミッターが作動しました。"));
            assertEquals("yahoo-finance", rateLimiterRegistry.rateLimiter("yahoo-finance").getName());

            verify(restTemplate, times(1)).getForObject(anyString(), any());
        }

        @DisplayName("getForHtml : レートリミッターが作動しないこと")
        @Test
        void rateLimiter_dont() throws IOException {
            rateLimiterRegistry = new RateLimiterRegistry.Builder()
                    .withRateLimiterConfig(
                            new RateLimiterConfig.Builder()
                                    .limitRefreshPeriod(Duration.ofMillis(1000))
                                    .limitForPeriod(10)
                                    .timeoutDuration(Duration.ofMillis(0))
                                    .build()
                    )
                    .build();
            client = spy(new JsoupClient(
                    properties(),
                    restTemplate,
                    retryTemplate,
                    circuitBreakerRegistry,
                    rateLimiterRegistry
            ));

            var code = "9999";

            var htmlFile = new File("src/test/resources/github/com/ioridazo/fundanalyzer/client/jsoup/yahoo-finance/yahoo-finance.html");
            doReturn(jsoupParser(htmlFile)).when(client).getForHtml(any(), any(), any());

            assertDoesNotThrow(() -> client.yahooFinance(code));
        }

        @DisplayName("getForHtml : 通信をリトライする")
        @Test
        void retryable() {
            var code = "9999";
            assertThrows(FundanalyzerCircuitBreakerRecordException.class, () -> client.kabuoji3(code));
            verify(restTemplate, times(2)).getForObject(anyString(), any());
            assertEquals(2, circuitBreakerRegistry.circuitBreaker("kabuoji3").getMetrics().getNumberOfFailedCalls());
        }
    }
}