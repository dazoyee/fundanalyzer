package github.com.ioridazo.fundanalyzer.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TrustHeaderAuthenticationFilterのテスト")
class TrustHeaderAuthenticationFilterTest {

    private static final String SECRET = "test-trust-secret";

    private final TrustHeaderAuthenticationFilter filter = new TrustHeaderAuthenticationFilter(SECRET);

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("検証成立")
    class Success {

        @Test
        @DisplayName("正しい署名付きヘッダー→ROLE_USERの認証済みコンテキストを確立する")
        void validHeaders_authenticated() throws Exception {
            final long now = Instant.now().getEpochSecond();
            doFilter(requestWith("alice", "ADMIN", String.valueOf(now), sign("alice", "ADMIN", now)));

            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(authentication);
            assertEquals("alice", authentication.getName());
            assertEquals(1, authentication.getAuthorities().size());
            assertEquals("ROLE_USER", authentication.getAuthorities().iterator().next().getAuthority());
        }
    }

    @Nested
    @DisplayName("検証不成立（未認証のまま）")
    class Failure {

        @Test
        @DisplayName("署名が不正→未認証のまま")
        void invalidSignature_unauthenticated() throws Exception {
            final long now = Instant.now().getEpochSecond();
            doFilter(requestWith("alice", "ADMIN", String.valueOf(now), "forged-signature"));

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("署名は他ユーザーのもの→未認証のまま")
        void signatureForDifferentUser_unauthenticated() throws Exception {
            final long now = Instant.now().getEpochSecond();
            doFilter(requestWith("mallory", "ADMIN", String.valueOf(now), sign("alice", "ADMIN", now)));

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("タイムスタンプが鮮度窓（±300秒）を超過→未認証のまま")
        void staleTimestamp_unauthenticated() throws Exception {
            final long stale = Instant.now().getEpochSecond() - 301L;
            doFilter(requestWith("alice", "ADMIN", String.valueOf(stale), sign("alice", "ADMIN", stale)));

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("タイムスタンプが数値でない→未認証のまま")
        void nonNumericTimestamp_unauthenticated() throws Exception {
            doFilter(requestWith("alice", "ADMIN", "not-a-number", "any"));

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("ヘッダーが欠落→未認証のまま")
        void missingHeaders_unauthenticated() throws Exception {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Orbit-Auth-User", "alice");
            doFilter(request);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("ユーザー名に制御文字（改行）を含む→署名が一致しても未認証のまま")
        void controlCharacterInUsername_unauthenticated() throws Exception {
            final long now = Instant.now().getEpochSecond();
            // "a\nADMIN" + "x" と "a" + "ADMIN\nx" は署名対象文字列が衝突する（区切り文字インジェクション）
            doFilter(requestWith("a\nADMIN", "x", String.valueOf(now), sign("a", "ADMIN\nx", now)));

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("同名ヘッダーが複数値→未認証のまま")
        void duplicatedHeader_unauthenticated() throws Exception {
            final long now = Instant.now().getEpochSecond();
            final MockHttpServletRequest request =
                    requestWith("alice", "ADMIN", String.valueOf(now), sign("alice", "ADMIN", now));
            request.addHeader("X-Orbit-Auth-User", "mallory");
            doFilter(request);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Test
    @DisplayName("シークレット未設定でフィルターを構成→環境変数名を含む例外で失敗する")
    void blankSecret_throws() {
        final IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> new TrustHeaderAuthenticationFilter(" "));
        assertTrue(exception.getMessage().contains("ORBIT_TRUST_HEADER_SECRET"));
    }

    private void doFilter(final MockHttpServletRequest request) throws Exception {
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    private MockHttpServletRequest requestWith(
            final String user, final String role, final String timestamp, final String signature) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Orbit-Auth-User", user);
        request.addHeader("X-Orbit-Auth-Role", role);
        request.addHeader("X-Orbit-Auth-Timestamp", timestamp);
        request.addHeader("X-Orbit-Auth-Signature", signature);
        return request;
    }

    private static String sign(final String user, final String role, final long epochSeconds) throws Exception {
        final Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                mac.doFinal((user + "\n" + role + "\n" + epochSeconds).getBytes(StandardCharsets.UTF_8)));
    }
}
