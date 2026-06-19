package github.com.ioridazo.fundanalyzer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
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
        @DisplayName("未認証で画面にアクセス→401を返す")
        void index_未認証_401() throws Exception {
            mockMvc.perform(get("/v3/index"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("正しい認証情報で画面にアクセス→200を返す")
        void index_正認証_200() throws Exception {
            mockMvc.perform(get("/v3/index").with(httpBasic("testuser", "testpass")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("誤った認証情報で画面にアクセス→401を返す")
        void index_誤認証_401() throws Exception {
            mockMvc.perform(get("/v3/index").with(httpBasic("testuser", "wrongpass")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("未認証で業務POSTにアクセス→401を返す")
        void post_未認証_401() throws Exception {
            mockMvc.perform(post("/v1/document/analysis").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("未認証で静的リソースにアクセス→401を返す")
        void staticResource_未認証_401() throws Exception {
            mockMvc.perform(get("/css/app.css"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("未認証で株価評価画面にアクセス→401を返す")
        void valuation_未認証_401() throws Exception {
            mockMvc.perform(get("/v3/valuation"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("CSRF のテスト")
    class Csrf {

        @Test
        @DisplayName("認証済み＋CSRFトークン付きPOST→CSRFで拒否されない(403以外)")
        void post_認証あり_CSRFあり_not403() throws Exception {
            mockMvc.perform(post("/v1/document/analysis")
                            .param("fromToDate", "01/01/2024 - 01/31/2024")
                            .with(httpBasic("testuser", "testpass"))
                            .with(csrf()))
                    .andExpect(status().is(org.springframework.http.HttpStatus.FOUND.value()));
        }

        @Test
        @DisplayName("認証済みだがCSRFトークンなしPOST→403を返す")
        void post_認証あり_CSRFなし_403() throws Exception {
            mockMvc.perform(post("/v1/document/analysis")
                            .param("fromToDate", "01/01/2024 - 01/31/2024")
                            .with(httpBasic("testuser", "testpass")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("セキュリティヘッダー のテスト")
    class SecurityHeaders {

        @Test
        @DisplayName("X-Content-Type-Options: nosniff が付与される")
        void header_xContentTypeOptions() throws Exception {
            mockMvc.perform(get("/v3/index").with(httpBasic("testuser", "testpass")))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("X-Frame-Options: DENY が付与される")
        void header_xFrameOptions() throws Exception {
            mockMvc.perform(get("/v3/index").with(httpBasic("testuser", "testpass")))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        }

        @Test
        @DisplayName("Content-Security-Policy に default-src/object-src ディレクティブが含まれる")
        void header_contentSecurityPolicy() throws Exception {
            mockMvc.perform(get("/v3/index").with(httpBasic("testuser", "testpass")))
                    .andExpect(header().exists("Content-Security-Policy"))
                    .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                    .andExpect(header().string("Content-Security-Policy", containsString("object-src 'none'")));
        }

        @Test
        @DisplayName("Referrer-Policy: same-origin が付与される")
        void header_referrerPolicy() throws Exception {
            mockMvc.perform(get("/v3/index").with(httpBasic("testuser", "testpass")))
                    .andExpect(header().string("Referrer-Policy", "same-origin"));
        }

        @Test
        @DisplayName("HTTPSリクエストで Strict-Transport-Security が付与される")
        void header_hsts() throws Exception {
            mockMvc.perform(get("/v3/index").secure(true).with(httpBasic("testuser", "testpass")))
                    .andExpect(header().exists("Strict-Transport-Security"));
        }
    }
}
