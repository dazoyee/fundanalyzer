package github.com.ioridazo.fundanalyzer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * trust-header モード（本番の認証構成）の認証・CSRF・セキュリティヘッダーを検証する統合テスト。
 * フォームログイン構成の検証は {@link SecurityConfigIntegrationTest} が担う。
 */
@SpringBootTest(properties = {
        "app.security.mode=trust-header",
        "app.security.trust-secret=integration-trust-secret",
        "management.server.port=",
        "management.endpoints.web.exposure.include=health"
})
@AutoConfigureMockMvc
@DisplayName("TrustHeaderSecurityConfigの統合テスト")
class TrustHeaderSecurityConfigIntegrationTest {

    private static final String SECRET = "integration-trust-secret";

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("認証 のテスト")
    class Authentication {

        @Test
        @DisplayName("正しい署名付きトラストヘッダー→画面に200でアクセスできる")
        void index_正規ヘッダー_200() throws Exception {
            mockMvc.perform(withTrustHeaders(get("/v3/index"), "alice", "ADMIN").accept(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("トラストヘッダーなし→401を返す（ログイン画面へのリダイレクトはしない）")
        void index_ヘッダーなし_401() throws Exception {
            mockMvc.perform(get("/v3/index").accept(MediaType.TEXT_HTML))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("署名が不正→401を返す")
        void index_署名不正_401() throws Exception {
            final long now = Instant.now().getEpochSecond();
            mockMvc.perform(get("/v3/index")
                            .header("X-Orbit-Auth-User", "alice")
                            .header("X-Orbit-Auth-Role", "ADMIN")
                            .header("X-Orbit-Auth-Timestamp", String.valueOf(now))
                            .header("X-Orbit-Auth-Signature", "forged-signature"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("タイムスタンプが期限切れ→401を返す")
        void index_期限切れ_401() throws Exception {
            final long stale = Instant.now().getEpochSecond() - 301L;
            mockMvc.perform(get("/v3/index")
                            .header("X-Orbit-Auth-User", "alice")
                            .header("X-Orbit-Auth-Role", "ADMIN")
                            .header("X-Orbit-Auth-Timestamp", String.valueOf(stale))
                            .header("X-Orbit-Auth-Signature", sign("alice", "ADMIN", stale)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("フォームログインのエンドポイントは存在しない→/loginは401を返す")
        void login_フォームログイン撤廃_401() throws Exception {
            mockMvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("未認証で静的リソースにアクセス→permitAllで200を返す")
        void staticResource_未認証_200() throws Exception {
            mockMvc.perform(get("/css/app.css"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("未認証でActuator healthにアクセス→200を返す")
        void actuatorHealth_未認証_200() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("CSRF のテスト")
    class Csrf {

        @Test
        @DisplayName("正規ヘッダー＋CSRFトークン付きPOST→CSRFで拒否されない(302)")
        void post_認証あり_CSRFあり_302() throws Exception {
            mockMvc.perform(withTrustHeaders(post("/v1/document/analysis"), "alice", "ADMIN")
                            .param("fromToDate", "01/01/2024 - 01/31/2024")
                            .with(csrf()))
                    .andExpect(status().isFound());
        }

        @Test
        @DisplayName("正規ヘッダーだがCSRFトークンなしPOST→403を返す")
        void post_認証あり_CSRFなし_403() throws Exception {
            mockMvc.perform(withTrustHeaders(post("/v1/document/analysis"), "alice", "ADMIN")
                            .param("fromToDate", "01/01/2024 - 01/31/2024"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("セキュリティヘッダー のテスト")
    class SecurityHeaders {

        @Test
        @DisplayName("フォームログイン構成と同一のセキュリティヘッダーが付与される")
        void header_formLogin構成と同一() throws Exception {
            mockMvc.perform(withTrustHeaders(get("/v3/index"), "alice", "ADMIN").secure(true))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                    .andExpect(header().string("Referrer-Policy", "same-origin"))
                    .andExpect(header().exists("Strict-Transport-Security"));
        }
    }

    private static MockHttpServletRequestBuilder withTrustHeaders(
            final MockHttpServletRequestBuilder builder, final String user, final String role) throws Exception {
        final long now = Instant.now().getEpochSecond();
        return builder
                .header("X-Orbit-Auth-User", user)
                .header("X-Orbit-Auth-Role", role)
                .header("X-Orbit-Auth-Timestamp", String.valueOf(now))
                .header("X-Orbit-Auth-Signature", sign(user, role, now));
    }

    private static String sign(final String user, final String role, final long epochSeconds) throws Exception {
        final Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                mac.doFinal((user + "\n" + role + "\n" + epochSeconds).getBytes(StandardCharsets.UTF_8)));
    }
}
