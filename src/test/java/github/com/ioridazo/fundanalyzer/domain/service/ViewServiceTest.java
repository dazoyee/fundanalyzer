package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewEdinetUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewValuationUseCase;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.presenter.Target;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ViewService のテスト")
class ViewServiceTest {

    private CompanyUseCase companyUseCase;
    private DocumentUseCase documentUseCase;
    private ViewCorporateUseCase viewCorporateUseCase;
    private ViewEdinetUseCase viewEdinetUseCase;
    private ViewValuationUseCase viewValuationUseCase;
    private ViewService service;

    @BeforeEach
    void setUp() {
        companyUseCase = mock(CompanyUseCase.class);
        documentUseCase = mock(DocumentUseCase.class);
        viewCorporateUseCase = mock(ViewCorporateUseCase.class);
        viewEdinetUseCase = mock(ViewEdinetUseCase.class);
        viewValuationUseCase = mock(ViewValuationUseCase.class);
        service = new ViewService(
                companyUseCase, documentUseCase,
                viewCorporateUseCase, viewEdinetUseCase, viewValuationUseCase);
    }

    @Nested
    @DisplayName("企業情報ビュー取得")
    class CorporateViews {

        @DisplayName("getCorporateView : viewCorporateUseCase.viewMain に委譲する")
        @Test
        void main() {
            when(viewCorporateUseCase.viewMain()).thenReturn(List.of());
            assertSame(List.of(), service.getCorporateView());
            verify(viewCorporateUseCase, times(1)).viewMain();
        }

        @DisplayName("getQuartCorporateView : viewCorporateUseCase.viewQuart に委譲する")
        @Test
        void quart() {
            when(viewCorporateUseCase.viewQuart()).thenReturn(List.of());
            service.getQuartCorporateView();
            verify(viewCorporateUseCase, times(1)).viewQuart();
        }

        @DisplayName("getAllCorporateView : viewCorporateUseCase.viewAll に委譲する")
        @Test
        void all() {
            when(viewCorporateUseCase.viewAll()).thenReturn(List.of());
            service.getAllCorporateView();
            verify(viewCorporateUseCase, times(1)).viewAll();
        }

        @DisplayName("getFavoriteCorporateView : viewCorporateUseCase.viewFavorite に委譲する")
        @Test
        void favorite() {
            when(viewCorporateUseCase.viewFavorite()).thenReturn(List.of());
            service.getFavoriteCorporateView();
            verify(viewCorporateUseCase, times(1)).viewFavorite();
        }
    }

    @Nested
    @DisplayName("EDINET ビュー取得")
    class EdinetViews {

        @DisplayName("getEdinetListView : viewEdinetUseCase.viewMain に委譲する")
        @Test
        void main() {
            when(viewEdinetUseCase.viewMain()).thenReturn(List.of());
            service.getEdinetListView();
            verify(viewEdinetUseCase, times(1)).viewMain();
        }

        @DisplayName("getAllEdinetListView : viewEdinetUseCase.viewAll に委譲する")
        @Test
        void all() {
            when(viewEdinetUseCase.viewAll()).thenReturn(List.of());
            service.getAllEdinetListView();
            verify(viewEdinetUseCase, times(1)).viewAll();
        }
    }

    @Nested
    @DisplayName("更新日時取得")
    class UpdateDate {

        @DisplayName("getUpdateDate : companyUseCase.getUpdateDate に委譲する")
        @Test
        void delegates() {
            when(companyUseCase.getUpdateDate()).thenReturn("2024-04-01");
            assertEquals("2024-04-01", service.getUpdateDate());
        }
    }

    @Nested
    @DisplayName("詳細ビュー取得")
    class DetailViews {

        @DisplayName("getCorporateDetailView(code) : viewCorporateUseCase に委譲する")
        @Test
        void corporateDetail() {
            final CodeInputData input = CodeInputData.of("1234");
            service.getCorporateDetailView(input);
            verify(viewCorporateUseCase, times(1)).viewCorporateDetail(input);
        }

        @DisplayName("getCorporateDetailView(code, target) : Target 引数版に委譲する")
        @Test
        void corporateDetailWithTarget() {
            final CodeInputData input = CodeInputData.of("1234");
            service.getCorporateDetailView(input, Target.MAIN);
            verify(viewCorporateUseCase, times(1)).viewCorporateDetail(input, Target.MAIN);
        }

        @DisplayName("getEdinetDetailView : viewEdinetUseCase.viewEdinetDetail に委譲する")
        @Test
        void edinetDetail() {
            final DateInputData input = DateInputData.of(LocalDate.parse("2024-04-01"));
            service.getEdinetDetailView(input);
            verify(viewEdinetUseCase, times(1)).viewEdinetDetail(input);
        }
    }

    @Nested
    @DisplayName("ビュー更新")
    class UpdateViews {

        @DisplayName("updateCorporateView : viewCorporateUseCase.updateView に委譲する")
        @Test
        void corporate() {
            service.updateCorporateView();
            verify(viewCorporateUseCase, times(1)).updateView();
        }

        @DisplayName("updateEdinetView : viewEdinetUseCase.updateView に委譲する")
        @Test
        void edinet() {
            service.updateEdinetView();
            verify(viewEdinetUseCase, times(1)).updateView();
        }

        @DisplayName("updateEdinetListView : 書類除外と EDINET ビュー更新を順に呼ぶ")
        @Test
        void edinetList() {
            final DateInputData input = DateInputData.of(LocalDate.parse("2024-04-01"));
            service.updateEdinetListView(input);
            verify(documentUseCase, times(1)).removeDocument(input);
            verify(viewEdinetUseCase, times(1)).updateView(input);
        }

        @DisplayName("updateValuationView() : viewValuationUseCase.updateView() に委譲する")
        @Test
        void valuationAll() {
            service.updateValuationView();
            verify(viewValuationUseCase, times(1)).updateView();
        }

        @DisplayName("updateValuationView(code) : viewValuationUseCase.updateView(input) に委譲する")
        @Test
        void valuationByCode() {
            final CodeInputData input = CodeInputData.of("1234");
            service.updateValuationView(input);
            verify(viewValuationUseCase, times(1)).updateView(input);
        }
    }

    @Nested
    @DisplayName("評価ビュー取得")
    class ValuationViews {

        @DisplayName("getValuationView : viewValuationUseCase.viewValuation に委譲する")
        @Test
        void main() {
            when(viewValuationUseCase.viewValuation()).thenReturn(List.of());
            service.getValuationView();
            verify(viewValuationUseCase, times(1)).viewValuation();
        }

        @DisplayName("getValuationView(code) : viewValuationUseCase.viewValuation(input) に委譲する")
        @Test
        void byCode() {
            final CodeInputData input = CodeInputData.of("1234");
            when(viewValuationUseCase.viewValuation(input)).thenReturn(List.of());
            service.getValuationView(input);
            verify(viewValuationUseCase, times(1)).viewValuation(input);
        }

        @DisplayName("getAllValuationView : viewValuationUseCase.viewAllValuation に委譲する")
        @Test
        void all() {
            when(viewValuationUseCase.viewAllValuation()).thenReturn(List.of());
            service.getAllValuationView();
            verify(viewValuationUseCase, times(1)).viewAllValuation();
        }

        @DisplayName("getFavoriteValuationView : viewValuationUseCase.viewFavoriteValuation に委譲する")
        @Test
        void favorite() {
            when(viewValuationUseCase.viewFavoriteValuation()).thenReturn(List.of());
            service.getFavoriteValuationView();
            verify(viewValuationUseCase, times(1)).viewFavoriteValuation();
        }

        @DisplayName("getIndustryValuationView : viewValuationUseCase.viewIndustryValuation に委譲する")
        @Test
        void industry() {
            when(viewValuationUseCase.viewIndustryValuation()).thenReturn(List.of());
            service.getIndustryValuationView();
            verify(viewValuationUseCase, times(1)).viewIndustryValuation();
        }
    }
}
