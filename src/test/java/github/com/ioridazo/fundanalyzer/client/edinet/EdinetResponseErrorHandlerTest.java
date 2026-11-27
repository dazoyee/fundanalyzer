package github.com.ioridazo.fundanalyzer.client.edinet;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(10)
class EdinetResponseErrorHandlerTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Nested
    class sanitizeLocation {

        @Test
        void removesQueryAndFragmentFromAbsoluteUrl() {
            final String actual = EdinetResponseErrorHandler.sanitizeLocation(
                    "https://user:password@example.com:8443/path?Subscription-Key=secret#fragment");

            assertEquals("https://example.com:8443/path", actual);
            assertFalse(actual.contains("secret"));
        }

        @Test
        void returnsRelativePath() {
            assertEquals("/redirect/path", EdinetResponseErrorHandler.sanitizeLocation("/redirect/path?key=secret#part"));
        }

        @Test
        void returnsNoneForNull() {
            assertEquals("(none)", EdinetResponseErrorHandler.sanitizeLocation(null));
        }
    }

    @Nested
    class request {

        @Test
        void treatsRedirectAsErrorWithoutFollowingIt() {
            server.enqueue(new MockResponse()
                    .setResponseCode(301)
                    .addHeader("Location", "/destination?Subscription-Key=secret"));
            final RestTemplate restTemplate = createRestTemplate();

            final RestClientResponseException exception = assertThrows(
                    RestClientResponseException.class,
                    () -> restTemplate.getForObject(server.url("/source").toString(), String.class));

            assertEquals(301, exception.getStatusCode().value());
            assertEquals(1, server.getRequestCount());
            assertTrue(exception.getMessage().contains("location=/destination"));
            assertFalse(exception.getMessage().contains("secret"));
        }

        @Test
        void returnsSuccessfulResponse() {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
            final RestTemplate restTemplate = createRestTemplate();

            assertEquals("ok", restTemplate.getForObject(server.url("/source").toString(), String.class));
            assertEquals(1, server.getRequestCount());
        }
    }

    private RestTemplate createRestTemplate() {
        final Supplier<ClientHttpRequestFactory> requestFactory = () -> new OkHttp3ClientHttpRequestFactory(
                new okhttp3.OkHttpClient.Builder()
                        .followRedirects(false)
                        .followSslRedirects(false)
                        .connectTimeout(Duration.ofSeconds(2))
                        .readTimeout(Duration.ofSeconds(2))
                        .build());
        final RestTemplate restTemplate = new org.springframework.boot.web.client.RestTemplateBuilder()
                .requestFactory(requestFactory)
                .errorHandler(new EdinetResponseErrorHandler())
                .build();
        return restTemplate;
    }
}
