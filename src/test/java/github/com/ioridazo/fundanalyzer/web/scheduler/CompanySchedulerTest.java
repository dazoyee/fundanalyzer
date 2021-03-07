package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.proxy.slack.SlackProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CompanySchedulerTest {

    private DocumentService documentService;
    private SlackProxy slackProxy;

    private CompanyScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.documentService = Mockito.mock(DocumentService.class);
        this.slackProxy = Mockito.mock(SlackProxy.class);

        this.scheduler = new CompanyScheduler(documentService, slackProxy);
    }

    @Nested
    class company {

        @DisplayName("company : 会社一覧を更新する")
        @Test
        void company_ok() {
            doNothing().when(documentService).downloadCompanyInfo();
            assertDoesNotThrow(() -> scheduler.companyScheduler());
        }

        @DisplayName("company : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void company_throwable() {
            doThrow(FundanalyzerRuntimeException.class).when(documentService).downloadCompanyInfo();
            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.companyScheduler());
            verify(slackProxy, times(1)).sendMessage(any(), any());
        }
    }
}