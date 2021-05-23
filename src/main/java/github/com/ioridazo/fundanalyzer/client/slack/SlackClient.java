package github.com.ioridazo.fundanalyzer.client.slack;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

@Component
@PropertySource(value = "classpath:/slack-message.properties", encoding = "UTF-8")
public class SlackClient {

    private final RestTemplate restTemplate;
    private final String baseUri;
    private final Environment environment;
    @Value("${app.api.slack.parameter.t}")
    String parameterT;
    @Value("${app.api.slack.parameter.b}")
    String parameterB;
    @Value("${app.api.slack.parameter.x}")
    String parameterX;

    public SlackClient(
            final RestTemplate restTemplate,
            @Value("${app.api.slack.base-uri}") final String baseUri,
            final Environment environment) {
        this.restTemplate = restTemplate;
        this.baseUri = baseUri;
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
    @NewSpan("SlackProxy.sendMessage")
    public void sendMessage(final String propertyPath) {
        final var message = Objects.requireNonNullElse(environment.getProperty(propertyPath), "message error");
        execute(message);
        FundanalyzerLogClient.logProxy("Slack通知完了. " + message, Category.SLACK, Process.NOTICE);
    }

    /**
     * Slackに通知する
     *
     * @param propertyPath プロパティパス
     * @param arguments    パラメータ
     */
    @NewSpan("SlackProxy.sendMessage")
    public void sendMessage(final String propertyPath, final Object... arguments) {
        final var templateMessage = Objects.requireNonNullElse(environment.getProperty(propertyPath), "message error");
        final String message = nowLocalDataTime() + "\t" + MessageFormat.format(templateMessage, arguments);
        execute(message);

        FundanalyzerLogClient.logProxy("Slack通知完了. " + message, Category.SLACK, Process.NOTICE);
    }

    private void execute(final String message) {
        final var url = UriComponentsBuilder.fromUriString(baseUri)
                .path("services/{t}/{b}/{x}")
                .buildAndExpand(Map.of("t", parameterT, "b", parameterB, "x", parameterX)).toUri();
        try {
            restTemplate.exchange(
                    RequestEntity.post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"text\": \"" + message + "\"}"),
                    String.class
            );
        } catch (RestClientException e) {
            throw new FundanalyzerRestClientException(
                    "Slackとの通信でなんらかのエラーが発生したため、通知できませんでした。詳細を確認してください。", e);
        }
    }
}
