package github.com.ioridazo.fundanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableRetry
public class FundanalyzerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(FundanalyzerApplication.class, args);
    }
}
