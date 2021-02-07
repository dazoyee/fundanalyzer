package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockSchedulerTest {

    private StockService stockService;
    private DocumentDao documentDao;

    private StockScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.stockService = Mockito.mock(StockService.class);
        this.documentDao = Mockito.mock(DocumentDao.class);

        this.scheduler = Mockito.spy(new StockScheduler(stockService, documentDao));
    }

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
    }
}