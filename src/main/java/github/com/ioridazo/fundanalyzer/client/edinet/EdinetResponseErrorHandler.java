package github.com.ioridazo.fundanalyzer.client.edinet;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

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
            final HttpHeaders headers = response.getHeaders();
            final byte[] body = getResponseBody(response);
            final Charset charset = getCharset(response);
            final String location = headers.getFirst(HttpHeaders.LOCATION);
            final String message = "Redirect returned. status=" + statusCode.value()
                    + " location=" + sanitizeLocation(location);
            throw new RestClientResponseException(
                    message,
                    statusCode,
                    response.getStatusText(),
                    headers,
                    body,
                    charset);
        }
        super.handleError(response);
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
