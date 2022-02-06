package github.com.ioridazo.fundanalyzer.client.slack;

import github.com.ioridazo.fundanalyzer.config.AppConfig;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

@Timeout(10)
class SlackClientTest {

    private static MockWebServer server;
    private RestTemplate restTemplate;
    private Environment environment;
    private SlackClient client;

    @BeforeEach
    void before() throws IOException {
        this.environment = Mockito.spy(Environment.class);
        server = new MockWebServer();
        server.start();

        this.restTemplate = Mockito.spy(new AppConfig().restTemplateSelenium(Duration.ofMillis(1), Duration.ofMillis(1)));
        this.client = Mockito.spy(new SlackClient(
                restTemplate,
                new AppConfig().retryTemplateSelenium(2, Duration.ofMillis(1)),
                String.format("http://localhost:%s", server.getPort()),
                environment
        ));
        client.parameterT = "t";
        client.parameterB = "b";
        client.parameterX = "x";

        Mockito.clearInvocations(client);
        Mockito.reset(client);
    }

    @AfterEach
    void after() throws IOException {
        server.shutdown();
    }

    @Disabled("テスト送信のため")
//    @Test
    void sendMessage_tester() {
        var propertyPath = "property.path";

        this.client = new SlackClient(
                restTemplate,
                new AppConfig().retryTemplateSelenium(2, Duration.ofSeconds(1)),
                "https://hooks.slack.com",
                environment
        );
        client.parameterT = "TKN2V6NQ4";
        client.parameterB = "B01DFHHPE07";
        client.parameterX = "";

        server.enqueue(new MockResponse());
        doReturn("*tester*").when(environment).getProperty(propertyPath);

        assertDoesNotThrow(() -> client.sendMessage(propertyPath));
    }

    @DisplayName("sendMessage : Slackにメッセージをそのまま通知する")
    @Test
    void sendMessage_no_parameter() throws InterruptedException {
        var propertyPath = "property.path";

        server.enqueue(new MockResponse());
        doReturn("*message*").when(environment).getProperty(propertyPath);

        client.sendMessage(propertyPath);
//        assertDoesNotThrow(() -> proxy.sendMessage(propertyPath));

        var recordedRequest = server.takeRequest();
        assertAll("request",
                () -> assertEquals("/services/t/b/x", recordedRequest.getPath()),
                () -> assertEquals("POST", recordedRequest.getMethod()),
                () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader("Content-Type")),
                () -> assertEquals("{\"text\": \"*message*\"}", recordedRequest.getBody().readUtf8())
        );
    }

    @DisplayName("sendMessage : Slackにメッセージをパラメータ付け加えて通知する")
    @Test
    void sendMessage_arguments() throws InterruptedException {
        var propertyPath = "property.path";
        var arguments1 = "arguments1";
        var arguments2 = "arguments2";

        server.enqueue(new MockResponse());
        doReturn("*message, {0}, {1}*").when(environment).getProperty(propertyPath);
        doReturn("nowLocalDataTime").when(client).nowLocalDataTime();

        assertDoesNotThrow(() -> client.sendMessage(propertyPath, arguments1, arguments2));

        var recordedRequest = server.takeRequest();
        assertAll("request",
                () -> assertEquals("/services/t/b/x", recordedRequest.getPath()),
                () -> assertEquals("POST", recordedRequest.getMethod()),
                () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader("Content-Type")),
                () -> assertEquals("{\"text\": \"nowLocalDataTime\t*message, arguments1, arguments2*\"}", recordedRequest.getBody().readUtf8())
        );
    }

    @DisplayName("sendMessage : Slackにメッセージをパラメータ付け加えて通知する")
    @ParameterizedTest
    @ValueSource(ints = {400, 404, 500, 503})
    void sendMessage_exception(int httpStatus) throws InterruptedException {
        var propertyPath = "property.path";

        server.enqueue(new MockResponse().setResponseCode(httpStatus));
        doReturn("*message*").when(environment).getProperty(propertyPath);

        var actual = assertThrows(FundanalyzerRestClientException.class, () -> client.sendMessage(propertyPath));

        var recordedRequest = server.takeRequest();
        assertAll("request",
                () -> assertEquals("/services/t/b/x", recordedRequest.getPath()),
                () -> assertEquals("POST", recordedRequest.getMethod()),
                () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader("Content-Type")),
                () -> assertEquals("{\"text\": \"*message*\"}", recordedRequest.getBody().readUtf8())
        );

        System.out.println(actual.getMessage());
    }
}