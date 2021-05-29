package github.com.ioridazo.fundanalyzer.client.selenium;

import github.com.ioridazo.fundanalyzer.config.AppConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Timeout(10)
class SeleniumClientTest {

    private static MockWebServer server;
    private SeleniumClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        this.client = Mockito.spy(new SeleniumClient(new AppConfig().restTemplate(2000, 2000),
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
    }
}