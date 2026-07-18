package github.com.ioridazo.fundanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

/**
 * 認証を行わない Web セキュリティ設定。アクセス制御はネットワーク境界（ファイアウォール等）で行う前提とし、
 * アプリケーション層ではセキュリティヘッダー（CSP/HSTS 等）と CSRF 保護のみを維持する。
 */
@Configuration
public class SecurityConfig {

    private final String contentSecurityPolicy;

    public SecurityConfig(@Value("${app.security.csp}") final String contentSecurityPolicy) {
        this.contentSecurityPolicy = contentSecurityPolicy;
    }

    /**
     * セキュリティフィルターチェーンを構成する。
     *
     * @param http HttpSecurity
     * @return セキュリティフィルターチェーン
     * @throws Exception 構成時の例外
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .addFilterBefore(new DisableEncodeUrlFilter(), UsernamePasswordAuthenticationFilter.class);
        SecurityHeadersCustomizer.apply(http, contentSecurityPolicy);
        return http.build();
    }
}
