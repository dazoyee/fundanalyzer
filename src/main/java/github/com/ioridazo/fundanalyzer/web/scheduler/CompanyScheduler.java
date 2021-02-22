package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Log4j2
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
        try {
            log.info("[スケジューラ]会社情報更新処理を開始します。");
            documentService.downloadCompanyInfo();
        } catch (Throwable t) {
            // Slack通知
            throw t;
        }
    }
}
