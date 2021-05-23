package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile({"prod"})
public class CompanyScheduler {

    private final CompanyUseCase companyUseCase;
    private final SlackClient slackClient;

    public CompanyScheduler(
            final CompanyUseCase companyUseCase,
            final SlackClient slackClient) {
        this.companyUseCase = companyUseCase;
        this.slackClient = slackClient;
    }

    /**
     * 会社情報更新スケジューラ
     */
    @Scheduled(cron = "${app.scheduler.cron.company}", zone = "Asia/Tokyo")
    public void companyScheduler() {
        FundanalyzerLogClient.logProcessStart(Category.SCHEDULER, Process.COMPANY);

        try {
            companyUseCase.importCompanyInfo();

            FundanalyzerLogClient.logProcessEnd(Category.SCHEDULER, Process.COMPANY);
        } catch (Throwable t) {
            // Slack通知
            slackClient.sendMessage("g.c.i.f.web.scheduler.notice.error", t);
            throw new FundanalyzerRuntimeException("スケジューラ処理中に想定外のエラーが発生しました。", t);
        }
    }
}
