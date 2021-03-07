package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.service.StockService;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.proxy.slack.SlackProxy;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockSchedulerTest {

    private StockService stockService;
    private SlackProxy slackProxy;
    private DocumentDao documentDao;

    private StockScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.stockService = Mockito.mock(StockService.class);
        this.slackProxy = Mockito.mock(SlackProxy.class);
        this.documentDao = Mockito.mock(DocumentDao.class);

        this.scheduler = Mockito.spy(new StockScheduler(stockService, slackProxy, documentDao));
    }

    @Nested
    class stockScheduler {

        @DisplayName("stockScheduler : 日が一致する提出日の会社の株価を更新する")
        @Test
        void stockScheduler_ok() {
            doReturn(LocalDate.parse("2021-02-06")).when(scheduler).nowLocalDate();
            when(documentDao.selectByDayOfSubmitDate("6")).thenReturn(List.of(
                    Document.builder()
                            .submitDate(LocalDate.parse("2021-01-06"))
                            .build(),
                    Document.builder()
                            .submitDate(LocalDate.parse("2021-02-06"))
                            .build(),
                    Document.builder()
                            .submitDate(LocalDate.parse("2021-02-06"))
                            .build()
            ));

            assertDoesNotThrow(() -> scheduler.stockScheduler());

            verify(stockService, times(1)).importStockPrice(LocalDate.parse("2021-01-06"));
            verify(stockService, times(1)).importStockPrice(LocalDate.parse("2021-02-06"));
            verify(slackProxy, times(1)).sendMessage("g.c.i.f.web.scheduler.notice.info", 2);
        }

        @DisplayName("stockScheduler : 想定外のエラーが発生したときはSlack通知する")
        @Test
        void stockScheduler_throwable() {
            doReturn(LocalDate.parse("2021-02-06")).when(scheduler).nowLocalDate();
            when(documentDao.selectByDayOfSubmitDate("6")).thenReturn(List.of(
                    Document.builder()
                            .submitDate(LocalDate.parse("2021-02-06"))
                            .build()
            ));
            when(stockService.importStockPrice((LocalDate) any())).thenThrow(FundanalyzerRuntimeException.class);

            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.stockScheduler());

            verify(slackProxy, times(1)).sendMessage(eq("g.c.i.f.web.scheduler.notice.error"), any());
        }
    }
}