package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile({"prod"})
public class CompanyScheduler {

    private final DocumentService documentService;

    public CompanyScheduler(
            final DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 会社情報更新スケジューラ
     */
    @Scheduled(cron = "${app.scheduler.cron.company}", zone = "Asia/Tokyo")
    public void company() {
        FundanalyzerLogClient.logProcessStart(Category.SCHEDULER, Process.COMPANY);

        try {
            documentService.downloadCompanyInfo();

            FundanalyzerLogClient.logProcessEnd(Category.SCHEDULER, Process.COMPANY);
        } catch (Throwable t) {
            // Slack通知
            throw t;
        }
    }
}
