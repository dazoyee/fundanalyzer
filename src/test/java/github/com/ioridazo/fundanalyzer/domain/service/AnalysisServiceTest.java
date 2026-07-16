package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SourceOfStockPrice;
import github.com.ioridazo.fundanalyzer.domain.usecase.AnalyzeUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.NoticeUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.StockUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ValuationUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewEdinetUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Result;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerShortCircuitException;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.FinancialStatementInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AnalysisService のテスト")
class AnalysisServiceTest {

    private CompanyUseCase companyUseCase;
    private DocumentUseCase documentUseCase;
    private AnalyzeUseCase analyzeUseCase;
    private StockUseCase stockUseCase;
    private ValuationUseCase valuationUseCase;
    private ViewCorporateUseCase viewCorporateUseCase;
    private ViewEdinetUseCase viewEdinetUseCase;
    private NoticeUseCase noticeUseCase;
    private AnalysisService service;

    @BeforeEach
    void setUp() {
        companyUseCase = mock(CompanyUseCase.class);
        documentUseCase = mock(DocumentUseCase.class);
        analyzeUseCase = mock(AnalyzeUseCase.class);
        stockUseCase = mock(StockUseCase.class);
        valuationUseCase = mock(ValuationUseCase.class);
        viewCorporateUseCase = mock(ViewCorporateUseCase.class);
        viewEdinetUseCase = mock(ViewEdinetUseCase.class);
        noticeUseCase = mock(NoticeUseCase.class);
        service = new AnalysisService(
                companyUseCase, documentUseCase, analyzeUseCase, stockUseCase,
                valuationUseCase, viewCorporateUseCase, viewEdinetUseCase, noticeUseCase);
    }

    @Nested
    @DisplayName("executeAllMain メソッド")
    class ExecuteAllMain {

        @DisplayName("executeAllMain : 期間内の各日付に対して 9 種の処理を順に呼ぶ")
        @Test
        void invokesAllStepsPerDate() {
            final BetweenDateInputData input = BetweenDateInputData.of(
                    LocalDate.parse("2024-04-01"),
                    LocalDate.parse("2024-04-02"));

            service.executeAllMain(input);

            verify(documentUseCase, times(2)).allProcess(any(DateInputData.class));
            verify(documentUseCase, times(2)).removeDocument(any(DateInputData.class));
            verify(stockUseCase, times(2)).importStockPrice(any(DateInputData.class), eq(SourceOfStockPrice.KABUOJI3));
            verify(stockUseCase, times(2)).importStockPrice(any(DateInputData.class), eq(SourceOfStockPrice.MINKABU));
            verify(stockUseCase, times(2)).importStockPrice(any(DateInputData.class), eq(SourceOfStockPrice.NIKKEI));
            verify(analyzeUseCase, times(2)).analyze(any(DateInputData.class));
            verify(viewCorporateUseCase, times(2)).updateView(any(DateInputData.class));
            verify(viewEdinetUseCase, times(2)).updateView(any(DateInputData.class));
            verify(noticeUseCase, times(2)).noticeSlack(any(DateInputData.class));
        }
    }

    @Nested
    @DisplayName("executePartOfMain メソッド")
    class ExecutePartOfMain {

        @DisplayName("executePartOfMain : 株価取得と Slack 通知を除いた処理を呼ぶ")
        @Test
        void skipsStockAndNotice() {
            final BetweenDateInputData input = BetweenDateInputData.of(
                    LocalDate.parse("2024-04-01"),
                    LocalDate.parse("2024-04-01"));

            service.executePartOfMain(input);

            verify(documentUseCase, times(1)).allProcess(any(DateInputData.class));
            verify(documentUseCase, times(1)).removeDocument(any(DateInputData.class));
            verify(analyzeUseCase, times(1)).analyze(any(DateInputData.class));
            verify(viewCorporateUseCase, times(1)).updateView(any(DateInputData.class));
            verify(viewEdinetUseCase, times(1)).updateView(any(DateInputData.class));
            verify(stockUseCase, times(0)).importStockPrice(any(DateInputData.class), any());
            verify(stockUseCase, times(0)).importStockPrice(any(CodeInputData.class), any());
            verify(noticeUseCase, times(0)).noticeSlack(any());
        }
    }

    @Nested
    @DisplayName("executeByDate メソッド")
    class ExecuteByDate {

        @DisplayName("executeByDate : スクレイピングと分析を呼ぶ")
        @Test
        void scrapeAndAnalyze() {
            final DateInputData input = DateInputData.of(LocalDate.parse("2024-04-01"));
            service.executeByDate(input);
            verify(documentUseCase, times(1)).scrape(input);
            verify(analyzeUseCase, times(1)).analyze(input);
        }
    }

    @Nested
    @DisplayName("executeById メソッド")
    class ExecuteById {

        @DisplayName("executeById : 書類IDのスクレイピングと分析を呼ぶ")
        @Test
        void scrapeAndAnalyze() {
            final IdInputData input = IdInputData.of("doc-1");
            service.executeById(input);
            verify(documentUseCase, times(1)).scrape(input);
            verify(analyzeUseCase, times(1)).analyze(input);
        }
    }

    @Nested
    @DisplayName("registerFinancialStatementValue メソッド")
    class RegisterFinancialStatementValue {

        @DisplayName("registerFinancialStatementValue : DocumentUseCase に委譲し結果を返す")
        @Test
        void delegatesAndReturns() {
            final FinancialStatementInputData input = FinancialStatementInputData.of(
                    "edinet", "doc-1", "fs-1", "subj-1", 100L);
            when(documentUseCase.registerFinancialStatementValue(input)).thenReturn(Result.OK);

            assertEquals(Result.OK, service.registerFinancialStatementValue(input));
        }
    }

    @Nested
    @DisplayName("analyzeByDate メソッド")
    class AnalyzeByDate {

        @DisplayName("analyzeByDate : 期間補正・分析・ビュー更新を順に呼ぶ")
        @Test
        void recoveryAnalyzeAndUpdate() {
            final DateInputData input = DateInputData.of(LocalDate.parse("2024-04-01"));
            service.analyzeByDate(input);
            verify(documentUseCase, times(1)).updateDocumentPeriodIfNotExist(input);
            verify(analyzeUseCase, times(1)).analyze(input);
            verify(viewEdinetUseCase, times(1)).updateView(input);
        }
    }

    @Nested
    @DisplayName("analyzeById メソッド")
    class AnalyzeById {

        @DisplayName("analyzeById : AnalyzeUseCase.analyze に委譲する")
        @Test
        void delegates() {
            final IdInputData input = IdInputData.of("doc-1");
            service.analyzeById(input);
            verify(analyzeUseCase, times(1)).analyze(input);
        }
    }

    @Nested
    @DisplayName("importStock(BetweenDateInputData) メソッド")
    class ImportStockBetween {

        @DisplayName("importStock : 期間内の各日付で 4 ソースの株価取得を呼ぶ")
        @Test
        void invokesAllSources() {
            final BetweenDateInputData input = BetweenDateInputData.of(
                    LocalDate.parse("2024-04-01"),
                    LocalDate.parse("2024-04-02"));

            service.importStock(input);

            verify(stockUseCase, times(2)).importStockPrice(any(DateInputData.class), eq(SourceOfStockPrice.KABUOJI3));
            verify(stockUseCase, times(2)).importStockPrice(any(DateInputData.class), eq(SourceOfStockPrice.MINKABU));
            verify(stockUseCase, times(2)).importStockPrice(any(DateInputData.class), eq(SourceOfStockPrice.YAHOO_FINANCE));
            verify(stockUseCase, times(2)).importStockPrice(any(DateInputData.class), eq(SourceOfStockPrice.NIKKEI));
        }
    }

    @Nested
    @DisplayName("importStock(CodeInputData) メソッド")
    class ImportStockByCode {

        @DisplayName("importStock : 上場中の場合は 4 ソースで株価取得する")
        @Test
        void invokesWhenLived() {
            final CodeInputData input = CodeInputData.of("1234");
            when(companyUseCase.isLived(input)).thenReturn(true);

            service.importStock(input);

            verify(stockUseCase, times(1)).importStockPrice(input, SourceOfStockPrice.KABUOJI3);
            verify(stockUseCase, times(1)).importStockPrice(input, SourceOfStockPrice.MINKABU);
            verify(stockUseCase, times(1)).importStockPrice(input, SourceOfStockPrice.YAHOO_FINANCE);
            verify(stockUseCase, times(1)).importStockPrice(input, SourceOfStockPrice.NIKKEI);
            verify(companyUseCase, times(0)).updateRemovedCompany(input);
        }

        @DisplayName("importStock : 上場廃止済みの場合は除外フラグを更新する")
        @Test
        void removesWhenNotLived() {
            final CodeInputData input = CodeInputData.of("1234");
            when(companyUseCase.isLived(input)).thenReturn(false);

            service.importStock(input);

            verify(companyUseCase, times(1)).updateRemovedCompany(input);
            verify(stockUseCase, times(0)).importStockPrice(any(CodeInputData.class), any());
        }

        @DisplayName("importStock : ShortCircuit 例外発生時はログ出力で握りつぶし、後続の取得を続行する")
        @Test
        void swallowsShortCircuitException() {
            final CodeInputData input = CodeInputData.of("1234");
            when(companyUseCase.isLived(input)).thenReturn(true);
            doThrow(new FundanalyzerShortCircuitException("kabuoji3 down"))
                    .when(stockUseCase).importStockPrice(input, SourceOfStockPrice.KABUOJI3);

            assertDoesNotThrow(() -> service.importStock(input));
            verify(stockUseCase, times(1)).importStockPrice(input, SourceOfStockPrice.MINKABU);
            verify(stockUseCase, times(1)).importStockPrice(input, SourceOfStockPrice.YAHOO_FINANCE);
            verify(stockUseCase, times(1)).importStockPrice(input, SourceOfStockPrice.NIKKEI);
        }
    }

    @Nested
    @DisplayName("deleteStock メソッド")
    class DeleteStock {

        @DisplayName("deleteStock : StockUseCase.deleteStockPrice の戻り値を返す")
        @Test
        void delegates() {
            when(stockUseCase.deleteStockPrice()).thenReturn(42);
            assertEquals(42, service.deleteStock());
        }
    }

    @Nested
    @DisplayName("updateFavoriteCompany メソッド")
    class UpdateFavoriteCompany {

        @DisplayName("updateFavoriteCompany : CompanyUseCase に委譲し結果を返す")
        @Test
        void delegates() {
            final CodeInputData input = CodeInputData.of("1234");
            when(companyUseCase.updateFavoriteCompany(input)).thenReturn(true);
            assertTrue(service.updateFavoriteCompany(input));
        }
    }

    @Nested
    @DisplayName("updateStarCompany メソッド")
    class UpdateStarCompany {

        @DisplayName("updateStarCompany : CompanyUseCase に委譲し結果を返す")
        @Test
        void delegates() {
            final CodeInputData input = CodeInputData.of("1234");
            when(companyUseCase.updateStarCompany(input)).thenReturn(true);
            assertTrue(service.updateStarCompany(input));
        }
    }

    @Nested
    @DisplayName("evaluate メソッド")
    class Evaluate {

        @DisplayName("evaluate : ValuationUseCase.evaluate() の戻り値を返す")
        @Test
        void all() {
            when(valuationUseCase.evaluate()).thenReturn(7);
            assertEquals(7, service.evaluate());
        }

        @DisplayName("evaluate(code) : ValuationUseCase.evaluate(input) の戻り値を返す")
        @Test
        void byCode() {
            final CodeInputData input = CodeInputData.of("1234");
            when(valuationUseCase.evaluate(input)).thenReturn(false);
            assertFalse(service.evaluate(input));
        }
    }
}
