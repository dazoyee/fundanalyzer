package github.com.ioridazo.fundanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(
            @Value("${app.config.rest-template.connect-timeout}") final int connectTimeout,
            @Value("${app.config.rest-template.connect-timeout}") final int readTimeout) {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

    @Bean
    public Executor executor(
            @Value("${app.config.executor.core-pool-size}") final int corePoolSize,
            @Value("${app.config.executor.queue-capacity}") final int queueCapacity,
            @Value("${app.config.executor.max-pool-size}") final int maxPoolSize) {
        final var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setMaxPoolSize(maxPoolSize);
        executor.initialize();
        return executor;
    }

}
