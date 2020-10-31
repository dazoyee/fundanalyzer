package github.com.ioridazo.fundanalyzer.slack;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SlackProxyTest {

    private static MockWebServer server;

    private SlackProxy proxy;

    @BeforeEach
    void before() throws IOException {
        server = new MockWebServer();
        server.start();

        this.proxy = Mockito.spy(new SlackProxy(String.format("http://localhost:%s/test-slack", server.getPort())));

        Mockito.clearInvocations(proxy);
        Mockito.reset(proxy);
    }

    @AfterEach
    void after() throws IOException {
        server.shutdown();
    }

    @Test
    void sendMessage() throws InterruptedException {
        final var message = "message";

        server.enqueue(new MockResponse());

        assertDoesNotThrow(() -> proxy.sendMessage(message));

        final var recordedRequest = server.takeRequest();
        assertAll("request",
                () -> assertEquals("/test-slack", recordedRequest.getPath()),
                () -> assertEquals("POST", recordedRequest.getMethod()),
                () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, recordedRequest.getHeader("Content-Type")),
                () -> assertEquals("{\"text\": \"message\"}", recordedRequest.getBody().readUtf8())
        );
    }
}