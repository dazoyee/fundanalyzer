package github.com.ioridazo.fundanalyzer.proxy.selenium;

import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Log4j2
@Component
public class SeleniumProxy {

    private final RestTemplate restTemplate;
    private final String baseUri;

    public SeleniumProxy(
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
    public String edinetCodeList(final String inputFilePath) {
        try {
            final URI uri = UriComponentsBuilder
                    .fromUriString(baseUri)
                    .path("/selenium/v1/edinetcode")
                    .queryParam("path", inputFilePath.replace("/", "\\"))
                    .build().toUri();
            log.info("Seleniumの通信を開始します。\tURL:{}", uri);
            final String fileName = restTemplate.getForObject(uri, String.class);
            log.info("Seleniumの通信を正常終了します。\tURL:{}", uri);
            return fileName;
        } catch (RestClientException e) {
            throw new FundanalyzerRestClientException(
                    "なんらかのエラーが発生したため、異常終了しました。詳細を確認してください。", e);
        }
    }
}
