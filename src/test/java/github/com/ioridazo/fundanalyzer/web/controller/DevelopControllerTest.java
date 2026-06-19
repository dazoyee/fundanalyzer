package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SourceOfStockPrice;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.usecase.AnalyzeUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.StockUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewEdinetUseCase;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DevelopControllerのテスト")
class DevelopControllerTest {

    private ViewService viewService;
    private CompanyUseCase companyUseCase;
    private DocumentUseCase documentUseCase;
    private AnalyzeUseCase analyzeUseCase;
    private StockUseCase stockUseCase;
    private ViewCorporateUseCase viewCorporateUseCase;
    private ViewEdinetUseCase viewEdinetUseCase;

    private DevelopController controller;

    @BeforeEach
    void setUp() {
        this.viewService = mock(ViewService.class);
        this.companyUseCase = mock(CompanyUseCase.class);
        this.documentUseCase = mock(DocumentUseCase.class);
        this.analyzeUseCase = mock(AnalyzeUseCase.class);
        this.stockUseCase = mock(StockUseCase.class);
        this.viewCorporateUseCase = mock(ViewCorporateUseCase.class);
        this.viewEdinetUseCase = mock(ViewEdinetUseCase.class);

        this.controller = new DevelopController(
                viewService,
                companyUseCase,
                documentUseCase,
                analyzeUseCase,
                stockUseCase,
                viewCorporateUseCase,
                viewEdinetUseCase
        );
    }

    @Nested
    @DisplayName("devEdinetList メソッド")
    class DevEdinetList {

        @Test
        @DisplayName("呼び出し時 → companyUpdated と edinetList を model に設定し edinet テンプレートを返す")
        void invoked_setsAttributesAndReturnsEdinetView() {
            final Model model = mock(Model.class);
            final List<EdinetListViewModel> expectedList = List.of();
            final String expectedUpdateDate = "2024-05-01";
            when(companyUseCase.getUpdateDate()).thenReturn(expectedUpdateDate);
            when(viewService.getEdinetListView()).thenReturn(expectedList);

            final String result = controller.devEdinetList(model);

            assertEquals("edinet", result);
            verify(model).addAttribute("companyUpdated", expectedUpdateDate);
            verify(model).addAttribute("edinetList", expectedList);
            verify(companyUseCase, times(1)).getUpdateDate();
            verify(viewService, times(1)).getEdinetListView();
        }
    }

    @Nested
    @DisplayName("devCompany メソッド")
    class DevCompany {

        @Test
        @DisplayName("呼び出し時 → 企業情報を保存し companies を model に設定し index テンプレートを返す")
        void invoked_savesCompaniesAndReturnsIndexView() {
            final Model model = mock(Model.class);
            final List<CorporateViewModel> expectedList = List.of();
            when(viewService.getCorporateView()).thenReturn(expectedList);

            final String result = controller.devCompany(model);

            assertEquals("index", result);
            verify(companyUseCase, times(1)).saveCompanyInfo();
            verify(model).addAttribute("companies", expectedList);
            verify(viewService, times(1)).getCorporateView();
        }
    }

    @Nested
    @DisplayName("devDoMain メソッド")
    class DevDoMain {

        @Test
        @DisplayName("日付が指定された場合 → 一連の処理を実行しリダイレクト先 URI を返す")
        void dateProvided_runsAllProcessesAndRedirects() {
            final Model model = mock(Model.class);
            final String dateString = "2024-04-15";
            final LocalDate date = LocalDate.parse(dateString);
            final DateInputData expectedInput = DateInputData.of(date);
            final List<CorporateViewModel> expectedList = List.of();
            when(viewService.getCorporateView()).thenReturn(expectedList);

            final String result = controller.devDoMain(dateString, model);

            assertEquals("redirect:/v3/index", result);
            verify(companyUseCase, times(1)).saveCompanyInfo();
            verify(documentUseCase, times(1)).allProcess(expectedInput);
            verify(documentUseCase, times(1)).removeDocument(expectedInput);
            verify(stockUseCase, times(1)).importStockPrice(expectedInput, SourceOfStockPrice.MINKABU);
            verify(stockUseCase, times(1)).importStockPrice(expectedInput, SourceOfStockPrice.NIKKEI);
            verify(analyzeUseCase, times(1)).analyze(expectedInput);
            verify(viewCorporateUseCase, times(1)).updateView(expectedInput);
            verify(viewEdinetUseCase, times(1)).updateView(expectedInput);
            verify(model).addAttribute("companies", expectedList);
        }
    }
}
