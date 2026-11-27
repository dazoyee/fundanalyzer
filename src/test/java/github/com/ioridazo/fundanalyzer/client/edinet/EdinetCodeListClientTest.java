package github.com.ioridazo.fundanalyzer.client.edinet;

import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(10)
class EdinetCodeListClientTest {

    private MockWebServer server;
    private EdinetCodeListClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        final RestTemplate restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory(
                new okhttp3.OkHttpClient.Builder()
                        .followRedirects(false)
                        .followSslRedirects(false)
                        .connectTimeout(Duration.ofSeconds(2))
                        .readTimeout(Duration.ofSeconds(2))
                        .build()));
        restTemplate.setErrorHandler(new EdinetResponseErrorHandler());
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(server.url("/").toString()));
        client = new EdinetCodeListClient(restTemplate, RetryTemplate.builder().maxAttempts(3).fixedBackoff(1).build(),
                new CircuitBreakerRegistry.Builder().build(), new RateLimiterRegistry.Builder().build());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Nested
    class download {

        @Test
        void downloadsResponseBody(@TempDir final Path storagePath) throws InterruptedException, IOException {
            final byte[] body = new byte[]{'P', 'K', 3, 4, 0, 1, 2};
            server.enqueue(new MockResponse().setResponseCode(200).setBody(new okio.Buffer().write(body)));

            assertEquals("Edinetcode.zip", client.download(storagePath.toFile()));

            assertTrue(Files.exists(storagePath.resolve("Edinetcode.zip")));
            assertTrue(java.util.Arrays.equals(body, Files.readAllBytes(storagePath.resolve("Edinetcode.zip"))));
            assertEquals("/searchdocument/codelist/Edinetcode.zip", server.takeRequest().getPath());
        }

        @Test
        void overwritesExistingFile(@TempDir final Path storagePath) throws IOException {
            final File target = storagePath.resolve("Edinetcode.zip").toFile();
            Files.write(target.toPath(), new byte[]{9, 9});
            final byte[] body = new byte[]{'P', 'K', 3, 4, 1};
            server.enqueue(new MockResponse().setResponseCode(200).setBody(new okio.Buffer().write(body)));

            assertEquals("Edinetcode.zip", client.download(storagePath.toFile()));
            assertTrue(java.util.Arrays.equals(body, Files.readAllBytes(target.toPath())));
        }

        @Test
        void mapsNotFoundToFailure(@TempDir final Path storagePath) {
            server.enqueue(new MockResponse().setResponseCode(404));
            server.enqueue(new MockResponse().setResponseCode(404));
            server.enqueue(new MockResponse().setResponseCode(404));
            assertThrows(FundanalyzerRestClientException.class, () -> client.download(storagePath.toFile()));
            assertEquals(3, server.getRequestCount());
        }

        @Test
        void mapsServerErrorToFailure(@TempDir final Path storagePath) {
            server.enqueue(new MockResponse().setResponseCode(500));
            server.enqueue(new MockResponse().setResponseCode(500));
            server.enqueue(new MockResponse().setResponseCode(500));
            assertThrows(FundanalyzerRestClientException.class, () -> client.download(storagePath.toFile()));
            assertEquals(3, server.getRequestCount());
        }

        @Test
        void rejectsHtmlResponse(@TempDir final Path storagePath) {
            for (int i = 0; i < 3; i++) {
                server.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "text/html")
                        .setBody("<html>error</html>"));
            }
            final FundanalyzerRestClientException actual =
                    assertThrows(FundanalyzerRestClientException.class, () -> client.download(storagePath.toFile()));
            assertTrue(actual.getMessage().contains("Content-Type:text/html"));
            assertTrue(actual.getMessage().contains("<html>error</html>"));
            assertFalse(Files.exists(storagePath.resolve("Edinetcode.zip")));
            assertEquals(3, server.getRequestCount());
        }

        @Test
        void rejectsRedirectWithoutFollowing(@TempDir final Path storagePath) {
            for (int i = 0; i < 3; i++) {
                server.enqueue(new MockResponse().setResponseCode(301)
                        .setHeader("Location", server.url("/moved?Subscription-Key=secret").toString())
                        .setBody("moved to ?Subscription-Key=secret"));
            }
            final FundanalyzerRestClientException actual =
                    assertThrows(FundanalyzerRestClientException.class, () -> client.download(storagePath.toFile()));
            assertTrue(actual.getMessage().contains("移転先:" + server.url("/moved")));
            assertFalse(actual.getMessage().contains("secret"));
            assertEquals(3, server.getRequestCount());
        }
    }
}
