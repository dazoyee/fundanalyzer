package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.usecase.SystemEventUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ValuationUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.ValuationCatchUpResult;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile({"prod"})
public class StockScheduler {

    private static final Logger log = LogManager.getLogger(StockScheduler.class);

    private final AnalysisService analysisService;
    private final StockSpecification stockSpecification;
    private final ValuationUseCase valuationUseCase;
    private final SystemEventUseCase systemEventUseCase;

    @Value("${app.scheduler.hour.stock}")
    List<Integer> hourOfStock;
    @Value("${app.scheduler.hour.evaluate}")
    int hourOfEvaluate;

    @Autowired
    public StockScheduler(
            final AnalysisService analysisService,
            final StockSpecification stockSpecification,
            final ValuationUseCase valuationUseCase,
            final SystemEventUseCase systemEventUseCase) {
        this.analysisService = analysisService;
        this.stockSpecification = stockSpecification;
        this.valuationUseCase = valuationUseCase;
        this.systemEventUseCase = systemEventUseCase;
    }

    public StockScheduler(
            final AnalysisService analysisService,
            final StockSpecification stockSpecification,
            final SystemEventUseCase systemEventUseCase) {
        this(analysisService, stockSpecification, null, systemEventUseCase);
    }

    public LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 株価更新スケジューラ
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Tokyo")
    public void stockScheduler() {
        if (hourOfStock.stream().anyMatch(integer -> nowLocalDateTime().getHour() == integer)) {

            log.info(FundanalyzerLogClient.toAccessLogObject(
                    Category.SCHEDULER,
                    Process.BEGINNING,
                    "stockScheduler",
                    0
            ));

            try {
                insert();
                // delete();
            } catch (Throwable t) {
                systemEventUseCase.record(SystemEventType.ERROR, "StockScheduler", buildErrorMessage("株価更新", t));
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
                final ValuationCatchUpResult catchUpResult = valuationUseCase == null
                        ? new ValuationCatchUpResult(0, 0, 0)
                        : valuationUseCase.catchUp();

                log.info(FundanalyzerLogClient.toAccessLogObject(
                        Category.SCHEDULER,
                        Process.END,
                        MessageFormat.format(
                                "valuationCatchUp target={0} advanced={1} unresolved={2}",
                                catchUpResult.targetCompanyCount(),
                                catchUpResult.advancedCount(),
                                catchUpResult.unresolvedCompanyCount()
                        ),
                        System.currentTimeMillis() - startTime
                ));
                log.info(FundanalyzerLogClient.toAccessLogObject(
                        Category.SCHEDULER,
                        Process.END,
                        "evaluateScheduler count=" + countValuation,
                        System.currentTimeMillis() - startTime
                ));
            } catch (Throwable t) {
                systemEventUseCase.record(SystemEventType.ERROR, "StockScheduler", buildErrorMessage("株価評価", t));
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
                    try {
                        analysisService.importStock(inputData);
                    } catch (final Exception e) {
                        log.warn(FundanalyzerLogClient.toInteractorLogObject(
                                MessageFormat.format(
                                        "株価取得処理で例外が発生したため、当該企業をスキップして継続します。\t企業コード:{0}",
                                        inputData.getCode()
                                ),
                                Category.STOCK,
                                Process.IMPORT
                        ), e);
                    }
                });

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

        final long durationTime = System.currentTimeMillis() - startTime;

        log.info(FundanalyzerLogClient.toAccessLogObject(
                Category.SCHEDULER,
                Process.END,
                "deleteStockScheduler count=" + deleteStock,
                durationTime
        ));
    }

    private String buildErrorMessage(final String label, final Throwable throwable) {
        return label + ": " + throwable;
    }
}
