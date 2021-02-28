package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import github.com.ioridazo.fundanalyzer.domain.service.StockService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.proxy.slack.SlackProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisSchedulerTest {

    private DocumentService documentService;
    private ViewService viewService;
    private SlackProxy slackProxy;
    private DocumentDao documentDao;

    private AnalysisScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.documentService = Mockito.mock(DocumentService.class);
        this.viewService = Mockito.mock(ViewService.class);
        this.slackProxy = Mockito.mock(SlackProxy.class);
        this.documentDao = Mockito.mock(DocumentDao.class);

        this.scheduler = Mockito.spy(new AnalysisScheduler(
                documentService,
                Mockito.mock(AnalysisService.class),
                Mockito.mock(StockService.class),
                viewService,
                slackProxy,
                documentDao
        ));
    }

    @Nested
    class analysisScheduler {

        @DisplayName("analysisScheduler : データベースにある最新提出日から昨日までの財務分析を実施する")
        @Test
        void analysisScheduler_ok() {
            when(documentDao.selectByDocumentTypeCode("120")).thenReturn(List.of(
                    Document.builder()
                            .submitDate(LocalDate.parse("2021-02-04"))
                            .build(),
                    Document.builder()
                            .submitDate(LocalDate.parse("2021-02-05"))
                            .build()
            ));
            doReturn(LocalDate.parse("2021-02-08")).when(scheduler).nowLocalDate();
            when(documentService.execute(any(), any())).thenReturn(new CompletableFuture<>());

            assertDoesNotThrow(() -> scheduler.analysisScheduler());

            verify(documentService, times(0)).execute("2021-02-05", "120");
            verify(documentService, times(1)).execute("2021-02-06", "120");
            verify(documentService, times(1)).execute("2021-02-07", "120");
            verify(documentService, times(0)).execute("2021-02-08", "120");
        }

        @DisplayName("analysisScheduler : データベースにある最新提出日が昨日の場合はなにも実施しない")
        @Test
        void analysisScheduler_nothing() {
            when(documentDao.selectByDocumentTypeCode("120")).thenReturn(List.of(
                    Document.builder()
                            .submitDate(LocalDate.parse("2021-02-07"))
                            .build()
            ));
            doReturn(LocalDate.parse("2021-02-08")).when(scheduler).nowLocalDate();
            when(documentService.execute(any(), any())).thenReturn(new CompletableFuture<>());

            assertDoesNotThrow(() -> scheduler.analysisScheduler());

            verify(documentService, times(0)).execute("2021-02-07", "120");
            verify(documentService, times(0)).execute("2021-02-08", "120");
        }

        @DisplayName("analysisScheduler : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void analysisScheduler_throwable() {
            when(documentDao.selectByDocumentTypeCode("120")).thenReturn(List.of(
                    Document.builder()
                            .submitDate(LocalDate.parse("2021-02-06"))
                            .build()
            ));
            doReturn(LocalDate.parse("2021-02-08")).when(scheduler).nowLocalDate();

            when(documentService.execute(any(), any())).thenThrow(FundanalyzerRuntimeException.class);

            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.analysisScheduler());

            verify(slackProxy, times(1)).sendMessage(any(), any());
        }
    }

    @Nested
    class updateViewScheduler {

        @DisplayName("updateViewScheduler : 表示をアップデートする")
        @Test
        void updateViewScheduler_ok() {
            when(viewService.updateCorporateView()).thenReturn(new CompletableFuture<>());
            when(viewService.updateEdinetListView("120")).thenReturn(new CompletableFuture<>());
            assertDoesNotThrow(() -> scheduler.updateViewScheduler());
        }

        @DisplayName("updateViewScheduler : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void updateViewScheduler_throwable() {
            when(viewService.updateCorporateView()).thenThrow(FundanalyzerRuntimeException.class);
            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.updateViewScheduler());
            verify(slackProxy, times(1)).sendMessage(any(), any());
        }
    }
}