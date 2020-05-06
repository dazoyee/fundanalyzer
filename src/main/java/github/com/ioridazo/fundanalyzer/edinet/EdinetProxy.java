package github.com.ioridazo.fundanalyzer.edinet;

import github.com.ioridazo.fundanalyzer.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundanalyzer.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestOperations;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@Component
public class EdinetProxy {

    final private RestOperations restOperations;

    public EdinetProxy(final RestOperations restOperations) {
        this.restOperations = restOperations;
    }

    public EdinetResponse list(final ListRequestParameter parameter) {
        try {
            return restOperations.getForObject(
                    "/api/v1/documents.json?date={date}&type={type}",
                    EdinetResponse.class,
                    Map.of("date", parameter.getDate(), "type", parameter.getType().toValue())
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

    public void acquisition(final File storagePath, final AcquisitionRequestParameter parameter) {
        if (!storagePath.exists()) //noinspection ResultOfMethodCallIgnored
            storagePath.mkdirs();

        try {
            restOperations.execute(
                    "/api/v1/documents/{docId}?type={type}",
                    HttpMethod.GET,
                    request -> request
                            .getHeaders()
                            .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)),
                    response -> {
                        try {
                            Files.copy(response.getBody(), Paths.get(storagePath + "/" + parameter.getDocId() + ".zip"));
                        } catch (final FileAlreadyExistsException e) {
                            log.error("重複ファイル：\"{}\"", e.getFile());
                            throw new FundanalyzerFileException("ファイルが既に存在しています。スタックトレースを参考に確認してください。", e);
                        }
                        return null;
                    },
                    Map.of("docId", parameter.getDocId(), "type", parameter.getType().toValue())
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
            throw new FundanalyzerRestClientException(
                    "IO系のエラーにより、HTTP通信に失敗しました。スタックトレースを参考に原因を特定してください。", e);
        }
    }
}
