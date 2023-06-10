package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
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

        this.scheduler = Mockito.spy(new CompanyScheduler(companyUseCase, slackClient));
        scheduler.hourOfCompany = 12;
    }

    @Nested
    class companyScheduler {

        @DisplayName("companyScheduler : 会社一覧を更新する")
        @Test
        void companyScheduler_ok() {
            doReturn(LocalDateTime.of(2021, 5, 29, 12, 0)).when(scheduler).nowLocalDateTime();

            doNothing().when(companyUseCase).importCompanyInfo();
            assertDoesNotThrow(() -> scheduler.companyScheduler());
        }

        @DisplayName("companyScheduler : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void companyScheduler_throwable() {
            doReturn(LocalDateTime.of(2021, 5, 29, 12, 0)).when(scheduler).nowLocalDateTime();

            doThrow(FundanalyzerRuntimeException.class).when(companyUseCase).importCompanyInfo();
            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.companyScheduler());
            verify(slackClient, times(1)).sendMessage(any(), any(), any());
        }

        @DisplayName("companyScheduler : 処理時間外")
        @Test
        void companyScheduler_noTarget() {
            doReturn(LocalDateTime.of(2021, 5, 29, 15, 0)).when(scheduler).nowLocalDateTime();

            assertDoesNotThrow(() -> scheduler.companyScheduler());
            verify(companyUseCase, times(0)).importCompanyInfo();
        }

    }
}