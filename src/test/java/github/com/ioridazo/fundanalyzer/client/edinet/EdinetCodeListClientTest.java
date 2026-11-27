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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(2000);
        final RestTemplate restTemplate = new RestTemplate(requestFactory);
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
            final byte[] body = new byte[]{0, 1, 2, 3, 4};
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
            final byte[] body = new byte[]{1, 2, 3};
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
    }
}
