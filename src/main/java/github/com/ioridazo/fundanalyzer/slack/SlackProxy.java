package github.com.ioridazo.fundanalyzer.slack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Slf4j
@Component
public class SlackProxy {

    private final String uri;

    public SlackProxy(
            @Value("${app.api.slack}") final String uri) {
        this.uri = uri;
    }

    public void sendMessage(final String message) {
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
