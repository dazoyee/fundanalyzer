package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import github.com.ioridazo.fundanalyzer.domain.service.StockService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.util.Target;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.proxy.slack.SlackProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Profile({"prod"})
public class AnalysisScheduler {

    private final DocumentService documentService;
    private final AnalysisService analysisService;
    private final StockService stockService;
    private final ViewService viewService;
    private final SlackProxy slackProxy;
    private final DocumentDao documentDao;

    public AnalysisScheduler(
            final DocumentService documentService,
            final AnalysisService analysisService,
            final StockService stockService,
            final ViewService viewService,
            final SlackProxy slackProxy,
            final DocumentDao documentDao) {
        this.documentService = documentService;
        this.analysisService = analysisService;
        this.stockService = stockService;
        this.viewService = viewService;
        this.slackProxy = slackProxy;
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

        final List<DocumentTypeCode> targetTypes = Target.annualSecuritiesReport();

        try {
            final List<String> docTypeCode = targetTypes.stream().map(DocumentTypeCode::toValue).collect(Collectors.toList());
            final List<LocalDate> submitDateList = documentDao.selectByDocumentTypeCode(docTypeCode).stream()
                    .map(Document::getSubmitDate)
                    // データベースの最新提出日を取得
                    .max(LocalDate::compareTo)
                    // 次の日から
                    .map(submitDate -> submitDate.plusDays(1))
                    .orElse(nowLocalDate())
                    // 本日までを対象に財務分析を実行
                    .datesUntil(nowLocalDate())
                    .collect(Collectors.toList());

            submitDateList.forEach(date -> {
                // execute実行
                documentService.execute(date.toString(), targetTypes)
                        // execute完了後、analyze実行
                        .thenAcceptAsync(unused -> analysisService.analyze(date, targetTypes))
                        // analyze完了後、importStockPrice実行
                        .thenAcceptAsync(unused -> stockService.importStockPrice(date, targetTypes))
                        // importStockPrice完了後、updateCorporateView実行
                        .thenAcceptAsync(unused -> viewService.updateCorporateView(date, targetTypes))
                        // updateCorporateView完了後、updateEdinetListView実行
                        .thenAcceptAsync(unused -> viewService.updateEdinetListView(date, targetTypes))
                        // updateEdinetListView完了後、notice実行
                        .thenAcceptAsync(unused -> viewService.notice(date, targetTypes));
            });

            FundanalyzerLogClient.logProcessEnd(Category.SCHEDULER, Process.ANALYSIS);
        } catch (Throwable t) {
            // Slack通知
            slackProxy.sendMessage("g.c.i.f.web.scheduler.notice.error", t);
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
            // アップデート処理が正常終了したらログ出力する
            CompletableFuture.allOf(
                    viewService.updateCorporateView(Target.annualSecuritiesReport()),
                    viewService.updateEdinetListView(Target.annualSecuritiesReport())
            ).thenRun(() -> FundanalyzerLogClient.logProcessEnd(Category.SCHEDULER, Process.UPDATE));
        } catch (Throwable t) {
            // Slack通知
            slackProxy.sendMessage("g.c.i.f.web.scheduler.notice.error", t);
            throw new FundanalyzerRuntimeException("スケジューラ処理中に想定外のエラーが発生しました。", t);
        }
    }
}
