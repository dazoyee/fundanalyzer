package github.com.ioridazo.fundanalyzer.client.selenium;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.MessageFormat;

@Log4j2
@Component
public class SeleniumClient {

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
    @NewSpan("SeleniumProxy.edinetCodeList")
    public String edinetCodeList(final String inputFilePath) {
        try {
            final URI uri = UriComponentsBuilder
                    .fromUriString(baseUri)
                    .path("/selenium/v1/edinetcode")
                    .queryParam("path", inputFilePath.replace("/", "\\"))
                    .build().toUri();

            FundanalyzerLogClient.logProxy(
                    MessageFormat.format("Seleniumの通信を開始します。\tURL:{0}", uri),
                    Category.DOCUMENT,
                    Process.COMPANY
            );

            final String fileName = restTemplate.getForObject(uri, String.class);

            FundanalyzerLogClient.logProxy(
                    MessageFormat.format("Seleniumの通信を正常終了します。\tURL:{0}", uri),
                    Category.DOCUMENT,
                    Process.COMPANY
            );

            return fileName;
        } catch (RestClientException e) {
            throw new FundanalyzerRestClientException(
                    "なんらかのエラーが発生したため、異常終了しました。詳細を確認してください。", e);
        }
    }
}
