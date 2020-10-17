package github.com.ioridazo.fundanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FundanalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundanalyzerApplication.class, args);
    }
}
