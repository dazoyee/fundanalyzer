package github.com.ioridazo.fundanalyzer.config;

import github.com.ioridazo.fundanalyzer.web.filter.AccessLogFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<AccessLogFilter> filter() {
        final FilterRegistrationBean<AccessLogFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new AccessLogFilter());
        return bean;
    }
}
