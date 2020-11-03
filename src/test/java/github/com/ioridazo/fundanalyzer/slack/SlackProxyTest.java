package github.com.ioridazo.fundanalyzer.slack;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

class SlackProxyTest {

    private static MockWebServer server;
    private Environment environment;
    private SlackProxy proxy;

    @BeforeEach
    void before() throws IOException {
        this.environment = Mockito.spy(Environment.class);
        server = new MockWebServer();
        server.start();

        this.proxy = Mockito.spy(new SlackProxy(
                String.format("http://localhost:%s", server.getPort()),
                environment
        ));
        proxy.parameterT = "t";
        proxy.parameterB = "b";
        proxy.parameterX = "x";

        Mockito.clearInvocations(proxy);
        Mockito.reset(proxy);
    }

    @AfterEach
    void after() throws IOException {
        server.shutdown();
    }

    @Disabled("テスト送信のため")
    @Test
    void sendMessage_tester() {
        var propertyPath = "property.path";

        this.proxy = new SlackProxy(
                "https://hooks.slack.com",
                environment
        );
        proxy.parameterT = "TKN2V6NQ4";
        proxy.parameterB = "B01DFHHPE07";
        proxy.parameterX = "";

        server.enqueue(new MockResponse());
        doReturn("*tester*").when(environment).getProperty(propertyPath);

        assertDoesNotThrow(() -> proxy.sendMessage(propertyPath));
    }

    @Test
    void sendMessage_no_parameter() throws InterruptedException {
        var propertyPath = "property.path";

        server.enqueue(new MockResponse());
        doReturn("*message*").when(environment).getProperty(propertyPath);

        assertDoesNotThrow(() -> proxy.sendMessage(propertyPath));

        var recordedRequest = server.takeRequest();
        assertAll("request",
                () -> assertEquals("/services/t/b/x", recordedRequest.getPath()),
                () -> assertEquals("POST", recordedRequest.getMethod()),
                () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader("Content-Type")),
                () -> assertEquals("{\"text\": \"*message*\"}", recordedRequest.getBody().readUtf8())
        );
    }

    @Test
    void sendMessage_arguments() throws InterruptedException {
        var propertyPath = "property.path";
        var arguments1 = "arguments1";
        var arguments2 = "arguments2";

        server.enqueue(new MockResponse());
        doReturn("*message, {0}, {1}*").when(environment).getProperty(propertyPath);
        doReturn("nowLocalDataTime").when(proxy).nowLocalDataTime();

        assertDoesNotThrow(() -> proxy.sendMessage(propertyPath, arguments1, arguments2));

        var recordedRequest = server.takeRequest();
        assertAll("request",
                () -> assertEquals("/services/t/b/x", recordedRequest.getPath()),
                () -> assertEquals("POST", recordedRequest.getMethod()),
                () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader("Content-Type")),
                () -> assertEquals("{\"text\": \"nowLocalDataTime\t*message, arguments1, arguments2*\"}", recordedRequest.getBody().readUtf8())
        );
    }
}