package github.com.ioridazo.fundanalyzer.client.selenium;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.MessageFormat;

@Component
public class SeleniumClient {

    private static final Logger log = LogManager.getLogger(SeleniumClient.class);

    private final RestTemplate restTemplate;
    private final String baseUri;

    public SeleniumClient(
            final RestTemplate restTemplate,
            @Value("${app.api.selenium}") final String baseUri) {
        this.restTemplate = restTemplate;
        this.baseUri = baseUri;
    }

    /**
     * Seleniumを利用して会社情報一覧CSVをダウンロードする
     *
     * @param inputFilePath 保存先パス
     * @return ダウンロードファイル名
     */
    @NewSpan
    @Retryable(value = RestClientException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public String edinetCodeList(final String inputFilePath) {
        final URI uri = UriComponentsBuilder
                .fromUriString(baseUri)
                .path("/selenium/v1/edinetcode")
                .queryParam("path", inputFilePath.replace("/", "\\"))
                .build().toUri();

        log.info(FundanalyzerLogClient.toClientLogObject(
                MessageFormat.format("Seleniumの通信を開始します。\tURL:{0}", uri),
                Category.COMPANY,
                Process.EDINET
        ));

        final String fileName = restTemplate.getForObject(uri, String.class);

        log.info(FundanalyzerLogClient.toClientLogObject(
                MessageFormat.format("Seleniumの通信を正常終了します。\tURL:{0}", uri),
                Category.COMPANY,
                Process.EDINET
        ));

        return fileName;
    }

    @Recover
    public String recover(final RestClientException e) {
        throw new FundanalyzerRestClientException("Selenium通信がリトライ上限に達しました。エラー内容を確認してください。", e);
    }
}
