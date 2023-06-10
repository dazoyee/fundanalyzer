package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AnalysisSchedulerTest {

    private AnalysisService analysisService;
    private ViewService viewService;
    private SlackClient slackClient;

    private AnalysisScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.analysisService = Mockito.mock(AnalysisService.class);
        this.viewService = Mockito.mock(ViewService.class);
        this.slackClient = Mockito.mock(SlackClient.class);

        this.scheduler = Mockito.spy(new AnalysisScheduler(
                analysisService,
                viewService,
                slackClient
        ));
        scheduler.hourOfAnalysis = 14;
        scheduler.hourOfUpdateView = 21;
        scheduler.hourOfRecoverDocumentPeriod = 1;
        scheduler.pastDaysForAnalysis = 30;
        scheduler.pastDaysForRecoveringDocumentPeriod = 30;
    }

    @Nested
    class analysisScheduler {

        @DisplayName("analysisScheduler : データベースにある最新提出日から昨日までの財務分析を実施する")
        @Test
        void analysisScheduler_ok() {
            doReturn(LocalDateTime.of(2021, 5, 29, 14, 0)).when(scheduler).nowLocalDateTime();
            doReturn(LocalDate.parse("2021-02-08")).when(scheduler).nowLocalDate();

            assertDoesNotThrow(() -> scheduler.analysisScheduler());
            verify(analysisService, times(1))
                    .executeAllMain(BetweenDateInputData.of(LocalDate.parse("2021-01-09"), LocalDate.parse("2021-02-08")));
        }

        @DisplayName("analysisScheduler : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void analysisScheduler_throwable() {
            doReturn(LocalDateTime.of(2021, 5, 29, 14, 0)).when(scheduler).nowLocalDateTime();

            doReturn(LocalDate.parse("2021-02-08")).when(scheduler).nowLocalDate();
            doThrow(new FundanalyzerRuntimeException()).when(analysisService).executeAllMain(any());

            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.analysisScheduler());
            verify(slackClient, times(1)).sendMessage(any(), any(), any());
        }

        @DisplayName("analysisScheduler : 処理時間外")
        @Test
        void analysisScheduler_noTarget() {
            doReturn(LocalDateTime.of(2021, 5, 29, 15, 0)).when(scheduler).nowLocalDateTime();

            assertDoesNotThrow(() -> scheduler.analysisScheduler());
            verify(analysisService, times(0)).executeAllMain(any());
        }
    }

    @Nested
    class updateViewScheduler {

        @DisplayName("updateViewScheduler : 表示をアップデートする")
        @Test
        void updateViewScheduler_ok() {
            doReturn(LocalDateTime.of(2021, 5, 29, 21, 0)).when(scheduler).nowLocalDateTime();

            assertDoesNotThrow(() -> scheduler.updateViewScheduler());
            verify(viewService, times(1)).updateCorporateView();
            verify(viewService, times(1)).updateEdinetView();
        }

        @DisplayName("updateViewScheduler : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void updateViewScheduler_throwable() {
            doReturn(LocalDateTime.of(2021, 5, 29, 21, 0)).when(scheduler).nowLocalDateTime();

            doThrow(new FundanalyzerRuntimeException()).when(viewService).updateCorporateView();
            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.updateViewScheduler());
            verify(slackClient, times(1)).sendMessage(any(), any(), any());
        }

        @DisplayName("updateViewScheduler : 処理時間外")
        @Test
        void updateViewScheduler_noTarget() {
            doReturn(LocalDateTime.of(2021, 5, 29, 15, 0)).when(scheduler).nowLocalDateTime();

            assertDoesNotThrow(() -> scheduler.updateViewScheduler());
            verify(viewService, times(0)).updateCorporateView();
            verify(viewService, times(0)).updateEdinetView();
        }
    }

    @Nested
    class recoverDocumentPeriodScheduler {

        @DisplayName("recoverDocumentPeriodScheduler : 一定期間の処理機関リカバリを実施する")
        @Test
        void recoverDocumentPeriodScheduler_ok() {
            doReturn(LocalDateTime.of(2021, 11, 3, 1, 0)).when(scheduler).nowLocalDateTime();
            doReturn(LocalDate.parse("2021-11-03")).when(scheduler).nowLocalDate();

            assertDoesNotThrow(() -> scheduler.recoverDocumentPeriodScheduler());
            verify(analysisService, times(0)).analyzeByDate(DateInputData.of(LocalDate.parse("2021-10-03")));
            verify(analysisService, times(1)).analyzeByDate(DateInputData.of(LocalDate.parse("2021-10-04")));
            verify(analysisService, times(1)).analyzeByDate(DateInputData.of(LocalDate.parse("2021-11-03")));
            verify(analysisService, times(0)).analyzeByDate(DateInputData.of(LocalDate.parse("2021-11-04")));
        }

        @DisplayName("recoverDocumentPeriodScheduler : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void recoverDocumentPeriodScheduler_throwable() {
            doReturn(LocalDateTime.of(2021, 11, 3, 1, 0)).when(scheduler).nowLocalDateTime();
            doReturn(LocalDate.parse("2021-11-03")).when(scheduler).nowLocalDate();
            doThrow(new FundanalyzerRuntimeException()).when(analysisService).analyzeByDate(any());

            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.recoverDocumentPeriodScheduler());
            verify(slackClient, times(1)).sendMessage(any(), any(), any());
        }

        @DisplayName("recoverDocumentPeriodScheduler : 処理時間外")
        @Test
        void recoverDocumentPeriodScheduler_noTarget() {
            doReturn(LocalDateTime.of(2021, 11, 3, 0, 0)).when(scheduler).nowLocalDateTime();

            assertDoesNotThrow(() -> scheduler.recoverDocumentPeriodScheduler());
            verify(analysisService, times(0)).analyzeByDate(any());
        }
    }
}