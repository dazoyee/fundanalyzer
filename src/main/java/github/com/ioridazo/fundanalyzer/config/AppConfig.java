package github.com.ioridazo.fundanalyzer.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AppConfig {

    private static final String REST = "rest-";
    private static final String RETRY = "retry-";
    private static final String EDINET = "edinet";
    private static final String SELENIUM = "selenium";
    private static final String SLACK = "slack";
    private static final String JSOUP = "jsoup";

    @Bean(REST + EDINET)
    public RestTemplate restTemplateEdinet(final RestClientProperties properties) {
        final RestClientProperties.Settings settings = properties.getRestClient().get(EDINET);
        final RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(settings.getConnectTimeout())
                .setReadTimeout(settings.getReadTimeout())
                .build();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(settings.getBaseUri()));
        return restTemplate;
    }

    @Bean(REST + SELENIUM)
    public RestTemplate restTemplateSelenium(final RestClientProperties properties) {
        final RestClientProperties.Settings settings = properties.getRestClient().get(SELENIUM);
        final RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(settings.getConnectTimeout())
                .setReadTimeout(settings.getReadTimeout())
                .build();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(settings.getBaseUri()));
        return restTemplate;
    }

    @Bean(REST + SLACK)
    public RestTemplate restTemplateSlack(final RestClientProperties properties) {
        final RestClientProperties.Settings settings = properties.getRestClient().get(SLACK);
        final RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(settings.getConnectTimeout())
                .setReadTimeout(settings.getReadTimeout())
                .build();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(settings.getBaseUri()));
        return restTemplate;
    }

    @Bean(REST + JSOUP)
    public RestTemplate restTemplateJsoup(final RestClientProperties properties) {
        final RestClientProperties.Settings settings = properties.getRestClient().get(JSOUP);
        return new RestTemplateBuilder()
                .setConnectTimeout(settings.getConnectTimeout())
                .setReadTimeout(settings.getReadTimeout())
                .build();
    }

    @Bean(RETRY + EDINET)
    public RetryTemplate retryTemplateEdinet(final RestClientProperties properties) {
        final RestClientProperties.Settings settings = properties.getRestClient().get(EDINET);
        return new RetryTemplateBuilder()
                .maxAttempts(settings.getMaxAttempts())
                .fixedBackoff(settings.getBackOff().toMillis())
                .build();
    }

    @Bean(RETRY + SELENIUM)
    public RetryTemplate retryTemplateSelenium(final RestClientProperties properties) {
        final RestClientProperties.Settings settings = properties.getRestClient().get(SELENIUM);
        return new RetryTemplateBuilder()
                .maxAttempts(settings.getMaxAttempts())
                .fixedBackoff(settings.getBackOff().toMillis())
                .build();
    }

    @Bean(RETRY + SLACK)
    public RetryTemplate retryTemplateSlack(final RestClientProperties properties) {
        final RestClientProperties.Settings settings = properties.getRestClient().get(SLACK);
        return new RetryTemplateBuilder()
                .maxAttempts(settings.getMaxAttempts())
                .fixedBackoff(settings.getBackOff().toMillis())
                .build();
    }

    @Bean(RETRY + JSOUP)
    public RetryTemplate retryTemplateJsoup(final RestClientProperties properties) {
        final RestClientProperties.Settings settings = properties.getRestClient().get(JSOUP);
        return new RetryTemplateBuilder()
                .maxAttempts(settings.getMaxAttempts())
                .fixedBackoff(settings.getBackOff().toMillis())
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

    @Bean
    public ObservedAspect observedAspect(final ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
