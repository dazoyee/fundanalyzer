package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile({"prod"})
public class AnalysisScheduler {

    private final AnalysisService analysisService;
    private final ViewService viewService;
    private final DocumentSpecification documentSpecification;
    private final SlackClient slackClient;

    public AnalysisScheduler(
            final AnalysisService analysisService,
            final ViewService viewService,
            final DocumentSpecification documentSpecification,
            final SlackClient slackClient) {
        this.analysisService = analysisService;
        this.viewService = viewService;
        this.documentSpecification = documentSpecification;
        this.slackClient = slackClient;
    }

    public LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    /**
     * 財務分析スケジューラ
     */
    @Scheduled(cron = "${app.scheduler.cron.analysis}", zone = "Asia/Tokyo")
    public void analysisScheduler() {
        FundanalyzerLogClient.logProcessStart(Category.SCHEDULER, Process.ANALYSIS);

        try {
            final LocalDate fromDate = documentSpecification.documentList().stream()
                    .map(Document::getSubmitDate)
                    // データベースの最新提出日を取得
                    .max(LocalDate::compareTo)
                    // 次の日から
                    .map(submitDate -> submitDate.plusDays(1))
                    .orElse(nowLocalDate());

            analysisService.doMain(BetweenDateInputData.of(fromDate, nowLocalDate()));

            FundanalyzerLogClient.logProcessEnd(Category.SCHEDULER, Process.ANALYSIS);
        } catch (Throwable t) {
            // Slack通知
            slackClient.sendMessage("g.c.i.f.web.scheduler.notice.error", t);
            throw new FundanalyzerRuntimeException("スケジューラ処理中に想定外のエラーが発生しました。", t);
        }
    }

    /**
     * 画面更新スケジューラ
     */
    @Scheduled(cron = "${app.scheduler.cron.update-view}", zone = "Asia/Tokyo")
    public void updateViewScheduler() {
        FundanalyzerLogClient.logProcessStart(Category.SCHEDULER, Process.UPDATE);

        try {
            viewService.updateView();

            FundanalyzerLogClient.logProcessEnd(Category.SCHEDULER, Process.UPDATE);
        } catch (Throwable t) {
            // Slack通知
            slackClient.sendMessage("g.c.i.f.web.scheduler.notice.error", t);
            throw new FundanalyzerRuntimeException("スケジューラ処理中に想定外のエラーが発生しました。", t);
        }
    }
}
