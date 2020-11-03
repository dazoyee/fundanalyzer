package github.com.ioridazo.fundanalyzer.slack;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
                String.format("http://localhost:%s/test-slack", server.getPort()),
                environment
        ));

        Mockito.clearInvocations(proxy);
        Mockito.reset(proxy);
    }

    @AfterEach
    void after() throws IOException {
        server.shutdown();
    }

    @Test
    void sendMessage_no_parameter() throws InterruptedException {
        var propertyPath = "property.path";

        server.enqueue(new MockResponse());
        doReturn("*message*").when(environment).getProperty(propertyPath);

        assertDoesNotThrow(() -> proxy.sendMessage(propertyPath));

        var recordedRequest = server.takeRequest();
        assertAll("request",
                () -> assertEquals("/test-slack", recordedRequest.getPath()),
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
                () -> assertEquals("/test-slack", recordedRequest.getPath()),
                () -> assertEquals("POST", recordedRequest.getMethod()),
                () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader("Content-Type")),
                () -> assertEquals("{\"text\": \"nowLocalDataTime\t*message, arguments1, arguments2*\"}", recordedRequest.getBody().readUtf8())
        );
    }
}