package github.com.ioridazo.fundanalyzer.client.edinet;

import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.ListType;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.EdinetResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

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

@Log4j2
@Component
public class EdinetClient {

    private final RestTemplate restTemplate;
    private final String baseUri;

    public EdinetClient(
            final RestTemplate restTemplate,
            @Value("${app.api.edinet}") final String baseUri) {
        this.restTemplate = restTemplate;
        this.baseUri = baseUri;
    }

    /**
     * EDINETの書類一覧API<br/>
     * 「メタデータのみ」または「提出書類一覧及びメタデータ」を取得することができる
     *
     * @param parameter パラメータ
     * @return EdinetResponse
     */
    @NewSpan("EdinetProxy.list")
    public EdinetResponse list(final ListRequestParameter parameter) {
        try {
            String message;
            if (ListType.DEFAULT.equals(parameter.getType())) {
                message = MessageFormat.format("書類一覧（メタデータ）取得処理を実行します。\t取得対象日:{0}", parameter.getDate());
            } else {
                message = MessageFormat.format("書類一覧（提出書類一覧及びメタデータ）取得処理を実行します。\t取得対象日:{0}", parameter.getDate());
            }
            FundanalyzerLogClient.logProxy(
                    message,
                    Category.DOCUMENT,
                    Process.EDINET
            );

            final EdinetResponse edinetResponse = restTemplate.getForObject(
                    baseUri + "/api/v1/documents.json?date={date}&type={type}",
                    EdinetResponse.class,
                    Map.of("date", parameter.getDate().toString(), "type", parameter.getType().toValue())
            );

            if (ListType.DEFAULT.equals(parameter.getType())) {
                message = MessageFormat.format("書類一覧（メタデータ）を正常に取得しました。\t取得対象日:{0}\t対象ファイル件数:{1}",
                        parameter.getDate(),
                        Optional.ofNullable(edinetResponse)
                                .map(er -> er.getMetadata().getResultset().getCount())
                                .orElse("0"));
            } else {
                message = "書類一覧（提出書類一覧及びメタデータ）を正常に取得しました。データベースへの登録作業を開始します。";
            }
            FundanalyzerLogClient.logProxy(
                    message,
                    Category.DOCUMENT,
                    Process.EDINET
            );

            return edinetResponse;
        } catch (final RestClientResponseException e) {
            log.error("EDINETから200以外のHTTPステータスコードが返却されました。" +
                            "\tHTTPステータスコード:{}" +
                            "\tHTTPレスポンスボディ:{}",
                    e.getRawStatusCode(),
                    e.getResponseBodyAsString()
            );

            if (HttpStatus.BAD_REQUEST.value() == e.getRawStatusCode()) {
                throw new FundanalyzerRestClientException(
                        "リクエスト内容が誤っています。リクエストの内容（エンドポイント、パラメータの形式等）を見直してください。", e);
            } else if (HttpStatus.NOT_FOUND.value() == e.getRawStatusCode()) {
                throw new FundanalyzerRestClientException(
                        "データが取得できません。パラメータの設定値を見直してください。", e);
            } else if (HttpStatus.INTERNAL_SERVER_ERROR.value() == e.getRawStatusCode()) {
                throw new FundanalyzerRestClientException(
                        "EDINET のトップページ又は金融庁ウェブサイトの各種情報検索サービスにてメンテナンス等の情報を確認してください。", e);
            } else {
                throw new FundanalyzerRestClientException(
                        "EDINET API仕様書に規定されていないHTTPステータスコードが返却されました。スタックトレースを参考に詳細を確認してください。", e);
            }
        } catch (final ResourceAccessException e) {
            throw new FundanalyzerRestClientException(
                    "IO系のエラーにより、HTTP通信に失敗しました。スタックトレースを参考に原因を特定してください。", e);
        }
    }

    /**
     * 書類取得API
     *
     * @param storagePath 保存先
     * @param parameter   パラメータ
     */
    @NewSpan("EdinetProxy.acquisition")
    public void acquisition(final File storagePath, final AcquisitionRequestParameter parameter) {
        makeDirectory(storagePath);
        try {
            FundanalyzerLogClient.logProxy(
                    MessageFormat.format("書類のダウンロード処理を実行します。\t書類管理番号:{0}", parameter.getDocId()),
                    Category.DOCUMENT,
                    Process.DOWNLOAD
            );

            restTemplate.execute(
                    baseUri + "/api/v1/documents/{docId}?type={type}",
                    HttpMethod.GET,
                    request -> request
                            .getHeaders()
                            .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)),
                    response -> copyFile(response.getBody(), Paths.get(storagePath + "/" + parameter.getDocId() + ".zip")),
                    Map.of("docId", parameter.getDocId(), "type", parameter.getType().toValue())
            );

            FundanalyzerLogClient.logProxy(
                    "書類のダウンロードが正常に実行されました。",
                    Category.DOCUMENT,
                    Process.DOWNLOAD
            );
        } catch (final RestClientResponseException e) {
            log.error("EDINETから200以外のHTTPステータスコードが返却されました。" +
                            "\tHTTPステータスコード:{}" +
                            "\tHTTPレスポンスボディ:{}",
                    e.getRawStatusCode(),
                    e.getResponseBodyAsString()
            );

            if (HttpStatus.BAD_REQUEST.value() == e.getRawStatusCode()) {
                throw new FundanalyzerRestClientException(
                        "リクエスト内容が誤っています。リクエストの内容（エンドポイント、パラメータの形式等）を見直してください。", e);
            } else if (HttpStatus.NOT_FOUND.value() == e.getRawStatusCode()) {
                throw new FundanalyzerRestClientException(
                        "データが取得できません。パラメータの設定値を見直してください。対象の書類が非開示となっている可能性があります。", e);
            } else if (HttpStatus.INTERNAL_SERVER_ERROR.value() == e.getRawStatusCode()) {
                throw new FundanalyzerRestClientException(
                        "EDINET のトップページ又は金融庁ウェブサイトの各種情報検索サービスにてメンテナンス等の情報を確認してください。", e);
            } else {
                throw new FundanalyzerRestClientException(
                        "EDINET API仕様書に規定されていないHTTPステータスコードが返却されました。スタックトレースを参考に詳細を確認してください。", e);
            }
        } catch (final ResourceAccessException e) {
            //noinspection StatementWithEmptyBody
            if (e.getCause() instanceof FileAlreadyExistsException) {
                // ファイルが既に存在していた場合、throwしない
            } else {
                throw new FundanalyzerRestClientException(
                        "IO系のエラーにより、HTTP通信に失敗しました。スタックトレースを参考に原因を特定してください。", e);
            }
        }
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
            log.error("重複ファイル：\"{}\"", e.getFile());
            throw e;
        }
        return null;
    }
}
