package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockSchedulerTest {

    private AnalysisService analysisService;
    private StockSpecification stockSpecification;
    private SlackClient slackClient;

    private StockScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.analysisService = Mockito.mock(AnalysisService.class);
        this.stockSpecification = Mockito.mock(StockSpecification.class);
        this.slackClient = Mockito.mock(SlackClient.class);

        this.scheduler = Mockito.spy(new StockScheduler(analysisService, stockSpecification, slackClient));
        scheduler.hourOfStock = List.of(13);
        scheduler.hourOfEvaluate = 13;
        scheduler.insertStockEnabled = true;
        scheduler.deleteStockEnabled = true;
        scheduler.evaluateEnabled = true;
    }

    @Nested
    class stockScheduler {

        @DisplayName("stockScheduler : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void insertStockScheduler_throwable() {
            doReturn(LocalDateTime.of(2021, 5, 29, 13, 0)).when(scheduler).nowLocalDateTime();
            when(stockSpecification.findTargetCodeForStockScheduler()).thenReturn(List.of("code"));
            doThrow(new FundanalyzerRuntimeException()).when(analysisService).importStock((CodeInputData) any());

            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.stockScheduler());
            verify(slackClient, times(1)).sendMessage(eq("g.c.i.f.web.scheduler.notice.error"), any(), any());
        }

        @DisplayName("stockScheduler : 株価を削除する")
//        @Test
        void deleteStockScheduler_ok() {
            doReturn(LocalDateTime.of(2021, 5, 29, 13, 0)).when(scheduler).nowLocalDateTime();
            when(analysisService.deleteStock()).thenReturn(1);

            assertDoesNotThrow(() -> scheduler.stockScheduler());
            verify(analysisService, times(1)).deleteStock();
            verify(slackClient, times(1)).sendMessage("github.com.ioridazo.fundanalyzer.web.scheduler.StockScheduler.delete", 1);
        }

        @DisplayName("stockScheduler : 想定外のエラーが発生したときはSlack通知する")
//        @Test
        void deleteStockScheduler_throwable() {
            doReturn(LocalDateTime.of(2021, 5, 29, 13, 0)).when(scheduler).nowLocalDateTime();
            doThrow(new FundanalyzerRuntimeException()).when(analysisService).deleteStock();

            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.stockScheduler());
            verify(slackClient, times(1)).sendMessage(eq("g.c.i.f.web.scheduler.notice.error"), any());
        }

        @DisplayName("stockScheduler : 処理時間外")
        @Test
        void stockScheduler_noTarget() {
            doReturn(LocalDateTime.of(2021, 5, 29, 15, 0)).when(scheduler).nowLocalDateTime();

            assertDoesNotThrow(() -> scheduler.stockScheduler());
            verify(analysisService, times(0)).deleteStock();
        }
    }

    @Nested
    class evaluateScheduler {

        @DisplayName("evaluateScheduler : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void throwable() {
            doReturn(LocalDateTime.of(2021, 5, 29, 13, 0)).when(scheduler).nowLocalDateTime();
            doThrow(new FundanalyzerRuntimeException()).when(analysisService).evaluate();

            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.evaluateScheduler());
            verify(slackClient, times(1)).sendMessage(eq("g.c.i.f.web.scheduler.notice.error"), any(), any());
        }

        @DisplayName("evaluateScheduler : 株価を評価する")
        @Test
        void ok() {
            doReturn(LocalDateTime.of(2021, 5, 29, 13, 0)).when(scheduler).nowLocalDateTime();
            when(analysisService.evaluate()).thenReturn(1);

            assertDoesNotThrow(() -> scheduler.evaluateScheduler());
            verify(analysisService, times(1)).evaluate();
            verify(slackClient, times(1)).sendMessage("github.com.ioridazo.fundanalyzer.web.scheduler.StockScheduler.evaluate", 1);
        }

        @DisplayName("evaluateScheduler : 処理時間外")
        @Test
        void evaluateScheduler_noTarget() {
            doReturn(LocalDateTime.of(2021, 5, 29, 15, 0)).when(scheduler).nowLocalDateTime();

            assertDoesNotThrow(() -> scheduler.evaluateScheduler());
            verify(analysisService, times(0)).evaluate();
        }
    }
}