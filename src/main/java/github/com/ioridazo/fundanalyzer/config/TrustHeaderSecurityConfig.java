package github.com.ioridazo.fundanalyzer.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

/**
 * リバースプロキシ（orbit）からの署名付きトラストヘッダーを唯一の認証手段とする Web セキュリティ設定
 * （{@code app.security.mode=trust-header}、本番プロファイルで使用）。
 *
 * <p>フォームログイン・パスワード認証を持たず、検証不成立のリクエストには 401 を返す。
 * 直アクセスの遮断はバインド制限（{@code server.address}）と署名検証の多層防御で行う。
 * CSRF・セキュリティヘッダー・Actuator health の公開範囲はフォームログイン構成と同一。
 */
@Configuration
@ConditionalOnProperty(name = "app.security.mode", havingValue = "trust-header")
public class TrustHeaderSecurityConfig {

    private final String trustSecret;
    private final String contentSecurityPolicy;

    public TrustHeaderSecurityConfig(
            @Value("${app.security.trust-secret}") final String trustSecret,
            @Value("${app.security.csp}") final String contentSecurityPolicy) {
        this.trustSecret = trustSecret;
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
    public SecurityFilterChain trustHeaderSecurityFilterChain(final HttpSecurity http) throws Exception {
        final var healthEndpoint = EndpointRequest.to(HealthEndpoint.class);
        http
                .authorizeHttpRequests(authorize -> authorize
                        // 404/500 等のエラーページ描画（ERROR ディスパッチ）は認可対象から除外する。
                        // Pre-Authentication は認証をセッション保存せず、OncePerRequestFilter は
                        // 再ディスパッチで実行されないため、除外しないとエラーページが 401 になる。
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/css/**", "/js/**").permitAll()
                        .requestMatchers(healthEndpoint).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new DisableEncodeUrlFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        new TrustHeaderAuthenticationFilter(trustSecret),
                        UsernamePasswordAuthenticationFilter.class);
        SecurityHeadersCustomizer.apply(http, contentSecurityPolicy);
        return http.build();
    }

    /**
     * パスワード認証経路が存在しないことを明示する。
     * Spring Boot の自動構成による「生成パスワード付き既定ユーザー」の作成を抑止する。
     *
     * @return いかなるユーザーも解決しない UserDetailsService
     */
    @Bean
    public UserDetailsService trustHeaderUserDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Password-based login is not available: " + username);
        };
    }
}
