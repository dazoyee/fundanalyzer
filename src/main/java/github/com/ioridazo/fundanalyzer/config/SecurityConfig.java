package github.com.ioridazo.fundanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

/**
 * フォームログイン認証・CSRF・セキュリティヘッダーを構成する Web セキュリティ設定。
 *
 * <p>全リクエストを認証必須とし、利用者は {@code app.security.user} / {@code app.security.password}
 * から構成する単一のメモリ内ユーザーのみ。CSRF は有効のまま維持し、Thymeleaf フォームへ自動でトークンを注入する。
 */
@Configuration
public class SecurityConfig {

    /** HSTS の max-age（秒）。1 年（推奨値）。 */
    private static final long HSTS_MAX_AGE_SECONDS = 31_536_000L;

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
     * @param http HttpSecurity
     * @return セキュリティフィルターチェーン
     * @throws Exception 構成時の例外
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/v3/index", true)
                        .permitAll())
                .httpBasic(Customizer.withDefaults())
                .logout(Customizer.withDefaults())
                .addFilterBefore(new DisableEncodeUrlFilter(), UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(false)
                                .maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)));
        return http.build();
    }
}
