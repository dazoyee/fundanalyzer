package github.com.ioridazo.fundanalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * dev プロファイル専用のセキュリティ設定。H2 コンソールへのアクセスを許可する。
 *
 * <p>H2 コンソールは iframe を使用するため {@code X-Frame-Options: SAMEORIGIN} を設定し、
 * 独自の CSRF トークン機構を持つため CSRF 保護を無効化する。
 * このチェーンは {@code @Order(1)} で明示的に優先度を指定しており、
 * 優先度を指定しない {@link SecurityConfig} の主チェーンより先に評価される。
 */
@Configuration
@Profile("dev")
public class DevSecurityConfig {

    /**
     * H2 コンソール専用のセキュリティフィルターチェーン。
     *
     * @param http HttpSecurity
     * @return H2 コンソール専用フィルターチェーン
     * @throws Exception 構成時の例外
     */
    @Bean
    @Order(1)
    public SecurityFilterChain h2ConsoleSecurityFilterChain(final HttpSecurity http) throws Exception {
        // H2 コンソールは DispatcherServlet を経由しない専用 Servlet で処理されるため、
        // デフォルトの MVC ベース securityMatcher（HandlerMappingIntrospector 依存）ではマッチせず
        // 主チェーンにフォールバックしてしまう。AntPathRequestMatcher で明示的にパス一致させる。
        http
                .securityMatcher(new AntPathRequestMatcher("/h2-console/**"))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        return http.build();
    }
}
