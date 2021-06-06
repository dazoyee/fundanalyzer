package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile({"prod"})
public class CompanyScheduler {

    private static final Logger log = LogManager.getLogger(CompanyScheduler.class);

    private final CompanyUseCase companyUseCase;
    private final SlackClient slackClient;

    @Value("${app.scheduler.hour.company}")
    int hourOfCompany;

    public CompanyScheduler(
            final CompanyUseCase companyUseCase,
            final SlackClient slackClient) {
        this.companyUseCase = companyUseCase;
        this.slackClient = slackClient;
    }

    public LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 会社情報更新スケジューラ
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Tokyo")
    public void companyScheduler() {
        if (nowLocalDateTime().getHour() == hourOfCompany) {

            final long startTime = System.currentTimeMillis();

            log.info(FundanalyzerLogClient.toAccessLogObject(Category.SCHEDULER, Process.BEGINNING, "companyScheduler", 0));

            try {
                companyUseCase.importCompanyInfo();

                final long durationTime = System.currentTimeMillis() - startTime;

                log.info(FundanalyzerLogClient.toAccessLogObject(Category.SCHEDULER, Process.END, "companyScheduler", durationTime));
            } catch (Throwable t) {
                // Slack通知
                slackClient.sendMessage("g.c.i.f.web.scheduler.notice.error", "会社情報更新", t);
                throw new FundanalyzerRuntimeException("会社情報更新スケジューラ処理中に想定外のエラーが発生しました。", t);
            }
        }
    }
}
