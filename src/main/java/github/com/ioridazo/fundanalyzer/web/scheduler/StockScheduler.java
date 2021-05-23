package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Profile({"prod"})
public class StockScheduler {

    private final AnalysisService analysisService;
    private final DocumentSpecification documentSpecification;
    private final SlackClient slackClient;

    public StockScheduler(
            final AnalysisService analysisService,
            final DocumentSpecification documentSpecification,
            final SlackClient slackClient) {
        this.analysisService = analysisService;
        this.documentSpecification = documentSpecification;
        this.slackClient = slackClient;
    }

    LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    /**
     * 株価更新スケジューラ
     */
    @Scheduled(cron = "${app.scheduler.cron.stock}", zone = "Asia/Tokyo")
    public void stockScheduler() {
        FundanalyzerLogClient.logProcessStart(Category.SCHEDULER, Process.IMPORT);

        try {
            final String dayOfMonth = String.valueOf(nowLocalDate().getDayOfMonth());
            final List<LocalDate> targetList = documentSpecification.stockSchedulerTargetList(dayOfMonth);
            targetList.stream()
                    .map(DateInputData::of)
                    .forEach(analysisService::importStock);

            slackClient.sendMessage("g.c.i.f.web.scheduler.notice.info", targetList.size());
            FundanalyzerLogClient.logProcessEnd(Category.SCHEDULER, Process.IMPORT);
        } catch (Throwable t) {
            // slack通知
            slackClient.sendMessage("g.c.i.f.web.scheduler.notice.error", t);
            throw new FundanalyzerRuntimeException("スケジューラ処理中に想定外のエラーが発生しました。", t);
        }
    }
}
