package github.com.ioridazo.fundanalyzer.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties("app.config")
@Configuration
@AllArgsConstructor
@Data
public class RestClientProperties {

    private Map<String, Settings> restClient;

    @Data
    public static class Settings {

        private String baseUri;

        private Duration connectTimeout;

        private Duration readTimeout;

        private Integer maxAttempts;

        private Duration backOff;
    }
}
