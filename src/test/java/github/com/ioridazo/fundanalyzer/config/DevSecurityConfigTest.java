package github.com.ioridazo.fundanalyzer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * dev プロファイル専用セキュリティ設定（H2 コンソールへのアクセス許可）を検証する統合テスト。
 *
 * <p>H2 コンソールは DispatcherServlet を経由しない専用 Servlet で処理されるため、
 * MockMvc では実サーブレットを経由できない（常に 404 になる）。実際の埋め込みサーバーで
 * 検証するため {@code webEnvironment = RANDOM_PORT} を使用する。
 *
 * <p>securityMatcher が MVC ベースの照合のままだと主チェーン（CSRF 有効）に
 * フォールバックし 403 Forbidden となる回帰を防ぐ。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@DisplayName("DevSecurityConfigの統合テスト")
class DevSecurityConfigTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    @DisplayName("CSRFトークンなしでH2コンソールにPOST→403にならない")
    void h2ConsoleLogin_CSRFなし_403にならない() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("driver", "org.h2.Driver");
        form.add("url", "jdbc:h2:mem:fundanalyzer");
        form.add("user", "sa");
        form.add("password", "");

        final ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/fundanalyzer/h2-console/login.do",
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                String.class);

        assertNotEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("H2コンソールのレスポンスにX-Frame-Options: SAMEORIGINが付与される")
    void h2Console_frameOptionsSameOrigin() {
        final ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/fundanalyzer/h2-console/login.jsp", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SAMEORIGIN", response.getHeaders().getFirst("X-Frame-Options"));
    }
}
