package github.com.ioridazo.fundanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(
            final RestTemplateBuilder builder,
            @Value("${app.api.edinet}") final String rootUri) {
        return builder
                .rootUri(rootUri)
                .build();
    }

    @Bean
    public Executor executor() {
        final var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setQueueCapacity(50);
        executor.setMaxPoolSize(500);
        executor.initialize();
        return executor;
    }

}
