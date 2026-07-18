package github.com.ioridazo.fundanalyzer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 認証を行わない Web セキュリティ設定（CSRF・セキュリティヘッダーのみ）を検証する統合テスト。
 */
@SpringBootTest(properties = {
        "management.server.port=",
        "management.endpoints.web.exposure.include=health"
})
@AutoConfigureMockMvc
@DisplayName("SecurityConfigの統合テスト")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("認証なし のテスト")
    class NoAuthentication {

        @Test
        @DisplayName("未認証で画面にアクセス→200を返す")
        void index_未認証_200() throws Exception {
            mockMvc.perform(get("/v3/index").accept(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("未認証で分析画面にアクセス→200を返す")
        void analysis_未認証_200() throws Exception {
            mockMvc.perform(get("/v3/analysis").accept(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("未認証で静的リソースにアクセス→200を返す")
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
        @DisplayName("CSRFトークン付きPOST→CSRFで拒否されない(302)")
        void post_CSRFあり_302() throws Exception {
            mockMvc.perform(post("/v1/document/analysis")
                            .param("fromToDate", "01/01/2024 - 01/31/2024")
                            .with(csrf()))
                    .andExpect(status().isFound());
        }

        @Test
        @DisplayName("CSRFトークンなしPOST→403を返す")
        void post_CSRFなし_403() throws Exception {
            mockMvc.perform(post("/v1/document/analysis")
                            .param("fromToDate", "01/01/2024 - 01/31/2024"))
                    .andExpect(status().isForbidden());
        }

    }

    @Nested
    @DisplayName("セキュリティヘッダー のテスト")
    class SecurityHeaders {

        @Test
        @DisplayName("X-Content-Type-Options: nosniff が付与される")
        void header_xContentTypeOptions() throws Exception {
            mockMvc.perform(get("/v3/index"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("X-Frame-Options: DENY が付与される")
        void header_xFrameOptions() throws Exception {
            mockMvc.perform(get("/v3/index"))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        }

        @Test
        @DisplayName("Content-Security-Policy に default-src/object-src ディレクティブが含まれる")
        void header_contentSecurityPolicy() throws Exception {
            mockMvc.perform(get("/v3/index"))
                    .andExpect(header().exists("Content-Security-Policy"))
                    .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                    .andExpect(header().string("Content-Security-Policy", containsString("object-src 'none'")));
        }

        @Test
        @DisplayName("Referrer-Policy: same-origin が付与される")
        void header_referrerPolicy() throws Exception {
            mockMvc.perform(get("/v3/index"))
                    .andExpect(header().string("Referrer-Policy", "same-origin"));
        }

        @Test
        @DisplayName("HTTPSリクエストで Strict-Transport-Security が付与される")
        void header_hsts() throws Exception {
            mockMvc.perform(get("/v3/index").secure(true))
                    .andExpect(header().exists("Strict-Transport-Security"));
        }
    }
}
