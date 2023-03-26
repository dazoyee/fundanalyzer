package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.jsoup.JsoupClient;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.MinkabuResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.NikkeiResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.StockPriceResultBean;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SourceOfStockPrice;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockInteractorTest {

    private CompanySpecification companySpecification;
    private DocumentSpecification documentSpecification;
    private StockSpecification stockSpecification;
    private JsoupClient jsoupClient;

    private StockInteractor stockInteractor;

    @BeforeEach
    void setUp() {
        companySpecification = Mockito.mock(CompanySpecification.class);
        documentSpecification = Mockito.mock(DocumentSpecification.class);
        stockSpecification = Mockito.mock(StockSpecification.class);
        jsoupClient = Mockito.mock(JsoupClient.class);

        stockInteractor = Mockito.spy(new StockInteractor(
                companySpecification,
                documentSpecification,
                stockSpecification,
                jsoupClient
        ));
        stockInteractor.isNikkei = true;
        stockInteractor.isKabuoji3 = true;
        stockInteractor.isMinkabu = true;
        stockInteractor.isYahooFinance = true;
        stockInteractor.daysToStoreStockPrice = 365;
    }

    @Nested
    class importStockPrice {

        Document document = new Document(
                null,
                null,
                null,
                "edinetCode",
                null,
                LocalDate.now(),
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
        Company company = new Company(
                "code",
                null,
                null,
                "edinetCode",
                null,
                null,
                null,
                null,
                null,
                false,
                true
        );

        @BeforeEach
        void setUp() {
            doReturn(LocalDate.parse("2021-06-06")).when(stockInteractor).nowLocalDate();
        }

        @DisplayName("importStockPrice : 株価を取得する（提出日）")
        @ParameterizedTest
        @EnumSource(SourceOfStockPrice.class)
        void date(SourceOfStockPrice place) {
            DateInputData inputData = DateInputData.of(LocalDate.parse("2021-05-15"));

            when(documentSpecification.inquiryTargetDocuments(inputData)).thenReturn(List.of(document));
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            doNothing().when(stockInteractor).importStockPrice(eq(CodeInputData.of("code")), any());

            assertDoesNotThrow(() -> stockInteractor.importStockPrice(inputData, place));
            verify(stockInteractor, times(1)).importStockPrice(CodeInputData.of("code"), place);
        }

        @DisplayName("importStockPrice : 株価を取得する（企業コード）")
        @ParameterizedTest
        @EnumSource(SourceOfStockPrice.class)
        void code(SourceOfStockPrice place) {
            CodeInputData inputData = CodeInputData.of("code0");

            when(jsoupClient.kabuoji3("code0")).thenReturn(List.of(
                    new StockPriceResultBean(
                            "2019-06-05",
                            "1000",
                            "1000",
                            "1000",
                            "1000",
                            "1000",
                            "1000"
                    ),
                    new StockPriceResultBean(
                            "2021-06-05",
                            "1000",
                            "1000",
                            "1000",
                            "1000",
                            "1000",
                            "1000"
                    )));
            when(jsoupClient.minkabuForStock("code0")).thenReturn(List.of(
                    new StockPriceResultBean(
                            "2021/06/05",
                            "1,000.0",
                            "1,000.0",
                            "1,000.0",
                            "1,000.0",
                            "1,000.0",
                            "1,000.0"
                    ),
                    new StockPriceResultBean(
                            "2019/06/05",
                            "1,000.0",
                            "1,000.0",
                            "1,000.0",
                            "1,000.0",
                            "1,000.0",
                            "1,000.0"
                    )));
            when(jsoupClient.yahooFinance("code0")).thenReturn(List.of(
                    new StockPriceResultBean(
                            "2019年06月05日",
                            "1,000",
                            "1,000",
                            "1,000",
                            "1,000",
                            "1,000",
                            "1,000"
                    ),
                    new StockPriceResultBean(
                            "2021年06月05日",
                            "1,000.5",
                            "1,000.5",
                            "1,000.5",
                            "1,000.5",
                            "1,000.5",
                            "1,000.5"
                    )));

            assertDoesNotThrow(() -> stockInteractor.importStockPrice(inputData, place));
            switch (place) {
                case NIKKEI -> verify(stockSpecification, times(1)).insert(eq("code0"), (NikkeiResultBean) any());
                case KABUOJI3, YAHOO_FINANCE ->
                        verify(stockSpecification, times(1)).insert(eq("code0"), any(), eq(place));
                case MINKABU -> {
                    verify(stockSpecification, times(1)).insert(eq("code0"), any(), eq(place));
                    verify(stockSpecification, times(1)).insert(eq("code0"), (MinkabuResultBean) any());
                }
            }
        }

        @DisplayName("importStockPrice : 株価を取得しない")
        @ParameterizedTest
        @EnumSource(SourceOfStockPrice.class)
        void code_nothing(SourceOfStockPrice place) {
            stockInteractor.isNikkei = false;
            stockInteractor.isKabuoji3 = false;
            stockInteractor.isMinkabu = false;
            stockInteractor.isYahooFinance = false;

            CodeInputData inputData = CodeInputData.of("code0");

            assertDoesNotThrow(() -> stockInteractor.importStockPrice(inputData, place));
            switch (place) {
                case NIKKEI -> verify(stockSpecification, times(0)).insert(eq("code0"), (NikkeiResultBean) any());
                case KABUOJI3, YAHOO_FINANCE ->
                        verify(stockSpecification, times(0)).insert(eq("code0"), any(), eq(place));
                case MINKABU -> verify(stockSpecification, times(0)).insert(eq("code0"), (MinkabuResultBean) any());
            }
        }
    }

    @Nested
    class deleteStockPrice {

        @DisplayName("deleteStockPrice : 削除対象の株価をカウントする")
        @Test
        void count() {
            when(stockSpecification.findTargetDateToDelete()).thenReturn(List.of(
                    LocalDate.parse("2020-06-06"), LocalDate.parse("2020-06-05"), LocalDate.parse("2020-05-06")
            ));
            when(stockSpecification.delete(any())).thenReturn(1);
            assertEquals(3, stockInteractor.deleteStockPrice());
        }
    }
}