package github.com.ioridazo.fundanalyzer.client.slack;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Component
@PropertySource(value = "classpath:/messages_ja.properties", encoding = "UTF-8")
public class SlackClient {

    private static final Logger log = LogManager.getLogger(SlackClient.class);

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final Environment environment;
    @Value("${app.config.slack.parameter.t}")
    String parameterT;
    @Value("${app.config.slack.parameter.b}")
    String parameterB;
    @Value("${app.config.slack.parameter.x}")
    String parameterX;

    public SlackClient(
            @Qualifier("rest-slack") final RestTemplate restTemplate,
            @Qualifier("retry-slack") final RetryTemplate retryTemplate,
            final Environment environment) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.environment = environment;
    }

    String nowLocalDataTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss"));
    }

    /**
     * Slackに通知する
     *
     * @param propertyPath プロパティパス
     */
    public void sendMessage(final String propertyPath) {
        final var message = Objects.requireNonNullElse(environment.getProperty(propertyPath), "message error");
        execute(message);
    }

    /**
     * Slackに通知する
     *
     * @param propertyPath プロパティパス
     * @param arguments    パラメータ
     */
    public void sendMessage(final String propertyPath, final Object... arguments) {
        final var templateMessage = Objects.requireNonNullElse(environment.getProperty(propertyPath), "message error");
        final String message = nowLocalDataTime() + "\t" + MessageFormat.format(templateMessage, arguments);
        execute(message);
    }

    private void execute(final String message) {
        try {
            retryTemplate.execute(retryContext -> restTemplate.exchange(
                    RequestEntity.post(String.format("/services/%s/%s/%s", parameterT, parameterB, parameterX))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"text\": \"" + message + "\"}"),
                    String.class
            ));

            log.info(FundanalyzerLogClient.toClientLogObject(
                    "Slack通知完了. ",
                    Category.NOTICE,
                    Process.SLACK
            ));
        } catch (RestClientException e) {
            throw new FundanalyzerRestClientException(
                    "Slackとの通信でなんらかのエラーが発生したため、通知できませんでした。詳細を確認してください。", e);
        }
    }
}
