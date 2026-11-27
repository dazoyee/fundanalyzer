package github.com.ioridazo.fundanalyzer.client.edinet;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCircuitBreakerRecordException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.micrometer.observation.annotation.Observed;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.function.Predicate;

@Component
public class EdinetCodeListClient {

    private static final Logger log = LogManager.getLogger(EdinetCodeListClient.class);
    private static final String EDINET_CODE_LIST = "edinet-codelist";
    private static final String FILE_NAME = "Edinetcode.zip";

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    public EdinetCodeListClient(
            @Qualifier("restEdinetCodeList") final RestTemplate restTemplate,
            @Qualifier("retryEdinetCodeList") final RetryTemplate retryTemplate,
            final CircuitBreakerRegistry circuitBreakerRegistry,
            final RateLimiterRegistry rateLimiterRegistry) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    /**
     * EDINETコードリストをダウンロードする
     *
     * @param storagePath 保存先
     * @return 保存したファイル名
     */
    @Observed
    public String download(final File storagePath) {
        try {
            Files.createDirectories(storagePath.toPath());
            final Path filePath = Paths.get(storagePath.getPath(), FILE_NAME);
            retryTemplate.execute(retryContext -> {
                log.info(FundanalyzerLogClient.toClientLogObject(
                        retryContext.getRetryCount() == 0
                                ? "EDINETコードリストのダウンロード処理を実行します。"
                                : MessageFormat.format("通信に失敗したため、リトライ{0}回目のEDINETコードリストのダウンロード処理を実行します。",
                                retryContext.getRetryCount()),
                        Category.COMPANY,
                        Process.DOWNLOAD
                ));
                return circuitBreakerRegistry.circuitBreaker(EDINET_CODE_LIST).executeSupplier(() ->
                        rateLimiterRegistry.rateLimiter(EDINET_CODE_LIST).executeSupplier(() -> {
                            try {
                                return restTemplate.execute(
                                        "/searchdocument/codelist/Edinetcode.zip",
                                        HttpMethod.GET,
                                        request -> request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)),
                                        response -> copyFile(response, filePath)
                                );
                            } catch (final RestClientResponseException e) {
                                if (e.getStatusCode().is3xxRedirection()) {
                                    throw EdinetClient.redirected(e, Category.COMPANY, Process.DOWNLOAD);
                                }
                                log.warn(FundanalyzerLogClient.toClientLogObject(
                                        MessageFormat.format("EDINETコードリスト取得でHTTPエラーが返却されました。\tHTTPステータスコード:{0}\tHTTPレスポンスボディ:{1}",
                                                e.getStatusCode(), e.getResponseBodyAsString()),
                                        Category.COMPANY,
                                        Process.DOWNLOAD
                                ));
                                throw new FundanalyzerCircuitBreakerRecordException("EDINETコードリストを取得できませんでした。", e);
                            } catch (final ResourceAccessException e) {
                                throw new FundanalyzerCircuitBreakerRecordException("IO系のエラーにより、HTTP通信に失敗しました。", e);
                            }
                        })
                );
            });
            log.info(FundanalyzerLogClient.toClientLogObject(
                    "EDINETコードリストを正常に取得しました。",
                    Category.COMPANY,
                    Process.DOWNLOAD
            ));
            return FILE_NAME;
        } catch (final RequestNotPermitted e) {
            throw new FundanalyzerRestClientException(EDINET_CODE_LIST + "との通信でレートリミッターが作動しました。", true);
        } catch (final CallNotPermittedException e) {
            throw new FundanalyzerRestClientException(EDINET_CODE_LIST + "との通信でサーキットブレーカーがオープンしました。", true);
        } catch (final Exception e) {
            throw new FundanalyzerRestClientException(e.getMessage(), e.getCause());
        }
    }

    private Object copyFile(final ClientHttpResponse response, final Path path) throws IOException {
        final InputStream input = EdinetZipResponse.requireZip(response, Category.COMPANY);
        Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
        return null;
    }

    public static class RecordFailurePredicate implements Predicate<Throwable> {
        @Override
        public boolean test(final Throwable throwable) {
            return throwable instanceof FundanalyzerCircuitBreakerRecordException;
        }
    }
}
