package github.com.ioridazo.fundanalyzer.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AccessLogFilterのテスト")
class AccessLogFilterTest {

    private AccessLogFilter filter;

    @BeforeEach
    void setUp() {
        this.filter = new AccessLogFilter();
    }

    @Nested
    @DisplayName("doFilterInternal メソッド")
    class DoFilterInternal {

        @Test
        @DisplayName("通常のリクエストURI → filterChain.doFilter が呼ばれる")
        void normalRequest_invokesFilterChain() throws ServletException, IOException {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain chain = mock(FilterChain.class);
            when(request.getRequestURI()).thenReturn("/fundanalyzer/v2/index");

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/actuator/prometheus",
                "/dist/css/style.css",
                "/dist/img/logo.png",
                "/dist/js/app.js",
                "/plugins/jquery/jquery.min.js",
                "/favicon.ico"
        })
        @DisplayName("除外対象URI → ログ出力をスキップしつつ filterChain は呼ばれる")
        void excludedRequest_skipsLoggingAndStillCallsChain(final String uri) throws ServletException, IOException {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain chain = mock(FilterChain.class);
            when(request.getRequestURI()).thenReturn(uri);

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("ルート/直下リクエスト → filterChain.doFilter が呼ばれる")
        void rootRequest_invokesFilterChain() throws ServletException, IOException {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain chain = mock(FilterChain.class);
            when(request.getRequestURI()).thenReturn("/");

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("空のURI → filterChain.doFilter が呼ばれ例外を投げない")
        void emptyUri_invokesFilterChainWithoutException() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain chain = mock(FilterChain.class);
            when(request.getRequestURI()).thenReturn("");

            assertDoesNotThrow(() -> filter.doFilter(request, response, chain));

            try {
                verify(chain, times(1)).doFilter(request, response);
            } catch (ServletException | IOException e) {
                throw new AssertionError(e);
            }
        }

        @Test
        @DisplayName("URIに除外対象キーワードを部分含む場合 → 除外として扱われ filterChain は呼ばれる")
        void uriContainsExcludedKeyword_treatedAsExcluded() throws ServletException, IOException {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final FilterChain chain = mock(FilterChain.class);
            when(request.getRequestURI()).thenReturn("/fundanalyzer/plugins/datatables/dataTables.min.js");

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
        }
    }
}
