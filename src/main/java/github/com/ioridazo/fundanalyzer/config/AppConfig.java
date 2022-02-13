package github.com.ioridazo.fundanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AppConfig {

    @Bean("edinet-rest")
    public RestTemplate restTemplateEdinet(
            @Value("${app.config.rest-template.edinet.connect-timeout}") final Duration connectTimeout,
            @Value("${app.config.rest-template.edinet.read-timeout}") final Duration readTimeout) {
        return new RestTemplateBuilder()
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }

    @Bean("selenium-rest")
    public RestTemplate restTemplateSelenium(
            @Value("${app.config.rest-template.selenium.connect-timeout}") final Duration connectTimeout,
            @Value("${app.config.rest-template.selenium.read-timeout}") final Duration readTimeout) {
        return new RestTemplateBuilder()
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }

    @Bean("slack-rest")
    public RestTemplate restTemplateSlack(
            @Value("${app.config.rest-template.slack.connect-timeout}") final Duration connectTimeout,
            @Value("${app.config.rest-template.slack.read-timeout}") final Duration readTimeout) {
        return new RestTemplateBuilder()
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }

    @Bean("jsoup-rest")
    public RestTemplate restTemplateJsoup(
            @Value("${app.config.rest-template.jsoup.connect-timeout}") final Duration connectTimeout,
            @Value("${app.config.rest-template.jsoup.read-timeout}") final Duration readTimeout) {
        return new RestTemplateBuilder()
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }

    @Bean("edinet-retry")
    public RetryTemplate retryTemplateEdinet(
            @Value("${app.config.rest-template.edinet.max-attempts}") final Integer maxAttempt,
            @Value("${app.config.rest-template.edinet.back-off}") final Duration backOff) {
        return new RetryTemplateBuilder()
                .maxAttempts(maxAttempt)
                .fixedBackoff(backOff.toMillis())
                .build();
    }

    @Bean("selenium-retry")
    public RetryTemplate retryTemplateSelenium(
            @Value("${app.config.rest-template.selenium.max-attempts}") final Integer maxAttempt,
            @Value("${app.config.rest-template.selenium.back-off}") final Duration backOff) {
        return new RetryTemplateBuilder()
                .maxAttempts(maxAttempt)
                .fixedBackoff(backOff.toMillis())
                .build();
    }

    @Bean("slack-retry")
    public RetryTemplate retryTemplateSlack(
            @Value("${app.config.rest-template.slack.max-attempts}") final Integer maxAttempt,
            @Value("${app.config.rest-template.slack.back-off}") final Duration backOff) {
        return new RetryTemplateBuilder()
                .maxAttempts(maxAttempt)
                .fixedBackoff(backOff.toMillis())
                .build();
    }

    @Bean("jsoup-retry")
    public RetryTemplate retryTemplateJsoup(
            @Value("${app.config.rest-template.jsoup.max-attempts}") final Integer maxAttempt,
            @Value("${app.config.rest-template.jsoup.back-off}") final Duration backOff) {
        return new RetryTemplateBuilder()
                .maxAttempts(maxAttempt)
                .fixedBackoff(backOff.toMillis())
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
