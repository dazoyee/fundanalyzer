package github.com.ioridazo.fundanalyzer.client.selenium;

import github.com.ioridazo.fundanalyzer.config.AppConfig;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Timeout(10)
@SuppressWarnings("NewClassNamingConvention")
class SeleniumClientTest {

    private static MockWebServer server;
    private RestTemplate restTemplate;
    private SeleniumClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        this.restTemplate = Mockito.spy(new AppConfig().restTemplateSelenium(Duration.ofMillis(1), Duration.ofMillis(1)));
        this.client = Mockito.spy(new SeleniumClient(
                restTemplate,
                new AppConfig().retryTemplateSelenium(2, Duration.ofMillis(1)),
                String.format("http://localhost:%s", server.getPort())
        ));

        Mockito.clearInvocations(client);
        Mockito.reset(client);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Nested
    class edinetCodeList {

        @DisplayName("edinetCodeList : Selenium通信する")
        @Test
        void edinetCodeList_ok() throws InterruptedException {
            var inputFilePath = "inputFilePath";

            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("EdinetcodeDInfo.zip"));

            var actual = assertDoesNotThrow(() -> client.edinetCodeList(inputFilePath));

            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/selenium/v1/edinetcode?path=inputFilePath", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );

            assertEquals("EdinetcodeDInfo.zip", actual);
        }

        @DisplayName("edinetCodeList : 通信をリトライする")
        @Test
        void retryable() throws InterruptedException {
            var inputFilePath = "inputFilePath";
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

            assertThrows(FundanalyzerRestClientException.class, () -> client.edinetCodeList(inputFilePath));

            var recordedRequest = server.takeRequest();
            assertAll("request",
                    () -> assertEquals("/selenium/v1/edinetcode?path=inputFilePath", recordedRequest.getPath()),
                    () -> assertEquals("GET", recordedRequest.getMethod())
            );
            verify(restTemplate, times(2)).getForObject(any(), any());
        }
    }
}