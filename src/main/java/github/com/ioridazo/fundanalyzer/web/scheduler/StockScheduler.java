package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile({"prod"})
public class StockScheduler {

    private final StockService stockService;
    private final DocumentDao documentDao;

    public StockScheduler(
            final StockService stockService,
            final DocumentDao documentDao) {
        this.stockService = stockService;
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
        try {
            final String dayOfMonth = String.valueOf(nowLocalDate().getDayOfMonth());
            log.info("[スケジューラ]株価更新処理を開始します。対象日:提出日の'日'が'{}'の企業", dayOfMonth);
            final List<LocalDate> targetList = documentDao.selectByDayOfSubmitDate(dayOfMonth).stream()
                    .map(Document::getSubmitDate)
                    .distinct()
                    .collect(Collectors.toList());

            if (targetList.isEmpty()) {
                log.info("[スケジューラ]株価更新処理対象の企業はありませんでした。");
            } else {
                log.info("[スケジューラ]株価更新処理対象の企業は{}件ありました。後続の処理を開始します。", targetList.size());
                targetList.forEach(stockService::importStockPrice);
            }
        } catch (Throwable t) {
            // slack通知
            throw t;
        }
    }
}
