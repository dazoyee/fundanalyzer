package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisSchedulerTest {

    private AnalysisService analysisService;
    private ViewService viewService;
    private DocumentSpecification documentSpecification;
    private SlackClient slackClient;

    private AnalysisScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.analysisService = Mockito.mock(AnalysisService.class);
        this.viewService = Mockito.mock(ViewService.class);
        this.documentSpecification = Mockito.mock(DocumentSpecification.class);
        this.slackClient = Mockito.mock(SlackClient.class);

        this.scheduler = Mockito.spy(new AnalysisScheduler(
                analysisService,
                viewService,
                documentSpecification,
                slackClient
        ));
        scheduler.hourOfAnalysis = 14;
        scheduler.hourOfUpdateView = 21;
    }

    @Nested
    class analysisScheduler {

        @DisplayName("analysisScheduler : データベースにある最新提出日から昨日までの財務分析を実施する")
        @Test
        void analysisScheduler_ok() {
            doReturn(LocalDateTime.of(2021, 5, 29, 14, 0)).when(scheduler).nowLocalDateTime();

            var document = new Document(
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDate.parse("2021-02-05"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false
            );
            when(documentSpecification.submitDateList()).thenReturn(List.of(LocalDate.parse("2021-02-05")));
            doReturn(LocalDate.parse("2021-02-08")).when(scheduler).nowLocalDate();

            assertDoesNotThrow(() -> scheduler.analysisScheduler());
            verify(analysisService, times(1))
                    .executeAllMain(BetweenDateInputData.of(LocalDate.parse("2021-02-06"), LocalDate.parse("2021-02-08")));
        }

        @DisplayName("analysisScheduler : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void analysisScheduler_throwable() {
            doReturn(LocalDateTime.of(2021, 5, 29, 14, 0)).when(scheduler).nowLocalDateTime();

            when(documentSpecification.submitDateList()).thenReturn(List.of());
            doReturn(LocalDate.parse("2021-02-08")).when(scheduler).nowLocalDate();
            doThrow(new FundanalyzerRuntimeException()).when(analysisService).executeAllMain(any());

            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.analysisScheduler());
            verify(slackClient, times(1)).sendMessage(any(), any());
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
            verify(slackClient, times(1)).sendMessage(any(), any());
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
}