package github.com.ioridazo.fundanalyzer.config;

import github.com.ioridazo.fundanalyzer.web.filter.AccessLogFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<AccessLogFilter> filter() {
        final FilterRegistrationBean<AccessLogFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new AccessLogFilter());
        return bean;
    }

    /**
     * コンテキストルートへの直行（リバースプロキシのリンク先が {@code /fundanalyzer} のため発生）を
     * トップ画面へ誘導する。
     */
    @Override
    public void addViewControllers(final ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/v3/index");
    }
}
