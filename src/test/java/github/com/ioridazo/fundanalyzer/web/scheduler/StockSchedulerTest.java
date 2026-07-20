package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.usecase.SystemEventUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ValuationUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.ValuationCatchUpResult;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockSchedulerTest {

    private AnalysisService analysisService;
    private StockSpecification stockSpecification;
    private ValuationUseCase valuationUseCase;
    private SystemEventUseCase systemEventUseCase;

    private StockScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.analysisService = Mockito.mock(AnalysisService.class);
        this.stockSpecification = Mockito.mock(StockSpecification.class);
        this.valuationUseCase = Mockito.mock(ValuationUseCase.class);
        this.systemEventUseCase = Mockito.mock(SystemEventUseCase.class);

        this.scheduler = Mockito.spy(new StockScheduler(analysisService, stockSpecification, valuationUseCase, systemEventUseCase));
        scheduler.hourOfStock = List.of(13);
        scheduler.hourOfEvaluate = 13;
    }

    @Nested
    class stockScheduler {

        @DisplayName("stockScheduler : 個社失敗は握りつぶし SystemEvent は記録しない")
        @Test
        void insertStockScheduler_throwable() {
            doReturn(LocalDateTime.of(2021, 5, 29, 13, 0)).when(scheduler).nowLocalDateTime();
            when(stockSpecification.findTargetCodeForStockScheduler()).thenReturn(List.of("code"));
            doThrow(new FundanalyzerRuntimeException()).when(analysisService).importStock((CodeInputData) any());

            assertDoesNotThrow(() -> scheduler.stockScheduler());
            verify(systemEventUseCase, times(0)).record(any(), any(), any());
        }

        @DisplayName("stockScheduler : 個社失敗でも後続企業を継続する")
        @Test
        void continuesPerCompanyFailure() {
            doReturn(LocalDateTime.of(2021, 5, 29, 13, 0)).when(scheduler).nowLocalDateTime();
            when(stockSpecification.findTargetCodeForStockScheduler()).thenReturn(List.of("1111", "2222"));
            doThrow(new FundanalyzerRuntimeException()).when(analysisService).importStock(CodeInputData.of("1111"));

            assertDoesNotThrow(() -> scheduler.stockScheduler());

            verify(analysisService, times(1)).importStock(CodeInputData.of("1111"));
            verify(analysisService, times(1)).importStock(CodeInputData.of("2222"));
            verify(systemEventUseCase, times(0)).record(any(), any(), any());
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

        @DisplayName("evaluateScheduler : 想定外のエラーが発生したときはSystemEventを記録する")
        @Test
        void throwable() {
            doReturn(LocalDateTime.of(2021, 5, 29, 13, 0)).when(scheduler).nowLocalDateTime();
            doThrow(new FundanalyzerRuntimeException()).when(analysisService).evaluate();

            assertThrows(FundanalyzerRuntimeException.class, () -> scheduler.evaluateScheduler());
            verify(systemEventUseCase, times(1))
                    .record(SystemEventType.ERROR, "StockScheduler",
                            "株価評価: github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException");
        }

        @DisplayName("evaluateScheduler : 株価を評価する")
        @Test
        void ok() {
            doReturn(LocalDateTime.of(2021, 5, 29, 13, 0)).when(scheduler).nowLocalDateTime();
            when(analysisService.evaluate()).thenReturn(1);
            when(valuationUseCase.catchUp()).thenReturn(new ValuationCatchUpResult(2, 3, 0));

            assertDoesNotThrow(() -> scheduler.evaluateScheduler());
            verify(analysisService, times(1)).evaluate();
            verify(valuationUseCase, times(1)).catchUp();
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
