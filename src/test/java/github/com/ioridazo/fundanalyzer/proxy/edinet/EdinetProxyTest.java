package github.com.ioridazo.fundanalyzer.proxy.edinet;

import github.com.ioridazo.fundanalyzer.config.AppConfig;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request.AcquisitionType;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request.ListType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

@Timeout(10)
class EdinetProxyTest {

    private static MockWebServer server;
    private EdinetProxy proxy;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        this.proxy = Mockito.spy(new EdinetProxy(
                new AppConfig().restTemplate(2000, 2000),
                String.format("http://localhost:%s", server.getPort())
        ));

        Mockito.clearInvocations(proxy);
        Mockito.reset(proxy);
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
            var parameter = new ListRequestParameter("2019-04-01", ListType.DEFAULT);

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

            var actual = assertDoesNotThrow(() -> proxy.list(parameter));

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
            var parameter = new ListRequestParameter("2019-04-01", ListType.GET_LIST);

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

            var actual = assertDoesNotThrow(() -> proxy.list(parameter));

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
                            () -> assertEquals("030", actual.getResults().get(0).getDocTypeCode()),
                            () -> assertEquals("2019-04-01", actual.getResults().get(0).getPeriodStart()),
                            () -> assertEquals("2020-03-31", actual.getResults().get(0).getPeriodEnd()),
                            () -> assertEquals("2019-04-01 12:34", actual.getResults().get(0).getSubmitDateTime()),
                            () -> assertEquals("有価証券届出書（内国投資信託受益証券）", actual.getResults().get(0).getDocDescription()),
                            () -> assertNull(actual.getResults().get(0).getIssuerEdinetCode()),
                            () -> assertNull(actual.getResults().get(0).getSubjectEdinetCode()),
                            () -> assertNull(actual.getResults().get(0).getSubsidiaryEdinetCode()),
                            () -> assertNull(actual.getResults().get(0).getCurrentReportReason()),
                            () -> assertNull(actual.getResults().get(0).getParentDocID()),
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
            var parameter = new ListRequestParameter("2019-04-01", ListType.DEFAULT);

            server.enqueue(new MockResponse().setResponseCode(httpStatus));

            var actual = assertThrows(FundanalyzerRestClientException.class, () -> proxy.list(parameter));

            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/api/v1/documents.json?date=2019-04-01&type=1", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );

            System.out.println(actual.getMessage());
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
            doNothing().when(proxy).makeDirectory(storagePath);
            doReturn(null).when(proxy).copyFile(any(), any());

            assertDoesNotThrow(() -> proxy.acquisition(storagePath, parameter));

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
            doNothing().when(proxy).makeDirectory(storagePath);
            doReturn(null).when(proxy).copyFile(any(), any());

            var actual = assertThrows(FundanalyzerRestClientException.class, () -> proxy.acquisition(storagePath, parameter));

            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/api/v1/documents/docId?type=1", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );

            System.out.println(actual.getMessage());
        }
    }
}