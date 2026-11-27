package github.com.ioridazo.fundanalyzer.client.edinet;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * EDINET のリダイレクト応答を HTTP エラーとして扱う。
 */
public class EdinetResponseErrorHandler extends DefaultResponseErrorHandler {

    private static final String NONE = "(none)";
    private static final String UNPARSEABLE = "(unparseable)";

    @Override
    protected boolean hasError(final HttpStatusCode statusCode) {
        return statusCode.is3xxRedirection() || super.hasError(statusCode);
    }

    @Override
    public void handleError(final ClientHttpResponse response) throws IOException {
        final HttpStatusCode statusCode = response.getStatusCode();
        if (statusCode.is3xxRedirection()) {
            // 移転先の URL やクエリに API キーが含まれうるため、本文は捨て、Location はクエリを除いて渡す
            final String rawLocation = response.getHeaders().getFirst(HttpHeaders.LOCATION);
            final String location = sanitizeLocation(rawLocation);
            final HttpHeaders headers = new HttpHeaders();
            if (rawLocation != null && !rawLocation.isEmpty()) {
                headers.set(HttpHeaders.LOCATION, location);
            }
            final String message = "Redirect returned. status=" + statusCode.value() + " location=" + location;
            throw new RestClientResponseException(
                    message,
                    statusCode,
                    response.getStatusText(),
                    headers,
                    new byte[0],
                    StandardCharsets.UTF_8);
        }
        super.handleError(response);
    }

    /**
     * 例外が持つ Location ヘッダを取り出す。
     *
     * @param e HTTP エラー
     * @return Location ヘッダ。無い場合は (none)
     */
    public static String locationOf(final RestClientResponseException e) {
        final HttpHeaders headers = e.getResponseHeaders();
        return sanitizeLocation(headers == null ? null : headers.getFirst(HttpHeaders.LOCATION));
    }

    /**
     * Location からクエリ、フラグメント、ユーザー情報を除去する。
     *
     * @param location Location ヘッダー
     * @return 秘密情報を含まない Location
     */
    public static String sanitizeLocation(final String location) {
        if (location == null || location.isEmpty()) {
            return NONE;
        }
        try {
            final URI uri = new URI(location);
            if (!uri.isAbsolute()) {
                final String path = uri.getPath();
                return path == null || path.isEmpty() ? NONE : path;
            }
            if (uri.getScheme() == null || uri.getHost() == null) {
                return UNPARSEABLE;
            }
            final StringBuilder sanitized = new StringBuilder()
                    .append(uri.getScheme())
                    .append("://");
            sanitized.append(uri.getHost());
            if (uri.getPort() != -1) {
                sanitized.append(':').append(uri.getPort());
            }
            if (uri.getPath() != null) {
                sanitized.append(uri.getPath());
            }
            return sanitized.toString();
        } catch (final URISyntaxException e) {
            return UNPARSEABLE;
        }
    }
}
