package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.service.StockService;
import github.com.ioridazo.fundanalyzer.domain.util.Target;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.proxy.slack.SlackProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Profile({"prod"})
public class StockScheduler {

    private final StockService stockService;
    private final SlackProxy slackProxy;
    private final DocumentDao documentDao;

    public StockScheduler(
            final StockService stockService,
            final SlackProxy slackProxy,
            final DocumentDao documentDao) {
        this.stockService = stockService;
        this.slackProxy = slackProxy;
        this.documentDao = documentDao;
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
            final List<LocalDate> targetList = documentDao.selectByDayOfSubmitDate(dayOfMonth).stream()
                    .map(Document::getSubmitDate)
                    .distinct()
                    .collect(Collectors.toList());

            targetList.forEach(submitDate -> stockService.importStockPrice(submitDate, Target.annualSecuritiesReport()));

            slackProxy.sendMessage("g.c.i.f.web.scheduler.notice.info", targetList.size());
            FundanalyzerLogClient.logProcessEnd(Category.SCHEDULER, Process.IMPORT);
        } catch (Throwable t) {
            // slack通知
            slackProxy.sendMessage("g.c.i.f.web.scheduler.notice.error", t);
            throw new FundanalyzerRuntimeException("スケジューラ処理中に想定外のエラーが発生しました。", t);
        }
    }
}
