package github.com.ioridazo.fundanalyzer.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

/**
 * フォームログイン認証・CSRF・セキュリティヘッダーを構成する Web セキュリティ設定
 * （{@code app.security.mode=form-login}（既定）、ローカル開発・テストで使用）。
 *
 * <p>全リクエストを認証必須とし、利用者は {@code app.security.user} / {@code app.security.password}
 * から構成する単一のメモリ内ユーザーのみ。CSRF は有効のまま維持し、Thymeleaf フォームへ自動でトークンを注入する。
 * 本番は orbit からの署名付きトラストヘッダー認証（{@link TrustHeaderSecurityConfig}）を使用する。
 */
@Configuration
@ConditionalOnProperty(name = "app.security.mode", havingValue = "form-login", matchIfMissing = true)
public class SecurityConfig {

    private final String username;
    private final String password;
    private final String contentSecurityPolicy;

    public SecurityConfig(
            @Value("${app.security.user}") final String username,
            @Value("${app.security.password}") final String password,
            @Value("${app.security.csp}") final String contentSecurityPolicy) {
        this.username = username;
        this.password = password;
        this.contentSecurityPolicy = contentSecurityPolicy;
    }

    /**
     * パスワードエンコーダ。
     *
     * @return BCrypt エンコーダ
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 環境変数由来の認証情報から単一の利用者を構成する。
     *
     * @param passwordEncoder パスワードエンコーダ
     * @return メモリ内ユーザー管理
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsManager(final PasswordEncoder passwordEncoder) {
        final UserDetails user = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    /**
     * セキュリティフィルターチェーンを構成する。
     *
     * <p>Actuator は health のみ未認証で疎通確認を許可し、他エンドポイントは従来どおり認証必須とする。
     *
     * @param http HttpSecurity
     * @return セキュリティフィルターチェーン
     * @throws Exception 構成時の例外
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        final var healthEndpoint = EndpointRequest.to(HealthEndpoint.class);
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                        .requestMatchers(healthEndpoint).permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/v3/index", true)
                        .permitAll())
                .logout(Customizer.withDefaults())
                .addFilterBefore(new DisableEncodeUrlFilter(), UsernamePasswordAuthenticationFilter.class);
        SecurityHeadersCustomizer.apply(http, contentSecurityPolicy);
        return http.build();
    }
}
