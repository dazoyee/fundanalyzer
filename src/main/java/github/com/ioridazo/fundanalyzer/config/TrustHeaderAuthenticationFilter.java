package github.com.ioridazo.fundanalyzer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

/**
 * リバースプロキシ（orbit）が付与する署名付きトラストヘッダーを検証し、
 * 成立時に Pre-Authentication として認証済みコンテキストを確立するフィルター。
 *
 * <p>検証規約: 4 ヘッダーすべて存在・各ヘッダー単一値・HMAC-SHA256 署名一致（定数時間比較）・
 * タイムスタンプ鮮度 ±300 秒。いずれか不成立の場合は未認証のまま後続へ進める
 * （認可設定の {@code authenticated()} により 401 となる）。
 * ヘッダー仕様の正本は orbit リポジトリの docs/auth-sso.md「署名付きトラストヘッダー伝搬」。
 *
 * <p>ロールは署名対象として検証するが、本アプリ内の権限差別化には使わず常に {@code ROLE_USER} を
 * 付与する（本アプリへのアクセス可否は orbit 側の app_visibility が認可済みのため）。
 */
public class TrustHeaderAuthenticationFilter extends OncePerRequestFilter {

    static final String USER_HEADER = "X-Orbit-Auth-User";
    static final String ROLE_HEADER = "X-Orbit-Auth-Role";
    static final String TIMESTAMP_HEADER = "X-Orbit-Auth-Timestamp";
    static final String SIGNATURE_HEADER = "X-Orbit-Auth-Signature";

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long MAX_CLOCK_SKEW_SECONDS = 300L;

    private final String secret;

    public TrustHeaderAuthenticationFilter(final String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.security.trust-secret is required for trust-header authentication. "
                            + "Set the ORBIT_TRUST_HEADER_SECRET environment variable.");
        }
        this.secret = secret;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        authenticate(request);
        filterChain.doFilter(request, response);
    }

    private void authenticate(final HttpServletRequest request) {
        final String username = singleHeaderValue(request, USER_HEADER);
        final String role = singleHeaderValue(request, ROLE_HEADER);
        final String timestamp = singleHeaderValue(request, TIMESTAMP_HEADER);
        final String signature = singleHeaderValue(request, SIGNATURE_HEADER);
        if (username == null || role == null || timestamp == null || signature == null) {
            return;
        }
        // 署名対象は改行区切りの連結のため、フィールドに制御文字（CR/LF 等）を含む値は
        // 区切り文字インジェクションによる署名衝突の余地を残す。輸送層（HTTP ヘッダー仕様）の
        // 制御文字禁止に暗黙依存せず、受信側でも明示的に拒否する。
        if (containsControlCharacter(username) || containsControlCharacter(role)) {
            return;
        }
        if (!isFresh(timestamp) || !hasValidSignature(username, role, timestamp, signature)) {
            // 検証前の生ヘッダー値のため、ログ改ざん（CRLF インジェクション）を防いで出力する
            logger.warn("Trust header verification failed for user header '"
                    + username.replace("\r", "\\r").replace("\n", "\\n") + "'");
            return;
        }
        final AbstractAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(
                username, "N/A", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * ヘッダーが複数値で届いた場合は上書き・混入の可能性があるため検証対象とせず null を返す。
     */
    private String singleHeaderValue(final HttpServletRequest request, final String headerName) {
        final Enumeration<String> values = request.getHeaders(headerName);
        if (values == null || !values.hasMoreElements()) {
            return null;
        }
        final String first = values.nextElement();
        return values.hasMoreElements() ? null : first;
    }

    private boolean containsControlCharacter(final String value) {
        return value.chars().anyMatch(Character::isISOControl);
    }

    private boolean isFresh(final String timestamp) {
        final long epochSeconds;
        try {
            epochSeconds = Long.parseLong(timestamp);
        } catch (final NumberFormatException ex) {
            return false;
        }
        return Math.abs(Instant.now().getEpochSecond() - epochSeconds) <= MAX_CLOCK_SKEW_SECONDS;
    }

    private boolean hasValidSignature(
            final String username, final String role, final String timestamp, final String signature) {
        final String expected = sign(username + "\n" + role + "\n" + timestamp);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(final String message) {
        try {
            final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(this.secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA256 signing is unavailable", ex);
        }
    }
}
