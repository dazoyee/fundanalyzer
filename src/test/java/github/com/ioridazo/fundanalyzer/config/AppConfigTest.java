package github.com.ioridazo.fundanalyzer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppConfigTest {

    @Test
    @DisplayName("restTemplateJsoup は userAgent 設定があるとき default header に設定する")
    void restTemplateJsoup_setsUserAgentHeader() {
        final RestClientProperties.Settings jsoup = jsoupSettings();
        jsoup.setUserAgent("Mozilla/5.0 test");

        final var actual = new AppConfig().restTemplateJsoup(new RestClientProperties(Map.of("jsoup", jsoup)));
        final MockRestServiceServer server = MockRestServiceServer.bindTo(actual).build();

        server.expect(once(), requestTo("https://example.com/test"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.USER_AGENT, "Mozilla/5.0 test"))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        actual.getForEntity("https://example.com/test", String.class);

        server.verify();
    }

    @Test
    @DisplayName("restTemplateJsoup は userAgent 設定が空のとき default header を設定しない")
    void restTemplateJsoup_doesNotSetUserAgentHeaderWhenBlank() {
        final RestClientProperties.Settings jsoup = jsoupSettings();
        jsoup.setUserAgent(" ");

        final var actual = new AppConfig().restTemplateJsoup(new RestClientProperties(Map.of("jsoup", jsoup)));

        assertTrue(actual.getInterceptors().isEmpty());
    }

    private RestClientProperties.Settings jsoupSettings() {
        final RestClientProperties.Settings jsoup = new RestClientProperties.Settings();
        jsoup.setConnectTimeout(Duration.ofSeconds(1));
        jsoup.setReadTimeout(Duration.ofSeconds(1));
        jsoup.setMaxAttempts(1);
        jsoup.setBackOff(Duration.ZERO);
        return jsoup;
    }
}
