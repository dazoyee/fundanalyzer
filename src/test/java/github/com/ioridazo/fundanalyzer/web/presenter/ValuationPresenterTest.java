package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationTableQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ValuationPresenterのテスト")
class ValuationPresenterTest {

    private ViewService viewService;
    private ValuationPresenter presenter;

    @BeforeEach
    void setUp() {
        this.viewService = mock(ViewService.class);
        this.presenter = new ValuationPresenter(viewService);
    }


    @Nested
    @DisplayName("valuationViewV3 メソッド")
    class ValuationViewV3 {

        @Test
        @DisplayName("デフォルトパラメータ → valuation-v2 view 名・view=stock にフォールバック")
        void defaultParams_returnsValuationV2WithStockView() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "stock");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            final String result = presenter.valuationViewV3(null, null, null, 0, 25, null, null, model);

            assertEquals("valuation-v2", result);
            verify(model).addAttribute("view", "stock");
            verify(model).addAttribute("table", page);
            verify(model).addAttribute("sortParam", "code,asc");
        }

        @Test
        @DisplayName("view=submit → CompanyValuationTableQuery に view=submit が渡る")
        void viewSubmit_passedToService() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "submit");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            presenter.valuationViewV3(null, "submit", "abc", 1, 50, "corporateValue,desc", null, model);

            final ArgumentCaptor<CompanyValuationTableQuery> captor =
                    ArgumentCaptor.forClass(CompanyValuationTableQuery.class);
            verify(viewService).findCompanyValuationTable(captor.capture());
            final CompanyValuationTableQuery query = captor.getValue();
            assertEquals("submit", query.view());
            assertEquals("abc", query.keyword());
            assertEquals(1, query.pageable().getPageNumber());
            assertEquals(50, query.pageable().getPageSize());
            assertEquals(Sort.by(Sort.Direction.DESC, "corporateValue"), query.pageable().getSort());
        }

        @Test
        @DisplayName("view=invalid → stock にフォールバック")
        void viewInvalid_fallsBackToStock() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "stock");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            presenter.valuationViewV3(null, "secret", null, 0, 25, null, null, model);

            verify(model).addAttribute("view", "stock");
        }

        @Test
        @DisplayName("view=stock で sort=corporateValue（submit 用フィールド） → デフォルト code,asc にフォールバック")
        void sortFieldNotAllowedForView_fallsBackToDefault() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "stock");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            presenter.valuationViewV3(null, "stock", null, 0, 25, "corporateValue,asc", null, model);

            final ArgumentCaptor<CompanyValuationTableQuery> captor =
                    ArgumentCaptor.forClass(CompanyValuationTableQuery.class);
            verify(viewService).findCompanyValuationTable(captor.capture());
            assertEquals(Sort.by(Sort.Direction.ASC, "code"), captor.getValue().pageable().getSort());
            verify(model).addAttribute("sortParam", "code,asc");
        }

        @Test
        @DisplayName("page < 0 / size > 100 はクランプされる")
        void pageAndSizeClamping() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 100, Sort.by("code"), "stock");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            presenter.valuationViewV3(null, null, null, -5, 1000, null, null, model);

            final ArgumentCaptor<CompanyValuationTableQuery> captor =
                    ArgumentCaptor.forClass(CompanyValuationTableQuery.class);
            verify(viewService).findCompanyValuationTable(captor.capture());
            assertEquals(0, captor.getValue().pageable().getPageNumber());
            assertEquals(100, captor.getValue().pageable().getPageSize());
        }
    }

    @Nested
    @DisplayName("valuationViewV3Table メソッド")
    class ValuationViewV3Table {

        @Test
        @DisplayName("view=stock → fragments/valuation-table :: stock-table")
        void viewStock_returnsStockTableFragment() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "stock");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            final String result = presenter.valuationViewV3Table(null, "stock", null, 0, 25, null, null, model);

            assertEquals("fragments/valuation-table :: stock-table", result);
        }

        @Test
        @DisplayName("view=submit → submit-table fragment")
        void viewSubmit_returnsSubmitFragment() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "submit");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            final String result = presenter.valuationViewV3Table(null, "submit", null, 0, 25, null, null, model);

            assertEquals("fragments/valuation-table :: submit-table", result);
        }

        @Test
        @DisplayName("view=graham-index → graham-index-table fragment")
        void viewGrahamIndex_returnsGrahamFragment() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "graham-index");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            final String result = presenter.valuationViewV3Table(null, "graham-index", null, 0, 25, null, null, model);

            assertEquals("fragments/valuation-table :: graham-index-table", result);
        }

        @Test
        @DisplayName("view=graham-index mode=relative → query.mode=relative・model に mode=relative")
        void grahamRelative_passesRelativeMode() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "graham-index");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            presenter.valuationViewV3Table(null, "graham-index", null, 0, 25, null, "relative", model);

            final ArgumentCaptor<CompanyValuationTableQuery> captor =
                    ArgumentCaptor.forClass(CompanyValuationTableQuery.class);
            verify(viewService).findCompanyValuationTable(captor.capture());
            assertEquals("relative", captor.getValue().mode());
            verify(model).addAttribute("mode", "relative");
        }

        @Test
        @DisplayName("不正な mode は raw にフォールバック")
        void invalidMode_fallsBackToRaw() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "graham-index");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            presenter.valuationViewV3Table(null, "graham-index", null, 0, 25, null, "bogus", model);

            final ArgumentCaptor<CompanyValuationTableQuery> captor =
                    ArgumentCaptor.forClass(CompanyValuationTableQuery.class);
            verify(viewService).findCompanyValuationTable(captor.capture());
            assertEquals("raw", captor.getValue().mode());
        }

        @Test
        @DisplayName("graham-index 以外の view で mode=relative → raw に倒す")
        void relativeOnNonGrahamView_fallsBackToRaw() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "stock");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            presenter.valuationViewV3Table(null, "stock", null, 0, 25, null, "relative", model);

            final ArgumentCaptor<CompanyValuationTableQuery> captor =
                    ArgumentCaptor.forClass(CompanyValuationTableQuery.class);
            verify(viewService).findCompanyValuationTable(captor.capture());
            assertEquals("raw", captor.getValue().mode());
        }

        @Test
        @DisplayName("view=dividend-yield → dividend-yield-table fragment")
        void viewDividendYield_returnsDividendFragment() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "dividend-yield");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            final String result = presenter.valuationViewV3Table(null, "dividend-yield", null, 0, 25, null, null, model);

            assertEquals("fragments/valuation-table :: dividend-yield-table", result);
        }

    }
}
