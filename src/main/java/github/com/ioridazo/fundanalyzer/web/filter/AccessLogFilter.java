package github.com.ioridazo.fundanalyzer.web.filter;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(AccessLogFilter.class);

    private static final String ACTUATOR_URI = "/actuator/prometheus";

    @Override
    protected void doFilterInternal(
            @NotNull final HttpServletRequest httpServletRequest,
            @NotNull final HttpServletResponse httpServletResponse,
            final FilterChain filterChain) throws ServletException, IOException {
        final long startTime = System.currentTimeMillis();

        final String requestURI = httpServletRequest.getRequestURI();

        if (!ACTUATOR_URI.equals(requestURI)) {
            log.info(FundanalyzerLogClient.toAccessLogObject(Category.ACCESS, Process.BEGINNING, requestURI, 0));
        }

        // ***********************************************************
        filterChain.doFilter(httpServletRequest, httpServletResponse);
        // ***********************************************************

        final long durationTime = System.currentTimeMillis() - startTime;

        if (!ACTUATOR_URI.equals(requestURI)) {
            log.info(FundanalyzerLogClient.toAccessLogObject(Category.ACCESS, Process.END, requestURI, durationTime));
        }
    }
}
