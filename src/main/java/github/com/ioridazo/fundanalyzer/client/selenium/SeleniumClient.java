package github.com.ioridazo.fundanalyzer.client.selenium;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;

@Component
public class SeleniumClient {

    private static final Logger log = LogManager.getLogger(SeleniumClient.class);

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    public SeleniumClient(
            @Qualifier("rest-selenium") final RestTemplate restTemplate,
            @Qualifier("retry-selenium") final RetryTemplate retryTemplate) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
    }

    /**
     * Seleniumを利用して会社情報一覧CSVをダウンロードする
     *
     * @param inputFilePath 保存先パス
     * @return ダウンロードファイル名
     */
    @NewSpan
    public String edinetCodeList(final String inputFilePath) {
        final String endpoint = String.format("/selenium/v1/edinetcode?path=%s", inputFilePath.replace("/", "\\"));

        try {
            final String fileName = retryTemplate.execute(context -> {
                log.info(FundanalyzerLogClient.toClientLogObject(
                        MessageFormat.format(
                                "{0}回目のSeleniumへの通信を開始します。\tURL:{1}",
                                context.getRetryCount() + 1,
                                endpoint
                        ),
                        Category.COMPANY,
                        Process.EDINET
                ));

                return restTemplate.getForObject(endpoint, String.class);
            });

            log.info(FundanalyzerLogClient.toClientLogObject(
                    MessageFormat.format("Seleniumの通信を正常終了します。\tURL:{0}", endpoint),
                    Category.COMPANY,
                    Process.EDINET
            ));

            return fileName;
        } catch (final RestClientException e) {
            throw new FundanalyzerRestClientException("Selenium通信がリトライ上限に達しました。エラー内容を確認してください。", e);
        }
    }
}
