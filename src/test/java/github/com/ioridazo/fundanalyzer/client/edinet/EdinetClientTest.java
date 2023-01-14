package github.com.ioridazo.fundanalyzer.client.edinet;

import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.AcquisitionType;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.ListType;
import github.com.ioridazo.fundanalyzer.config.AppConfig;
import github.com.ioridazo.fundanalyzer.config.RestClientProperties;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Timeout(10)
class EdinetClientTest {

    private static final String CIRCUIT_BREAKER_EDINET = "edinet";

    private static MockWebServer server;
    private RestTemplate restTemplate;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private EdinetClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        this.restTemplate = Mockito.spy(new AppConfig().restTemplateEdinet(properties()));
        this.circuitBreakerRegistry = new CircuitBreakerRegistry.Builder().build();
        this.client = Mockito.spy(new EdinetClient(
                restTemplate,
                new AppConfig().retryTemplateEdinet(properties()),
                circuitBreakerRegistry
        ));

        Mockito.clearInvocations(client);
        Mockito.reset(client);
    }

    @AfterEach
    void after() throws IOException {
        server.shutdown();
    }

    @Nested
    class list {

        @DisplayName("list : EDINETの書類一覧APIでパラメータのみを取得する")
        @Test
        void list_ok_DEFAULT() throws InterruptedException {
            var parameter = new ListRequestParameter(LocalDate.parse("2019-04-01"), ListType.DEFAULT);

            var json = "" +
                    "{" +
                    "    \"metadata\":" +
                    "        {" +
                    "            \"title\": \"提出された書類を把握するための API\"," +
                    "            \"parameter\":" +
                    "                {" +
                    "                    \"date\": \"2019-04-01\"," +
                    "                    \"type\": \"1\"" +
                    "                }," +
                    "            \"resultset\":" +
                    "                {" +
                    "                    \"count\": 1" +
                    "                }," +
                    "            \"processDateTime\": \"2019-04-01 13:01\"," +
                    "            \"status\": \"200\"," +
                    "            \"message\": \"OK\"" +
                    "        }" +
                    "}";

            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json; charset=utf-8")
                    .setBody(json));

            var actual = assertDoesNotThrow(() -> client.list(parameter));

            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/api/v1/documents.json?date=2019-04-01&type=1", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );

            assertAll("EdinetResponse",
                    () -> assertAll("Metadata",
                            () -> assertEquals("提出された書類を把握するための API", actual.getMetadata().getTitle()),
                            () -> assertAll("Parameter",
                                    () -> assertEquals("2019-04-01", actual.getMetadata().getParameter().getDate()),
                                    () -> assertEquals("1", actual.getMetadata().getParameter().getType())
                            ),
                            () -> assertAll("ResultSet",
                                    () -> assertEquals("1", actual.getMetadata().getResultset().getCount())
                            ),
                            () -> assertEquals("2019-04-01 13:01", actual.getMetadata().getProcessDateTime()),
                            () -> assertEquals("200", actual.getMetadata().getStatus()),
                            () -> assertEquals("OK", actual.getMetadata().getMessage())
                    )
            );
        }

        @DisplayName("list : EDINETの書類一覧APIで提出書類一覧を取得する")
        @Test
        void list_ok_GET_LIST() throws InterruptedException {
            var parameter = new ListRequestParameter(LocalDate.parse("2019-04-01"), ListType.GET_LIST);

            var json = "" +
                    "{" +
                    "    \"metadata\":" +
                    "        {" +
                    "            \"title\": \"提出された書類を把握するための API\"," +
                    "            \"parameter\":" +
                    "                {" +
                    "                    \"date\": \"2019-04-01\"," +
                    "                    \"type\": \"2\"" +
                    "                }," +
                    "            \"resultset\":" +
                    "                {" +
                    "                    \"count\": 2" +
                    "                }," +
                    "            \"processDateTime\": \"2019-04-01 13:01\"," +
                    "            \"status\": \"200\"," +
                    "            \"message\": \"OK\"" +
                    "        }," +
                    "    \"results\": [" +
                    "        {" +
                    "            \"seqNumber\": 1," +
                    "            \"docID\": \"S1000001\"," +
                    "            \"edinetCode\": \"E10001\"," +
                    "            \"secCode\": \"10000\"," +
                    "            \"JCN\": \"6000012010023\"," +
                    "            \"filerName\": \"エディネット株式会社\"," +
                    "            \"fundCode\": \"G00001\"," +
                    "            \"ordinanceCode\": \"030\"," +
                    "            \"formCode\": \"04A000\"," +
                    "            \"docTypeCode\": \"030\"," +
                    "            \"periodStart\": \"2019-04-01\", " +
                    "            \"periodEnd\": \"2020-03-31\"," +
                    "            \"submitDateTime\": \"2019-04-01 12:34\"," +
                    "            \"docDescription\": \"有価証券届出書（内国投資信託受益証券）\"," +
                    "            \"issuerEdinetCode\": null," +
                    "            \"subjectEdinetCode\": null," +
                    "            \"subsidiaryEdinetCode\": null," +
                    "            \"currentReportReason\": null," +
                    "            \"parentDocID\": null," +
                    "            \"opeDateTime\": null," +
                    "            \"withdrawalStatus\": \"0\"," +
                    "            \"docInfoEditStatus\": \"0\"," +
                    "            \"disclosureStatus\": \"0\"," +
                    "            \"xbrlFlag\": \"1\"," +
                    "            \"pdfFlag\": \"1\"," +
                    "            \"attachDocFlag\": \"1\"," +
                    "            \"englishDocFlag\": \"0\"" +
                    "        }" +
                    "    ]" +
                    "}";

            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json; charset=utf-8")
                    .setBody(json));

            var actual = assertDoesNotThrow(() -> client.list(parameter));

            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/api/v1/documents.json?date=2019-04-01&type=2", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );

            assertAll("EdinetResponse",
                    () -> assertAll("Metadata",
                            () -> assertEquals("提出された書類を把握するための API", actual.getMetadata().getTitle()),
                            () -> assertAll("Parameter",
                                    () -> assertEquals("2019-04-01", actual.getMetadata().getParameter().getDate()),
                                    () -> assertEquals("2", actual.getMetadata().getParameter().getType())
                            ),
                            () -> assertAll("ResultSet",
                                    () -> assertEquals("2", actual.getMetadata().getResultset().getCount())
                            ),
                            () -> assertEquals("2019-04-01 13:01", actual.getMetadata().getProcessDateTime()),
                            () -> assertEquals("200", actual.getMetadata().getStatus()),
                            () -> assertEquals("OK", actual.getMetadata().getMessage())
                    ),
                    () -> assertAll("",
                            () -> assertEquals("1", actual.getResults().get(0).getSeqNumber()),
                            () -> assertEquals("S1000001", actual.getResults().get(0).getDocId()),
                            () -> assertEquals("E10001", actual.getResults().get(0).getEdinetCode().orElseThrow()),
                            () -> assertEquals("10000", actual.getResults().get(0).getSecCode()),
                            () -> assertEquals("6000012010023", actual.getResults().get(0).getJcn()),
                            () -> assertEquals("エディネット株式会社", actual.getResults().get(0).getFilerName()),
                            () -> assertEquals("G00001", actual.getResults().get(0).getFundCode()),
                            () -> assertEquals("030", actual.getResults().get(0).getOrdinanceCode()),
                            () -> assertEquals("04A000", actual.getResults().get(0).getFormCode()),
                            () -> assertEquals("030", actual.getResults().get(0).getDocTypeCode().orElseThrow()),
                            () -> assertEquals("2019-04-01", actual.getResults().get(0).getPeriodStart()),
                            () -> assertEquals("2020-03-31", actual.getResults().get(0).getPeriodEnd().orElseThrow()),
                            () -> assertEquals("2019-04-01 12:34", actual.getResults().get(0).getSubmitDateTime()),
                            () -> assertEquals("有価証券届出書（内国投資信託受益証券）", actual.getResults().get(0).getDocDescription()),
                            () -> assertNull(actual.getResults().get(0).getIssuerEdinetCode()),
                            () -> assertNull(actual.getResults().get(0).getSubjectEdinetCode()),
                            () -> assertNull(actual.getResults().get(0).getSubsidiaryEdinetCode()),
                            () -> assertNull(actual.getResults().get(0).getCurrentReportReason()),
                            () -> assertNull(actual.getResults().get(0).getParentDocID().orElse(null)),
                            () -> assertNull(actual.getResults().get(0).getOpeDateTime()),
                            () -> assertEquals("0", actual.getResults().get(0).getWithdrawalStatus()),
                            () -> assertEquals("0", actual.getResults().get(0).getDocInfoEditStatus()),
                            () -> assertEquals("0", actual.getResults().get(0).getDisclosureStatus()),
                            () -> assertEquals("1", actual.getResults().get(0).getXbrlFlag()),
                            () -> assertEquals("1", actual.getResults().get(0).getPdfFlag()),
                            () -> assertEquals("1", actual.getResults().get(0).getAttachDocFlag()),
                            () -> assertEquals("0", actual.getResults().get(0).getEnglishDocFlag())
                    )
            );
        }

        @DisplayName("list : EDINETの書類一覧APIでエラーが発生したときの挙動を確認する")
        @ParameterizedTest
        @ValueSource(ints = {400, 404, 500, 503})
        void list_FundanalyzerRestClientException(int httpStatus) throws InterruptedException {
            var parameter = new ListRequestParameter(LocalDate.parse("2019-04-01"), ListType.DEFAULT);

            server.enqueue(new MockResponse().setResponseCode(httpStatus));

            var actual = assertThrows(FundanalyzerRestClientException.class, () -> client.list(parameter));

            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/api/v1/documents.json?date=2019-04-01&type=1", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );

            System.out.println(actual.getMessage());
        }

        @Nested
        class circuitBreaker {

            private RestTemplate restTemplate;
            private CircuitBreakerRegistry circuitBreakerRegistry;
            private EdinetClient client;

            @BeforeEach
            void setUp() throws IOException {
                server = new MockWebServer();
                server.start();

                this.restTemplate = Mockito.spy(new AppConfig().restTemplateEdinet(properties()));
                this.circuitBreakerRegistry = new CircuitBreakerRegistry.Builder()
                        .withCircuitBreakerConfig(
                                new CircuitBreakerConfig.Builder()
                                        .failureRateThreshold(100)
                                        .permittedNumberOfCallsInHalfOpenState(1)
                                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                                        .slidingWindowSize(2)
                                        .recordException(new EdinetClient.RecordFailurePredicate())
                                        .build()
                        )
                        .build();
                this.client = Mockito.spy(new EdinetClient(
                        restTemplate,
                        new AppConfig().retryTemplateEdinet(properties()),
                        circuitBreakerRegistry
                ));

                Mockito.clearInvocations(client);
                Mockito.reset(client);
            }

            @DisplayName("list : HTTPステータス404のときにサーキットブレーカーがオープンすること")
            @Test
            void not_found() {
                var parameter = new ListRequestParameter(LocalDate.parse("2019-04-01"), ListType.DEFAULT);

                server.enqueue(new MockResponse().setResponseCode(404));
                server.enqueue(new MockResponse().setResponseCode(404));

                var actual1 = assertThrows(FundanalyzerRestClientException.class, () -> client.list(parameter));
                assertTrue(Objects.requireNonNull(actual1.getMessage()).contains("データが取得できません。パラメータの設定値を見直してください。"));
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                var actual2 = assertThrows(FundanalyzerRestClientException.class, () -> client.list(parameter));
                assertEquals("edinetとの通信でサーキットブレーカーがオープンしました。", actual2.getMessage());
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                verify(restTemplate, times(2)).getForObject(anyString(), any(), anyMap());
            }

            @DisplayName("list : HTTPステータス500のときにサーキットブレーカーがオープンすること")
            @Test
            void internal_server_error() {
                var parameter = new ListRequestParameter(LocalDate.parse("2019-04-01"), ListType.DEFAULT);

                server.enqueue(new MockResponse().setResponseCode(500));
                server.enqueue(new MockResponse().setResponseCode(500));

                var actual1 = assertThrows(FundanalyzerRestClientException.class, () -> client.list(parameter));
                assertTrue(Objects.requireNonNull(actual1.getMessage()).contains("EDINET のトップページ又は金融庁ウェブサイトの各種情報検索サービスにてメンテナンス等の情報を確認してください。"));
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                var actual2 = assertThrows(FundanalyzerRestClientException.class, () -> client.list(parameter));
                assertEquals("edinetとの通信でサーキットブレーカーがオープンしました。", actual2.getMessage());
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                verify(restTemplate, times(2)).getForObject(anyString(), any(), anyMap());
            }

            @DisplayName("list : HTTPステータス502のときにサーキットブレーカーがオープンすること")
            @Test
            void other_error() {
                var parameter = new ListRequestParameter(LocalDate.parse("2019-04-01"), ListType.DEFAULT);

                server.enqueue(new MockResponse().setResponseCode(502));
                server.enqueue(new MockResponse().setResponseCode(502));

                var actual1 = assertThrows(FundanalyzerRestClientException.class, () -> client.list(parameter));
                assertTrue(Objects.requireNonNull(actual1.getMessage()).contains("EDINET API仕様書に規定されていないHTTPステータスコードが返却されました。スタックトレースを参考に詳細を確認してください。"));
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                var actual2 = assertThrows(FundanalyzerRestClientException.class, () -> client.list(parameter));
                assertEquals("edinetとの通信でサーキットブレーカーがオープンしました。", actual2.getMessage());
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                verify(restTemplate, times(2)).getForObject(anyString(), any(), anyMap());
            }

            @DisplayName("list : 通信タイムアウトのときにサーキットブレーカーがオープンすること")
            @Test
            void timeout() {
                var parameter = new ListRequestParameter(LocalDate.parse("2019-04-01"), ListType.DEFAULT);

                server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
                server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

                var actual1 = assertThrows(FundanalyzerRestClientException.class, () -> client.list(parameter));
                assertTrue(Objects.requireNonNull(actual1.getMessage()).contains("IO系のエラーにより、HTTP通信に失敗しました。スタックトレースを参考に原因を特定してください。"));
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                var actual2 = assertThrows(FundanalyzerRestClientException.class, () -> client.list(parameter));
                assertEquals("edinetとの通信でサーキットブレーカーがオープンしました。", actual2.getMessage());
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                verify(restTemplate, times(2)).getForObject(anyString(), any(), anyMap());
            }

            @DisplayName("list : サーキットブレーカーがオープンしないこと")
            @Test
            void bad_request() {
                var parameter = new ListRequestParameter(LocalDate.parse("2019-04-01"), ListType.DEFAULT);

                server.enqueue(new MockResponse().setResponseCode(400));
                server.enqueue(new MockResponse().setResponseCode(400));
                server.enqueue(new MockResponse().setResponseCode(400));
                server.enqueue(new MockResponse().setResponseCode(400));

                var actual1 = assertThrows(FundanalyzerRestClientException.class, () -> client.list(parameter));
                assertTrue(Objects.requireNonNull(actual1.getMessage()).contains("リクエスト内容が誤っています。リクエストの内容（エンドポイント、パラメータの形式等）を見直してください。"));
                assertEquals("CLOSED", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                var actual2 = assertThrows(FundanalyzerRestClientException.class, () -> client.list(parameter));
                assertTrue(Objects.requireNonNull(actual2.getMessage()).contains("リクエスト内容が誤っています。リクエストの内容（エンドポイント、パラメータの形式等）を見直してください。"));
                assertEquals("CLOSED", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                // オープンしないからリトライは4回
                verify(restTemplate, times(4)).getForObject(anyString(), any(), anyMap());
            }
        }

        @DisplayName("list : 通信をリトライする")
        @Test
        void retryable() throws InterruptedException {
            var parameter = new ListRequestParameter(LocalDate.parse("2019-04-01"), ListType.DEFAULT);

            assertThrows(FundanalyzerRestClientException.class, () -> client.list(parameter));

            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/api/v1/documents.json?date=2019-04-01&type=1", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );
            verify(restTemplate, times(2)).getForObject(anyString(), any(), anyMap());
            assertEquals(2, circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getMetrics().getNumberOfFailedCalls());
        }
    }

    @Nested
    class acquisition {

        @DisplayName("acquisition : EDINETの書類取得APIでファイルをダウンロードする")
        @Test
        void acquisition_ok() throws InterruptedException, IOException {
            var storagePath = new File("path1");
            var parameter = new AcquisitionRequestParameter("docId", AcquisitionType.DEFAULT);

            server.enqueue(new MockResponse().setResponseCode(200));
            doNothing().when(client).makeDirectory(storagePath);
            doReturn(null).when(client).copyFile(any(), any());

            assertDoesNotThrow(() -> client.acquisition(storagePath, parameter));

            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/api/v1/documents/docId?type=1", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );
        }

        @DisplayName("acquisition : EDINETの書類取得APIでエラーが発生したときの挙動を確認する")
        @ParameterizedTest
        @ValueSource(ints = {400, 404, 500, 503})
        void acquisition_FundanalyzerRestClientException(int httpStatus) throws InterruptedException, IOException {
            var storagePath = new File("path1");
            var parameter = new AcquisitionRequestParameter("docId", AcquisitionType.DEFAULT);

            server.enqueue(new MockResponse().setResponseCode(httpStatus));
            doNothing().when(client).makeDirectory(storagePath);
            doReturn(null).when(client).copyFile(any(), any());

            var actual = assertThrows(FundanalyzerRestClientException.class, () -> client.acquisition(storagePath, parameter));

            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/api/v1/documents/docId?type=1", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );

            System.out.println(actual.getMessage());
        }

        @Nested
        class circuitBreaker {

            private RestTemplate restTemplate;
            private CircuitBreakerRegistry circuitBreakerRegistry;
            private EdinetClient client;

            @BeforeEach
            void setUp() throws IOException {
                server = new MockWebServer();
                server.start();

                this.restTemplate = Mockito.spy(new AppConfig().restTemplateEdinet(properties()));
                this.circuitBreakerRegistry = new CircuitBreakerRegistry.Builder()
                        .withCircuitBreakerConfig(
                                new CircuitBreakerConfig.Builder()
                                        .failureRateThreshold(100)
                                        .permittedNumberOfCallsInHalfOpenState(1)
                                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                                        .slidingWindowSize(2)
                                        .recordException(new EdinetClient.RecordFailurePredicate())
                                        .build()
                        )
                        .build();
                this.client = Mockito.spy(new EdinetClient(
                        restTemplate,
                        new AppConfig().retryTemplateEdinet(properties()),
                        circuitBreakerRegistry
                ));

                Mockito.clearInvocations(client);
                Mockito.reset(client);
            }

            @DisplayName("acquisition : HTTPステータス404のときにサーキットブレーカーがオープンすること")
            @Test
            void not_found() {
                var storagePath = new File("path1");
                var parameter = new AcquisitionRequestParameter("docId", AcquisitionType.DEFAULT);

                server.enqueue(new MockResponse().setResponseCode(404));
                server.enqueue(new MockResponse().setResponseCode(404));

                var actual1 = assertThrows(FundanalyzerRestClientException.class, () -> client.acquisition(storagePath, parameter));
                assertTrue(Objects.requireNonNull(actual1.getMessage()).contains("データが取得できません。パラメータの設定値を見直してください。対象の書類が非開示となっている可能性があります。"));
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                var actual2 = assertThrows(FundanalyzerRestClientException.class, () -> client.acquisition(storagePath, parameter));
                assertEquals("edinetとの通信でサーキットブレーカーがオープンしました。", actual2.getMessage());
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                verify(restTemplate, times(2)).execute(anyString(), any(), any(), any(), anyMap());
            }

            @DisplayName("acquisition : HTTPステータス500のときにサーキットブレーカーがオープンすること")
            @Test
            void internal_server_error() {
                var storagePath = new File("path1");
                var parameter = new AcquisitionRequestParameter("docId", AcquisitionType.DEFAULT);

                server.enqueue(new MockResponse().setResponseCode(500));
                server.enqueue(new MockResponse().setResponseCode(500));

                var actual1 = assertThrows(FundanalyzerRestClientException.class, () -> client.acquisition(storagePath, parameter));
                assertTrue(Objects.requireNonNull(actual1.getMessage()).contains("EDINET のトップページ又は金融庁ウェブサイトの各種情報検索サービスにてメンテナンス等の情報を確認してください。"));
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                var actual2 = assertThrows(FundanalyzerRestClientException.class, () -> client.acquisition(storagePath, parameter));
                assertEquals("edinetとの通信でサーキットブレーカーがオープンしました。", actual2.getMessage());
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                verify(restTemplate, times(2)).execute(anyString(), any(), any(), any(), anyMap());
            }

            @DisplayName("acquisition : HTTPステータス502のときにサーキットブレーカーがオープンすること")
            @Test
            void other_error() {
                var storagePath = new File("path1");
                var parameter = new AcquisitionRequestParameter("docId", AcquisitionType.DEFAULT);

                server.enqueue(new MockResponse().setResponseCode(502));
                server.enqueue(new MockResponse().setResponseCode(502));

                var actual1 = assertThrows(FundanalyzerRestClientException.class, () -> client.acquisition(storagePath, parameter));
                assertTrue(Objects.requireNonNull(actual1.getMessage()).contains("EDINET API仕様書に規定されていないHTTPステータスコードが返却されました。スタックトレースを参考に詳細を確認してください。"));
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                var actual2 = assertThrows(FundanalyzerRestClientException.class, () -> client.acquisition(storagePath, parameter));
                assertEquals("edinetとの通信でサーキットブレーカーがオープンしました。", actual2.getMessage());
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                verify(restTemplate, times(2)).execute(anyString(), any(), any(), any(), anyMap());
            }

            @DisplayName("acquisition : 通信タイムアウトのときにサーキットブレーカーがオープンすること")
            @Test
            void timeout() {
                var storagePath = new File("path1");
                var parameter = new AcquisitionRequestParameter("docId", AcquisitionType.DEFAULT);

                server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
                server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

                var actual1 = assertThrows(FundanalyzerRestClientException.class, () -> client.acquisition(storagePath, parameter));
                assertTrue(Objects.requireNonNull(actual1.getMessage()).contains("IO系のエラーにより、HTTP通信に失敗しました。スタックトレースを参考に原因を特定してください。"));

                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                var actual2 = assertThrows(FundanalyzerRestClientException.class, () -> client.acquisition(storagePath, parameter));
                assertEquals("edinetとの通信でサーキットブレーカーがオープンしました。", actual2.getMessage());
                assertEquals("OPEN", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                verify(restTemplate, times(2)).execute(anyString(), any(), any(), any(), anyMap());
            }

            @DisplayName("acquisition : サーキットブレーカーがオープンしないこと")
            @Test
            void bad_request() {
                var storagePath = new File("path1");
                var parameter = new AcquisitionRequestParameter("docId", AcquisitionType.DEFAULT);

                server.enqueue(new MockResponse().setResponseCode(400));
                server.enqueue(new MockResponse().setResponseCode(400));
                server.enqueue(new MockResponse().setResponseCode(400));
                server.enqueue(new MockResponse().setResponseCode(400));

                var actual1 = assertThrows(FundanalyzerRestClientException.class, () -> client.acquisition(storagePath, parameter));
                assertTrue(Objects.requireNonNull(actual1.getMessage()).contains("リクエスト内容が誤っています。リクエストの内容（エンドポイント、パラメータの形式等）を見直してください。"));
                assertEquals("CLOSED", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                var actual2 = assertThrows(FundanalyzerRestClientException.class, () -> client.acquisition(storagePath, parameter));
                assertTrue(Objects.requireNonNull(actual2.getMessage()).contains("リクエスト内容が誤っています。リクエストの内容（エンドポイント、パラメータの形式等）を見直してください。"));
                assertEquals("CLOSED", circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getState().name());

                // オープンしないからリトライは4回
                verify(restTemplate, times(4)).execute(anyString(), any(), any(), any(), anyMap());
            }
        }

        @DisplayName("acquisition : 通信をリトライする")
        @Test
        void retryable() throws InterruptedException {
            var storagePath = new File("path1");
            var parameter = new AcquisitionRequestParameter("docId", AcquisitionType.DEFAULT);

            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

            assertThrows(FundanalyzerRestClientException.class, () -> client.acquisition(storagePath, parameter));

            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/api/v1/documents/docId?type=1", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );
            verify(restTemplate, times(2)).execute(anyString(), any(), any(), any(), anyMap());
            assertEquals(2, circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_EDINET).getMetrics().getNumberOfFailedCalls());
        }
    }

    private static RestClientProperties properties() {
        var edinet = new RestClientProperties.Settings();
        edinet.setBaseUri(String.format("http://localhost:%s", server.getPort()));
        edinet.setConnectTimeout(Duration.ofMillis(100));
        edinet.setReadTimeout(Duration.ofMillis(100));
        edinet.setMaxAttempts(2);
        edinet.setBackOff(Duration.ofMillis(1));
        return new RestClientProperties(Map.of(
                "edinet", edinet
        ));
    }
}