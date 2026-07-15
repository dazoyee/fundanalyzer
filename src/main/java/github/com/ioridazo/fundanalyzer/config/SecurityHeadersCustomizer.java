package github.com.ioridazo.fundanalyzer.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * 認証方式（フォームログイン / トラストヘッダー）によらず共通のセキュリティヘッダー構成。
 */
final class SecurityHeadersCustomizer {

    /** HSTS の max-age（秒）。1 年（推奨値）。 */
    private static final long HSTS_MAX_AGE_SECONDS = 31_536_000L;

    private SecurityHeadersCustomizer() {
    }

    static void apply(final HttpSecurity http, final String contentSecurityPolicy) throws Exception {
        http.headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy))
                .referrerPolicy(referrer -> referrer.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(false)
                        .maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)));
    }
}
