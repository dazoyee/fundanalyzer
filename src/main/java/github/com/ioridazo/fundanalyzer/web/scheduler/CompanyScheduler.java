package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
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
        documentService.downloadCompanyInfo();
    }
}
