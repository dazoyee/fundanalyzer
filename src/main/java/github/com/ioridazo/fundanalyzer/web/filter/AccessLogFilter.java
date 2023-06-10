package github.com.ioridazo.fundanalyzer.web.filter;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(AccessLogFilter.class);

    @Override
    protected void doFilterInternal(
            @NotNull final HttpServletRequest httpServletRequest,
            @NotNull final HttpServletResponse httpServletResponse,
            @NotNull final FilterChain filterChain) throws ServletException, IOException {
        final long startTime = System.currentTimeMillis();

        final String requestURI = httpServletRequest.getRequestURI();

        final List<String> removedList = List.of(
                "/actuator/prometheus",
                "/dist/css",
                "/dist/img",
                "/dist/js",
                "/plugins",
                "/favicon.ico"
        );

        if (removedList.stream().noneMatch(requestURI::contains)) {
            log.info(FundanalyzerLogClient.toAccessLogObject(Category.ACCESS, Process.BEGINNING, requestURI, 0));
        }

        // ***********************************************************
        filterChain.doFilter(httpServletRequest, httpServletResponse);
        // ***********************************************************

        final long durationTime = System.currentTimeMillis() - startTime;

        if (removedList.stream().noneMatch(requestURI::contains)) {
            log.info(FundanalyzerLogClient.toAccessLogObject(Category.ACCESS, Process.END, requestURI, durationTime));
        }
    }
}
