package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Profile({"prod"})
public class AnalysisScheduler {

    private static final Logger log = LogManager.getLogger(AnalysisScheduler.class);

    private final AnalysisService analysisService;
    private final ViewService viewService;
    private final SlackClient slackClient;

    @Value("${app.scheduler.hour.analysis}")
    int hourOfAnalysis;
    @Value("${app.scheduler.hour.update-view}")
    int hourOfUpdateView;
    @Value("${app.scheduler.analysis.past-days}")
    int pastDaysForAnalysis;

    public AnalysisScheduler(
            final AnalysisService analysisService,
            final ViewService viewService,
            final SlackClient slackClient) {
        this.analysisService = analysisService;
        this.viewService = viewService;
        this.slackClient = slackClient;
    }

    public LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    public LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 財務分析スケジューラ
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Tokyo")
    public void analysisScheduler() {
        if (nowLocalDateTime().getHour() == hourOfAnalysis) {

            final long startTime = System.currentTimeMillis();

            log.info(FundanalyzerLogClient.toAccessLogObject(Category.SCHEDULER, Process.BEGINNING, "analysisScheduler", 0));

            try {
                final LocalDate nowLocalDate = nowLocalDate();
                analysisService.executeAllMain(
                        BetweenDateInputData.of(nowLocalDate.minusDays(pastDaysForAnalysis), nowLocalDate));

                final long durationTime = System.currentTimeMillis() - startTime;

                log.info(FundanalyzerLogClient.toAccessLogObject(Category.SCHEDULER, Process.END, "analysisScheduler", durationTime));
            } catch (Throwable t) {
                // Slack通知
                slackClient.sendMessage("g.c.i.f.web.scheduler.notice.error", "財務分析", t);
                throw new FundanalyzerRuntimeException("財務分析スケジューラ処理中に想定外のエラーが発生しました。", t);
            }
        }
    }

    /**
     * 画面更新スケジューラ
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Tokyo")
    public void updateViewScheduler() {
        if (nowLocalDateTime().getHour() == hourOfUpdateView) {

            final long startTime = System.currentTimeMillis();

            log.info(FundanalyzerLogClient.toAccessLogObject(Category.SCHEDULER, Process.BEGINNING, "updateViewScheduler", 0));

            try {
                viewService.updateCorporateView();
                viewService.updateEdinetView();

                final long durationTime = System.currentTimeMillis() - startTime;

                log.info(FundanalyzerLogClient.toAccessLogObject(Category.SCHEDULER, Process.END, "updateViewScheduler", durationTime));
            } catch (Throwable t) {
                // Slack通知
                slackClient.sendMessage("g.c.i.f.web.scheduler.notice.error", "画面更新", t);
                throw new FundanalyzerRuntimeException("画面更新スケジューラ処理中に想定外のエラーが発生しました。", t);
            }
        }
    }
}
