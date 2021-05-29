package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
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

    private CompanyUseCase companyUseCase;
    private SlackClient slackClient;

    private CompanyScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.companyUseCase = Mockito.mock(CompanyUseCase.class);
        this.slackClient = Mockito.mock(SlackClient.class);

        this.scheduler = new CompanyScheduler(companyUseCase, slackClient);
    }

    @Nested
    class company {

        @DisplayName("company : 会社一覧を更新する")
        @Test
        void company_ok() {
            doNothing().when(companyUseCase).importCompanyInfo();
            assertDoesNotThrow(() -> scheduler.companyScheduler());
        }

        @DisplayName("company : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void company_throwable() {
            doThrow(FundanalyzerRuntimeException.class).when(companyUseCase).importCompanyInfo();
            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.companyScheduler());
            verify(slackClient, times(1)).sendMessage(any(), any());
        }
    }
}