package github.com.ioridazo.fundanalyzer.slack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Slf4j
@Component
@PropertySource(value = "classpath:/slack-message.properties", encoding = "UTF-8")
public class SlackProxy {

    private final String uri;

    private final Environment environment;

    public SlackProxy(
            @Value("${app.api.slack}") final String uri,
            final Environment environment) {
        this.uri = uri;
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
        execute(nowLocalDataTime() + "\t" + MessageFormat.format(templateMessage, arguments));
    }

    private void execute(final String message) {
        restTemplate().exchange(
                RequestEntity.post(URI.create(uri))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"text\": \"" + message + "\"}"),
                String.class
        );
    }

    // FIXME
    private RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
