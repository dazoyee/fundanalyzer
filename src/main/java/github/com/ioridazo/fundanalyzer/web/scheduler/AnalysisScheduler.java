package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import github.com.ioridazo.fundanalyzer.domain.service.StockService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile({"prod"})
public class AnalysisScheduler {

    private final DocumentService documentService;
    private final AnalysisService analysisService;
    private final StockService stockService;
    private final ViewService viewService;
    private final DocumentDao documentDao;

    public AnalysisScheduler(
            final DocumentService documentService,
            final AnalysisService analysisService,
            final StockService stockService,
            final ViewService viewService,
            final DocumentDao documentDao) {
        this.documentService = documentService;
        this.analysisService = analysisService;
        this.stockService = stockService;
        this.viewService = viewService;
        this.documentDao = documentDao;
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
            documentDao.selectByDocumentTypeCode("120").stream()
                    .map(Document::getSubmitDate)
                    // データベースの最新提出日を取得
                    .max(LocalDate::compareTo)
                    // 次の日から
                    .map(submitDate -> submitDate.plusDays(1))
                    .orElse(nowLocalDate())
                    // 本日までを対象に財務分析を実行
                    .datesUntil(nowLocalDate())
                    .forEach(date -> {
                        // execute実行
                        documentService.execute(date.toString(), "120")
                                // execute完了後、analyze実行
                                .thenAcceptAsync(unused -> analysisService.analyze(date))
                                // analyze完了後、importStockPrice実行
                                .thenAcceptAsync(unused -> stockService.importStockPrice(date))
                                // importStockPrice完了後、notice実行
                                .thenAcceptAsync(unused -> viewService.notice(date));
                    });

            FundanalyzerLogClient.logProcessEnd(Category.SCHEDULER, Process.ANALYSIS);
        } catch (Throwable t) {
            // Slack通知
            throw t;
        }
    }

    /**
     * 画面更新スケジューラ
     */
    @Scheduled(cron = "${app.scheduler.cron.update-view}", zone = "Asia/Tokyo")
    public void updateViewScheduler() {
        FundanalyzerLogClient.logProcessStart(Category.SCHEDULER, Process.UPDATE);

        try {
            viewService.updateCorporateView();
            viewService.updateEdinetListView("120");

            FundanalyzerLogClient.logProcessEnd(Category.SCHEDULER, Process.UPDATE);
        } catch (Throwable t) {
            // Slack通知
            throw t;
        }
    }
}
