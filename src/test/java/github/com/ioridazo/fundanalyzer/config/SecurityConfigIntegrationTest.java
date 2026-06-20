package github.com.ioridazo.fundanalyzer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Security の認証・CSRF・セキュリティヘッダーを検証する統合テスト。
 */
@SpringBootTest(properties = {"app.security.user=testuser", "app.security.password=testpass"})
@AutoConfigureMockMvc
@DisplayName("SecurityConfigの統合テスト")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("認証 のテスト")
    class Authentication {

        @Test
        @DisplayName("未認証で画面にアクセス→ログインページへリダイレクト")
        void index_未認証_302() throws Exception {
            mockMvc.perform(get("/v3/index"))
                    .andExpect(status().isFound());
        }

        @Test
        @DisplayName("正しい認証情報でフォームログイン→認証済みセッションになる")
        void formLogin_正認証_authenticated() throws Exception {
            mockMvc.perform(formLogin("/login").user("testuser").password("testpass"))
                    .andExpect(authenticated());
        }

        @Test
        @DisplayName("誤った認証情報でフォームログイン→未認証のまま")
        void formLogin_誤認証_unauthenticated() throws Exception {
            mockMvc.perform(formLogin("/login").user("testuser").password("wrongpass"))
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("未認証で業務POSTにアクセス→ログインページへリダイレクト")
        void post_未認証_302() throws Exception {
            mockMvc.perform(post("/v1/document/analysis").with(csrf()))
                    .andExpect(status().isFound());
        }

        @Test
        @DisplayName("未認証で静的リソースにアクセス→permitAllで200を返す")
        void staticResource_未認証_200() throws Exception {
            mockMvc.perform(get("/css/app.css"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("未認証で株価評価画面にアクセス→ログインページへリダイレクト")
        void valuation_未認証_302() throws Exception {
            mockMvc.perform(get("/v3/valuation"))
                    .andExpect(status().isFound());
        }
    }

    @Nested
    @DisplayName("CSRF のテスト")
    class Csrf {

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("認証済み＋CSRFトークン付きPOST→CSRFで拒否されない(302)")
        void post_認証あり_CSRFあり_302() throws Exception {
            mockMvc.perform(post("/v1/document/analysis")
                            .param("fromToDate", "01/01/2024 - 01/31/2024")
                            .with(csrf()))
                    .andExpect(status().isFound());
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("認証済みだがCSRFトークンなしPOST→403を返す")
        void post_認証あり_CSRFなし_403() throws Exception {
            mockMvc.perform(post("/v1/document/analysis")
                            .param("fromToDate", "01/01/2024 - 01/31/2024"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("セキュリティヘッダー のテスト")
    class SecurityHeaders {

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("X-Content-Type-Options: nosniff が付与される")
        void header_xContentTypeOptions() throws Exception {
            mockMvc.perform(get("/v3/index"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("X-Frame-Options: DENY が付与される")
        void header_xFrameOptions() throws Exception {
            mockMvc.perform(get("/v3/index"))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Content-Security-Policy に default-src/object-src ディレクティブが含まれる")
        void header_contentSecurityPolicy() throws Exception {
            mockMvc.perform(get("/v3/index"))
                    .andExpect(header().exists("Content-Security-Policy"))
                    .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                    .andExpect(header().string("Content-Security-Policy", containsString("object-src 'none'")));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Referrer-Policy: same-origin が付与される")
        void header_referrerPolicy() throws Exception {
            mockMvc.perform(get("/v3/index"))
                    .andExpect(header().string("Referrer-Policy", "same-origin"));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("HTTPSリクエストで Strict-Transport-Security が付与される")
        void header_hsts() throws Exception {
            mockMvc.perform(get("/v3/index").secure(true))
                    .andExpect(header().exists("Strict-Transport-Security"));
        }
    }
}
