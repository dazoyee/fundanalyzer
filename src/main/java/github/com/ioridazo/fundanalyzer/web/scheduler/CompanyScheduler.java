package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.proxy.slack.SlackProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile({"prod"})
public class CompanyScheduler {

    private final CompanyUseCase companyUseCase;
    private final SlackProxy slackProxy;

    public CompanyScheduler(
            final CompanyUseCase companyUseCase,
            final SlackProxy slackProxy) {
        this.companyUseCase = companyUseCase;
        this.slackProxy = slackProxy;
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
            slackProxy.sendMessage("g.c.i.f.web.scheduler.notice.error", t);
            throw new FundanalyzerRuntimeException("スケジューラ処理中に想定外のエラーが発生しました。", t);
        }
    }
}
