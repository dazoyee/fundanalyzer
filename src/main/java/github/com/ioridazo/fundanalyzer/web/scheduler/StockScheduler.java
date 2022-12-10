package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile({"prod"})
public class StockScheduler {

    private static final Logger log = LogManager.getLogger(StockScheduler.class);

    private final AnalysisService analysisService;
    private final StockSpecification stockSpecification;
    private final SlackClient slackClient;

    @Value("${app.scheduler.hour.stock}")
    int hourOfStock;
    @Value("${app.scheduler.hour.evaluate}")
    int hourOfEvaluate;
    @Value("${app.slack.insert-stock.enabled:true}")
    boolean insertStockEnabled;
    @Value("${app.slack.delete-stock.enabled:true}")
    boolean deleteStockEnabled;
    @Value("${app.slack.evaluate.enabled:true}")
    boolean evaluateEnabled;

    public StockScheduler(
            final AnalysisService analysisService,
            final StockSpecification stockSpecification,
            final SlackClient slackClient) {
        this.analysisService = analysisService;
        this.stockSpecification = stockSpecification;
        this.slackClient = slackClient;
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

            log.info(FundanalyzerLogClient.toAccessLogObject(
                    Category.SCHEDULER,
                    Process.BEGINNING,
                    "stockScheduler",
                    0
            ));

            try {
                insert();
                delete();
            } catch (Throwable t) {
                // slack通知
                slackClient.sendMessage("g.c.i.f.web.scheduler.notice.error", "株価更新", t);
                throw new FundanalyzerRuntimeException("株価更新スケジューラ処理中に想定外のエラーが発生しました。", t);
            }
        }
    }

    /**
     * 株価評価スケジューラ
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Tokyo")
    public void evaluateScheduler() {
        if (nowLocalDateTime().getHour() == hourOfEvaluate) {

            log.info(FundanalyzerLogClient.toAccessLogObject(
                    Category.SCHEDULER,
                    Process.BEGINNING,
                    "evaluateScheduler",
                    0
            ));

            try {
                final long startTime = System.currentTimeMillis();

                final int countValuation = analysisService.evaluate();

                if (evaluateEnabled) {
                    slackClient.sendMessage("github.com.ioridazo.fundanalyzer.web.scheduler.StockScheduler.evaluate", countValuation);
                }
                log.info(FundanalyzerLogClient.toAccessLogObject(
                        Category.SCHEDULER,
                        Process.END,
                        "evaluateScheduler",
                        System.currentTimeMillis() - startTime
                ));
            } catch (Throwable t) {
                // slack通知
                slackClient.sendMessage("g.c.i.f.web.scheduler.notice.error", "株価評価", t);
                throw new FundanalyzerRuntimeException("株価評価スケジューラ処理中に想定外のエラーが発生しました。", t);
            }
        }
    }

    /**
     * 株価を取得する
     */
    private void insert() {
        final long startTime = System.currentTimeMillis();

        final List<String> targetCodeList = stockSpecification.findTargetCodeForStockScheduler();
        targetCodeList.stream()
                .map(CodeInputData::of)
                .forEach(inputData -> {
                    analysisService.importStock(inputData);
                    analysisService.indicate(inputData);
                });

        if (insertStockEnabled) {
            slackClient.sendMessage("github.com.ioridazo.fundanalyzer.web.scheduler.StockScheduler.insert", targetCodeList.size());
        }

        final long durationTime = System.currentTimeMillis() - startTime;

        log.info(FundanalyzerLogClient.toAccessLogObject(
                Category.SCHEDULER,
                Process.END,
                "insertStockScheduler",
                durationTime
        ));
    }

    /**
     * 過去の株価を削除する
     */
    private void delete() {
        final long startTime = System.currentTimeMillis();

        final int deleteStock = analysisService.deleteStock();

        if (deleteStockEnabled) {
            slackClient.sendMessage("github.com.ioridazo.fundanalyzer.web.scheduler.StockScheduler.delete", deleteStock);
        }

        final long durationTime = System.currentTimeMillis() - startTime;

        log.info(FundanalyzerLogClient.toAccessLogObject(
                Category.SCHEDULER,
                Process.END,
                "deleteStockScheduler",
                durationTime
        ));
    }
}
