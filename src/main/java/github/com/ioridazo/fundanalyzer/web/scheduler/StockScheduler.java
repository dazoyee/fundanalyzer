package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile({"prod"})
public class StockScheduler {

    private static final Logger log = LogManager.getLogger(StockScheduler.class);

    private final AnalysisService analysisService;
    private final DocumentSpecification documentSpecification;
    private final SlackClient slackClient;

    @Value("${app.scheduler.hour.stock}")
    int hourOfStock;

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

    public LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 株価更新スケジューラ
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Tokyo")
    public void stockScheduler() {
        if (nowLocalDateTime().getHour() == hourOfStock) {

            final long startTime = System.currentTimeMillis();

            log.info(FundanalyzerLogClient.toAccessLogObject(Category.SCHEDULER, Process.BEGINNING, "stockScheduler", 0));

            try {
                final String dayOfMonth = String.valueOf(nowLocalDate().getDayOfMonth());
                final List<LocalDate> targetList = documentSpecification.stockSchedulerTargetList(dayOfMonth);
                targetList.stream()
                        .map(DateInputData::of)
                        .forEach(analysisService::importStock);

                slackClient.sendMessage("g.c.i.f.web.scheduler.notice.info", targetList.size());

                final long durationTime = System.currentTimeMillis() - startTime;

                log.info(FundanalyzerLogClient.toAccessLogObject(Category.SCHEDULER, Process.END, "stockScheduler", durationTime));
            } catch (Throwable t) {
                // slack通知
                slackClient.sendMessage("g.c.i.f.web.scheduler.notice.error", t);
                throw new FundanalyzerRuntimeException("スケジューラ処理中に想定外のエラーが発生しました。", t);
            }
        }
    }
}
