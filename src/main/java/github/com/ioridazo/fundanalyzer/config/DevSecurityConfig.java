package github.com.ioridazo.fundanalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * dev プロファイル専用のセキュリティ設定。H2 コンソールへのアクセスを許可する。
 *
 * <p>H2 コンソールは iframe を使用するため {@code X-Frame-Options: SAMEORIGIN} を設定し、
 * 独自の CSRF トークン機構を持つため CSRF 保護を無効化する。
 * このチェーンは {@link SecurityConfig} の主チェーン（Order=100）より先に評価される。
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
        http
                .securityMatcher("/h2-console/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        return http.build();
    }
}
