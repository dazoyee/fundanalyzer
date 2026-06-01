package github.com.ioridazo.fundanalyzer;

import github.com.ioridazo.fundanalyzer.config.AnalysisCoefficient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableRetry
@EnableConfigurationProperties(AnalysisCoefficient.class)
public class FundanalyzerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(FundanalyzerApplication.class, args);
    }
}
