package github.com.ioridazo.fundanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(
            final RestTemplateBuilder builder,
            @Value("${edinet.api}") final String rootUri
    ) {
        return builder
                .rootUri(rootUri)
                .build();
    }
}
