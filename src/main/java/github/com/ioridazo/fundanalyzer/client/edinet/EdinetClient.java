package github.com.ioridazo.fundanalyzer.client.edinet;

import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.ListType;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.Metadata;
import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCircuitBreakerRecordException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownContentTypeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class EdinetClient {

    private static final Logger log = LogManager.getLogger(EdinetClient.class);

    private static final String CIRCUIT_BREAKER_EDINET = "edinet";

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final CircuitBreakerRegistry circuitBreaker;

    public EdinetClient(
            @Qualifier("rest-edinet") final RestTemplate restTemplate,
            @Qualifier("retry-edinet") final RetryTemplate retryTemplate,
            final CircuitBreakerRegistry circuitBreaker) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * EDINETの書類一覧API<br/>
     * 「メタデータのみ」または「提出書類一覧及びメタデータ」を取得することができる
     *
     * @param parameter パラメータ
     * @return EdinetResponse
     * @throws FundanalyzerRestClientException 通知エラー時
     */
    @NewSpan
    public EdinetResponse list(final ListRequestParameter parameter) throws FundanalyzerRestClientException {
        if (ListType.DEFAULT.equals(parameter.getType())) {
            log.info(FundanalyzerLogClient.toClientLogObject(
                    MessageFormat.format("書類一覧（メタデータ）取得処理を実行します。\t取得対象日:{0}", parameter.getDate()),
                    Category.DOCUMENT,
                    Process.EDINET
            ));
        } else {
            log.info(FundanalyzerLogClient.toClientLogObject(
                    MessageFormat.format("書類一覧（提出書類一覧及びメタデータ）取得処理を実行します。\t取得対象日:{0}", parameter.getDate()),
                    Category.DOCUMENT,
                    Process.EDINET
            ));
        }


        // retry
        final EdinetResponse edinetResponse = retryTemplate.execute(context -> {
            if (context.getRetryCount() > 0) {
                if (ListType.DEFAULT.equals(parameter.getType())) {
                    log.info(FundanalyzerLogClient.toClientLogObject(
                            MessageFormat.format(
                                    "通信できなかったため、{0}回目の書類一覧（メタデータ）取得処理を実行します。\t取得対象日:{1}",
                                    context.getRetryCount() + 1,
                                    parameter.getDate()
                            ),
                            Category.DOCUMENT,
                            Process.EDINET
                    ));
                } else {
                    log.info(FundanalyzerLogClient.toClientLogObject(
                            MessageFormat.format(
                                    "通信できなかったため、{0}回目の書類一覧（提出書類一覧及びメタデータ）取得処理を実行します。\t取得対象日:{1}",
                                    context.getRetryCount() + 1,
                                    parameter.getDate()
                            ),
                            Category.DOCUMENT,
                            Process.EDINET
                    ));
                }
            }

            try {
                // circuitBreaker
                return circuitBreaker.circuitBreaker(CIRCUIT_BREAKER_EDINET)
                        .executeSupplier(() -> {
                            try {
                                // send
                                return restTemplate.getForObject(
                                        "/api/v1/documents.json?date={date}&type={type}",
                                        EdinetResponse.class,
                                        Map.of("date", parameter.getDate().toString(), "type", parameter.getType().toValue())
                                );
                            } catch (final RestClientResponseException e) {
                                log.error(FundanalyzerLogClient.toClientLogObject(
                                        MessageFormat.format(
                                                "EDINETから200以外のHTTPステータスコードが返却されました。" +
                                                        "\tHTTPステータスコード:{0}\tHTTPレスポンスボディ:{1}",
                                                e.getRawStatusCode(),
                                                e.getResponseBodyAsString()
                                        ),
                                        Category.DOCUMENT,
                                        Process.EDINET
                                ));

                                if (HttpStatus.BAD_REQUEST.value() == e.getRawStatusCode()) {
                                    throw new FundanalyzerRestClientException(
                                            "リクエスト内容が誤っています。リクエストの内容（エンドポイント、パラメータの形式等）を見直してください。", e);
                                } else if (HttpStatus.NOT_FOUND.value() == e.getRawStatusCode()) {
                                    throw new FundanalyzerCircuitBreakerRecordException(
                                            "データが取得できません。パラメータの設定値を見直してください。", e);
                                } else if (HttpStatus.INTERNAL_SERVER_ERROR.value() == e.getRawStatusCode()) {
                                    throw new FundanalyzerCircuitBreakerRecordException(
                                            "EDINET のトップページ又は金融庁ウェブサイトの各種情報検索サービスにてメンテナンス等の情報を確認してください。", e);
                                } else {
                                    throw new FundanalyzerCircuitBreakerRecordException(
                                            "EDINET API仕様書に規定されていないHTTPステータスコードが返却されました。スタックトレースを参考に詳細を確認してください。", e);
                                }
                            } catch (final ResourceAccessException e) {
                                throw new FundanalyzerCircuitBreakerRecordException(
                                        "IO系のエラーにより、HTTP通信に失敗しました。スタックトレースを参考に原因を特定してください。", e);
                            } catch (final UnknownContentTypeException e) {
                                throw new FundanalyzerCircuitBreakerRecordException(
                                        "HttpMessageConverter応答を抽出するのに適したものが見つかりませんでした。" +
                                                "外部機関のサービス稼働状況を確認してください。※システムメンテナンスの疑いあり", e
                                );
                            }
                        });
            } catch (final CallNotPermittedException e) {
                throw new FundanalyzerRestClientException(CIRCUIT_BREAKER_EDINET + "との通信でサーキットブレーカーがオープンしました。");
            }
        });

        if (ListType.DEFAULT.equals(parameter.getType())) {
            log.info(FundanalyzerLogClient.toClientLogObject(
                    MessageFormat.format(
                            "書類一覧（メタデータ）を正常に取得しました。\t取得対象日:{0}\t対象ファイル件数:{1}",
                            parameter.getDate(),
                            Optional.ofNullable(edinetResponse)
                                    .map(EdinetResponse::getMetadata)
                                    .map(Metadata::getResultset)
                                    .map(Metadata.ResultSet::getCount)
                                    .orElse("0")
                    ),
                    Category.DOCUMENT,
                    Process.EDINET
            ));
        } else {
            log.info(FundanalyzerLogClient.toClientLogObject(
                    "書類一覧（提出書類一覧及びメタデータ）を正常に取得しました。データベースへの登録作業を開始します。",
                    Category.DOCUMENT,
                    Process.EDINET
            ));
        }

        return edinetResponse;
    }

    /**
     * 書類取得API
     *
     * @param storagePath 保存先
     * @param parameter   パラメータ
     */
    @NewSpan
    public void acquisition(final File storagePath, final AcquisitionRequestParameter parameter) {
        makeDirectory(storagePath);

        // retry
        retryTemplate.execute(retryContext -> {
            if (retryContext.getRetryCount() == 0) {
                log.info(FundanalyzerLogClient.toClientLogObject(
                        MessageFormat.format("書類のダウンロード処理を実行します。\t書類管理番号:{0}", parameter.getDocId()),
                        parameter.getDocId(),
                        Category.DOCUMENT,
                        Process.DOWNLOAD
                ));
            } else {
                log.info(FundanalyzerLogClient.toClientLogObject(
                        MessageFormat.format(
                                "通信できなかったため、{0}回目の書類のダウンロード処理を実行します。\t書類管理番号:{1}",
                                retryContext.getRetryCount(),
                                parameter.getDocId()
                        ),
                        parameter.getDocId(),
                        Category.DOCUMENT,
                        Process.DOWNLOAD
                ));
            }

            try {
                // circuitBreaker
                return circuitBreaker.circuitBreaker(CIRCUIT_BREAKER_EDINET)
                        .executeSupplier(() -> {
                            try {
                                // send
                                return restTemplate.execute(
                                        "/api/v1/documents/{docId}?type={type}",
                                        HttpMethod.GET,
                                        request -> request
                                                .getHeaders()
                                                .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)),
                                        response -> copyFile(response.getBody(), Paths.get(storagePath + "/" + parameter.getDocId() + ".zip")),
                                        Map.of("docId", parameter.getDocId(), "type", parameter.getType().toValue())
                                );
                            } catch (final RestClientResponseException e) {
                                log.error(FundanalyzerLogClient.toClientLogObject(
                                        MessageFormat.format(
                                                "EDINETから200以外のHTTPステータスコードが返却されました。" +
                                                        "\tHTTPステータスコード:{0}\tHTTPレスポンスボディ:{1}",
                                                e.getRawStatusCode(),
                                                e.getResponseBodyAsString()
                                        ),
                                        parameter.getDocId(),
                                        Category.DOCUMENT,
                                        Process.DOWNLOAD
                                ));

                                if (HttpStatus.BAD_REQUEST.value() == e.getRawStatusCode()) {
                                    throw new FundanalyzerRestClientException(
                                            "リクエスト内容が誤っています。リクエストの内容（エンドポイント、パラメータの形式等）を見直してください。", e);
                                } else if (HttpStatus.NOT_FOUND.value() == e.getRawStatusCode()) {
                                    throw new FundanalyzerCircuitBreakerRecordException(
                                            "データが取得できません。パラメータの設定値を見直してください。対象の書類が非開示となっている可能性があります。", e);
                                } else if (HttpStatus.INTERNAL_SERVER_ERROR.value() == e.getRawStatusCode()) {
                                    throw new FundanalyzerCircuitBreakerRecordException(
                                            "EDINET のトップページ又は金融庁ウェブサイトの各種情報検索サービスにてメンテナンス等の情報を確認してください。", e);
                                } else {
                                    throw new FundanalyzerCircuitBreakerRecordException(
                                            "EDINET API仕様書に規定されていないHTTPステータスコードが返却されました。スタックトレースを参考に詳細を確認してください。", e);
                                }
                            } catch (final ResourceAccessException e) {
                                if (e.getCause() instanceof FileAlreadyExistsException) {
                                    // ファイルが既に存在していた場合、throwしない
                                    return null;
                                } else {
                                    throw new FundanalyzerCircuitBreakerRecordException(
                                            "IO系のエラーにより、HTTP通信に失敗しました。スタックトレースを参考に原因を特定してください。", e);
                                }
                            }
                        });
            } catch (final CallNotPermittedException e) {
                throw new FundanalyzerRestClientException(CIRCUIT_BREAKER_EDINET + "との通信でサーキットブレーカーがオープンしました。");
            }
        });

        log.info(FundanalyzerLogClient.toClientLogObject(
                MessageFormat.format("書類のダウンロードが正常に実行されました。\t書類管理番号:{0}", parameter.getDocId()),
                parameter.getDocId(),
                Category.DOCUMENT,
                Process.DOWNLOAD
        ));
    }

    /**
     * ファイルの保存先が存在しなかったら作成する
     *
     * @param storagePath 保存先
     */
    void makeDirectory(final File storagePath) {
        if (!storagePath.exists()) {
            //noinspection ResultOfMethodCallIgnored
            storagePath.mkdirs();
        }
    }

    /**
     * ダウンロードしたファイルを保存する
     *
     * @param file ダウンロードしたファイル
     * @param path 保存先
     * @return null
     * @throws IOException エラー発生時
     */
    Object copyFile(final InputStream file, final Path path) throws IOException {
        try {
            Files.copy(file, path);
        } catch (final FileAlreadyExistsException e) {
            log.error(FundanalyzerLogClient.toClientLogObject(
                    MessageFormat.format("重複ファイル：\"{0}\"", e.getFile()),
                    Category.DOCUMENT,
                    Process.DOWNLOAD
            ));
            throw e;
        }
        return null;
    }

    public static class RecordFailurePredicate implements Predicate<Throwable> {

        @Override
        public boolean test(final Throwable throwable) {
            return throwable instanceof FundanalyzerCircuitBreakerRecordException;
        }
    }
}
