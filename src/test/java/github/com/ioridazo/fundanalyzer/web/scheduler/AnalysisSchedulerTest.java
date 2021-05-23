package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
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
    }

    @Nested
    class analysisScheduler {

        @DisplayName("analysisScheduler : データベースにある最新提出日から昨日までの財務分析を実施する")
        @Test
        void analysisScheduler_ok() {
            var document = new Document(
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
            when(documentSpecification.documentList()).thenReturn(List.of(document));
            doReturn(LocalDate.parse("2021-02-08")).when(scheduler).nowLocalDate();

            assertDoesNotThrow(() -> scheduler.analysisScheduler());
            verify(analysisService, times(1))
                    .doMain(BetweenDateInputData.of(LocalDate.parse("2021-02-06"), LocalDate.parse("2021-02-08")));
        }

        @DisplayName("analysisScheduler : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void analysisScheduler_throwable() {
            when(documentSpecification.documentList()).thenReturn(List.of());
            doReturn(LocalDate.parse("2021-02-08")).when(scheduler).nowLocalDate();
            doThrow(new FundanalyzerRuntimeException()).when(analysisService).doMain(any());

            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.analysisScheduler());
            verify(slackClient, times(1)).sendMessage(any(), any());
        }
    }

    @Nested
    class updateViewScheduler {

        @DisplayName("updateViewScheduler : 表示をアップデートする")
        @Test
        void updateViewScheduler_ok() {
            assertDoesNotThrow(() -> scheduler.updateViewScheduler());
            verify(viewService, times(1)).updateView();
        }

        @DisplayName("updateViewScheduler : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void updateViewScheduler_throwable() {
            doThrow(new FundanalyzerRuntimeException()).when(viewService).updateView();
            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.updateViewScheduler());
            verify(slackClient, times(1)).sendMessage(any(), any());
        }
    }
}