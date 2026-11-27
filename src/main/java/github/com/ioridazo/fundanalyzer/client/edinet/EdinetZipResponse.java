package github.com.ioridazo.fundanalyzer.client.edinet;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCircuitBreakerRecordException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

/**
 * EDINET からダウンロードした応答本文が zip であることを検証する。
 */
public final class EdinetZipResponse {

    private static final Logger log = LogManager.getLogger(EdinetZipResponse.class);

    /** ログに出力する応答本文の最大文字数 */
    static final int BODY_HEAD_LENGTH = 500;

    private static final int SIGNATURE_LENGTH = 4;

    private EdinetZipResponse() {
    }

    /**
     * 応答本文が zip であれば、先頭から読み直せる入力ストリームを返す。
     *
     * @param response HTTP レスポンス
     * @param category ログのカテゴリ
     * @return 応答本文
     * @throws IOException                              応答本文の読み込みに失敗したとき
     * @throws FundanalyzerCircuitBreakerRecordException 応答本文が zip でないとき
     */
    public static InputStream requireZip(final ClientHttpResponse response, final Category category) throws IOException {
        final BufferedInputStream body = new BufferedInputStream(response.getBody());
        body.mark(BODY_HEAD_LENGTH);
        final byte[] head = body.readNBytes(BODY_HEAD_LENGTH);
        if (isZip(head)) {
            body.reset();
            return body;
        }

        final String message = MessageFormat.format(
                "EDINETからzip以外の応答が返却されました。接続先やサービスの稼働状況を確認してください。"
                + "\tHTTPステータスコード:{0}\tContent-Type:{1}\tHTTPレスポンスボディ（先頭）:{2}",
                response.getStatusCode().value(),
                response.getHeaders().getContentType(),
                toHeadText(head)
        );
        log.warn(FundanalyzerLogClient.toClientLogObject(message, category, Process.DOWNLOAD));
        throw new FundanalyzerCircuitBreakerRecordException(message, null);
    }

    /**
     * 応答本文の先頭をログ出力用の文字列にする。
     *
     * @param body 応答本文
     * @return 改行を除いた先頭の文字列
     */
    static String toHeadText(final String body) {
        if (body == null) {
            return "";
        }
        final String singleLine = body.replaceAll("[\\r\\n]+", " ");
        return singleLine.length() > BODY_HEAD_LENGTH ? singleLine.substring(0, BODY_HEAD_LENGTH) : singleLine;
    }

    private static String toHeadText(final byte[] head) {
        return toHeadText(new String(head, StandardCharsets.UTF_8));
    }

    private static boolean isZip(final byte[] head) {
        if (head.length < SIGNATURE_LENGTH || head[0] != 'P' || head[1] != 'K') {
            return false;
        }
        // ローカルファイルヘッダ、空アーカイブの終端レコード、分割アーカイブの署名
        return (head[2] == 3 && head[3] == 4) || (head[2] == 5 && head[3] == 6) || (head[2] == 7 && head[3] == 8);
    }
}
