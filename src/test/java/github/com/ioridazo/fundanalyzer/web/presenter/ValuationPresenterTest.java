package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationViewModel;
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
    @DisplayName("valuationView メソッド")
    class ValuationView {

        @Test
        @DisplayName("target=null かつ message=null の場合 → デフォルトのバリュエーションリストが表示される")
        void targetNullAndMessageNull_displaysDefaultValuationList() {
            final Model model = mock(Model.class);
            final List<CompanyValuationViewModel> expectedList = List.of();
            when(viewService.getValuationView()).thenReturn(expectedList);

            final String result = presenter.valuationView(null, null, model);

            assertEquals("valuation", result);
            verify(model, never()).addAttribute("message", "");
            verify(model).addAttribute("valuations", expectedList);
            verify(viewService, times(1)).getValuationView();
            verify(viewService, never()).getAllValuationView();
            verify(viewService, never()).getFavoriteValuationView();
            verify(viewService, never()).getIndustryValuationView();
        }

        @Test
        @DisplayName("target=all の場合 → 全件バリュエーションリストが表示される")
        void targetAll_displaysAllValuationList() {
            final Model model = mock(Model.class);
            final List<CompanyValuationViewModel> expectedList = List.of();
            when(viewService.getAllValuationView()).thenReturn(expectedList);

            final String result = presenter.valuationView("all", null, model);

            assertEquals("valuation", result);
            verify(model).addAttribute("target", "all");
            verify(model).addAttribute("valuations", expectedList);
            verify(viewService, times(1)).getAllValuationView();
            verify(viewService, never()).getValuationView();
        }

        @Test
        @DisplayName("target=favorite の場合 → お気に入りバリュエーションリストが表示される")
        void targetFavorite_displaysFavoriteValuationList() {
            final Model model = mock(Model.class);
            final List<CompanyValuationViewModel> expectedList = List.of();
            when(viewService.getFavoriteValuationView()).thenReturn(expectedList);

            final String result = presenter.valuationView("favorite", null, model);

            assertEquals("valuation", result);
            verify(model).addAttribute("target", "favorite");
            verify(model).addAttribute("valuations", expectedList);
            verify(viewService, times(1)).getFavoriteValuationView();
            verify(viewService, never()).getValuationView();
        }

        @Test
        @DisplayName("target=industry の場合 → 業種別バリュエーションリストが表示される")
        void targetIndustry_displaysIndustryValuationList() {
            final Model model = mock(Model.class);
            final List<IndustryValuationViewModel> expectedList = List.of();
            when(viewService.getIndustryValuationView()).thenReturn(expectedList);

            final String result = presenter.valuationView("industry", null, model);

            assertEquals("valuation", result);
            verify(model).addAttribute("target", "industry");
            verify(model).addAttribute("valuations", expectedList);
            verify(viewService, times(1)).getIndustryValuationView();
            verify(viewService, never()).getValuationView();
        }

        @Test
        @DisplayName("messageが指定された場合 → URIデコードされてmodelに設定される")
        void messageProvided_addsDecodedMessageToModel() {
            final Model model = mock(Model.class);
            when(viewService.getValuationView()).thenReturn(List.of());

            final String encodedMessage = "%E3%81%82";
            final String result = presenter.valuationView(null, encodedMessage, model);

            assertEquals("valuation", result);
            verify(model).addAttribute("message", "あ");
        }

        @Test
        @DisplayName("target=allかつmessage指定 → 両方modelに設定され全件表示される")
        void targetAllAndMessage_addsBothAndShowsAllList() {
            final Model model = mock(Model.class);
            final List<CompanyValuationViewModel> expectedList = List.of();
            when(viewService.getAllValuationView()).thenReturn(expectedList);

            final String result = presenter.valuationView("all", "hello", model);

            assertEquals("valuation", result);
            verify(model).addAttribute("message", "hello");
            verify(model).addAttribute("target", "all");
            verify(model).addAttribute("valuations", expectedList);
        }

        @Test
        @DisplayName("targetが未知の値の場合 → デフォルト分岐に入る")
        void targetUnknown_fallsBackToDefault() {
            final Model model = mock(Model.class);
            final List<CompanyValuationViewModel> expectedList = List.of();
            when(viewService.getValuationView()).thenReturn(expectedList);

            final String result = presenter.valuationView("unknown", null, model);

            assertEquals("valuation", result);
            verify(model).addAttribute("valuations", expectedList);
            verify(viewService, times(1)).getValuationView();
            verify(viewService, never()).getAllValuationView();
            verify(viewService, never()).getFavoriteValuationView();
            verify(viewService, never()).getIndustryValuationView();
        }
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

            final String result = presenter.valuationViewV3(null, null, null, 0, 25, null, model);

            assertEquals("valuation-v2", result);
            verify(model).addAttribute("view", "stock");
            verify(model).addAttribute("table", page);
            verify(model).addAttribute("sortParam", "code,asc");
        }

        @Test
        @DisplayName("target=industry → view=industry を強制")
        void targetIndustry_forcesIndustryView() {
            final Model model = mock(Model.class);
            final IndustryValuationTablePage page = new IndustryValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("name"));
            when(viewService.findIndustryValuationTable(any(IndustryValuationTableQuery.class))).thenReturn(page);

            presenter.valuationViewV3("industry", "stock", null, 0, 25, null, model);

            verify(model).addAttribute("view", "industry");
            verify(viewService, times(1)).findIndustryValuationTable(any(IndustryValuationTableQuery.class));
            verify(viewService, never()).findCompanyValuationTable(any(CompanyValuationTableQuery.class));
        }

        @Test
        @DisplayName("view=submit → CompanyValuationTableQuery に view=submit が渡る")
        void viewSubmit_passedToService() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "submit");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            presenter.valuationViewV3(null, "submit", "abc", 1, 50, "corporateValue,desc", model);

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

            presenter.valuationViewV3(null, "secret", null, 0, 25, null, model);

            verify(model).addAttribute("view", "stock");
        }

        @Test
        @DisplayName("view=stock で sort=corporateValue（submit 用フィールド） → デフォルト code,asc にフォールバック")
        void sortFieldNotAllowedForView_fallsBackToDefault() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "stock");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            presenter.valuationViewV3(null, "stock", null, 0, 25, "corporateValue,asc", model);

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

            presenter.valuationViewV3(null, null, null, -5, 1000, null, model);

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

            final String result = presenter.valuationViewV3Table(null, "stock", null, 0, 25, null, model);

            assertEquals("fragments/valuation-table :: stock-table", result);
        }

        @Test
        @DisplayName("view=submit → submit-table fragment")
        void viewSubmit_returnsSubmitFragment() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "submit");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            final String result = presenter.valuationViewV3Table(null, "submit", null, 0, 25, null, model);

            assertEquals("fragments/valuation-table :: submit-table", result);
        }

        @Test
        @DisplayName("view=graham-index → graham-index-table fragment")
        void viewGrahamIndex_returnsGrahamFragment() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "graham-index");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            final String result = presenter.valuationViewV3Table(null, "graham-index", null, 0, 25, null, model);

            assertEquals("fragments/valuation-table :: graham-index-table", result);
        }

        @Test
        @DisplayName("view=dividend-yield → dividend-yield-table fragment")
        void viewDividendYield_returnsDividendFragment() {
            final Model model = mock(Model.class);
            final CompanyValuationTablePage page = new CompanyValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("code"), "dividend-yield");
            when(viewService.findCompanyValuationTable(any(CompanyValuationTableQuery.class))).thenReturn(page);

            final String result = presenter.valuationViewV3Table(null, "dividend-yield", null, 0, 25, null, model);

            assertEquals("fragments/valuation-table :: dividend-yield-table", result);
        }

        @Test
        @DisplayName("target=industry → industry-table fragment")
        void targetIndustry_returnsIndustryFragment() {
            final Model model = mock(Model.class);
            final IndustryValuationTablePage page = new IndustryValuationTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("name"));
            when(viewService.findIndustryValuationTable(any(IndustryValuationTableQuery.class))).thenReturn(page);

            final String result = presenter.valuationViewV3Table("industry", null, null, 0, 25, null, model);

            assertEquals("fragments/valuation-table :: industry-table", result);
        }
    }
}
