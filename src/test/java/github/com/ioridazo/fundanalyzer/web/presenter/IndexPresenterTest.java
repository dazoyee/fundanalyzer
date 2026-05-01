package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IndexPresenterのテスト")
class IndexPresenterTest {

    private ViewService viewService;
    private IndexPresenter presenter;

    @BeforeEach
    void setUp() {
        this.viewService = mock(ViewService.class);
        this.presenter = new IndexPresenter(viewService);
    }

    @Nested
    @DisplayName("corporateView メソッド")
    class CorporateView {

        @Test
        @DisplayName("target=null かつ message=null の場合 → デフォルトの企業一覧が表示される")
        void targetNullAndMessageNull_displaysDefaultCorporateView() {
            final Model model = mock(Model.class);
            final List<CorporateViewModel> expectedList = List.of();
            when(viewService.getCorporateView()).thenReturn(expectedList);

            final String result = presenter.corporateView(null, null, model);

            assertEquals("index", result);
            verify(model).addAttribute(eq("companies"), eq(expectedList));
            verify(model, never()).addAttribute(eq("target"), anyString());
            verify(viewService, times(1)).getCorporateView();
            verify(viewService, never()).getQuartCorporateView();
            verify(viewService, never()).getAllCorporateView();
            verify(viewService, never()).getFavoriteCorporateView();
        }

        @Test
        @DisplayName("target=quart の場合 → 四半期企業一覧が表示される")
        void targetQuart_displaysQuartCorporateView() {
            final Model model = mock(Model.class);
            final List<CorporateViewModel> expectedList = List.of();
            when(viewService.getQuartCorporateView()).thenReturn(expectedList);

            final String result = presenter.corporateView("quart", null, model);

            assertEquals("index", result);
            verify(model).addAttribute("target", "quart");
            verify(model).addAttribute("companies", expectedList);
            verify(viewService, times(1)).getQuartCorporateView();
            verify(viewService, never()).getCorporateView();
            verify(viewService, never()).getAllCorporateView();
            verify(viewService, never()).getFavoriteCorporateView();
        }

        @Test
        @DisplayName("target=all の場合 → 全件企業一覧が表示される")
        void targetAll_displaysAllCorporateView() {
            final Model model = mock(Model.class);
            final List<CorporateViewModel> expectedList = List.of();
            when(viewService.getAllCorporateView()).thenReturn(expectedList);

            final String result = presenter.corporateView("all", null, model);

            assertEquals("index", result);
            verify(model).addAttribute("target", "all");
            verify(model).addAttribute("companies", expectedList);
            verify(viewService, times(1)).getAllCorporateView();
            verify(viewService, never()).getCorporateView();
            verify(viewService, never()).getQuartCorporateView();
            verify(viewService, never()).getFavoriteCorporateView();
        }

        @Test
        @DisplayName("target=favorite の場合 → お気に入り企業一覧が表示される")
        void targetFavorite_displaysFavoriteCorporateView() {
            final Model model = mock(Model.class);
            final List<CorporateViewModel> expectedList = List.of();
            when(viewService.getFavoriteCorporateView()).thenReturn(expectedList);

            final String result = presenter.corporateView("favorite", null, model);

            assertEquals("index", result);
            verify(model).addAttribute("target", "favorite");
            verify(model).addAttribute("companies", expectedList);
            verify(viewService, times(1)).getFavoriteCorporateView();
            verify(viewService, never()).getCorporateView();
            verify(viewService, never()).getQuartCorporateView();
            verify(viewService, never()).getAllCorporateView();
        }

        @Test
        @DisplayName("messageが指定された場合 → URIデコードされてmodelに設定される")
        void messageProvided_addsDecodedMessageToModel() {
            final Model model = mock(Model.class);
            when(viewService.getCorporateView()).thenReturn(List.of());

            final String encodedMessage = "%E3%81%82";
            final String result = presenter.corporateView(null, encodedMessage, model);

            assertEquals("index", result);
            verify(model).addAttribute("message", "あ");
        }

        @Test
        @DisplayName("target=quart かつ message指定 → 両方modelに設定される")
        void targetQuartAndMessage_addsBothAttributes() {
            final Model model = mock(Model.class);
            final List<CorporateViewModel> expectedList = List.of();
            when(viewService.getQuartCorporateView()).thenReturn(expectedList);

            final String result = presenter.corporateView("quart", "hello", model);

            assertEquals("index", result);
            verify(model).addAttribute("message", "hello");
            verify(model).addAttribute("target", "quart");
            verify(model).addAttribute("companies", expectedList);
        }

        @Test
        @DisplayName("targetが未知の値の場合 → デフォルト分岐に入る")
        void targetUnknown_fallsBackToDefault() {
            final Model model = mock(Model.class);
            final List<CorporateViewModel> expectedList = List.of();
            when(viewService.getCorporateView()).thenReturn(expectedList);

            final String result = presenter.corporateView("unknown", null, model);

            assertEquals("index", result);
            verify(model).addAttribute("companies", expectedList);
            verify(viewService, times(1)).getCorporateView();
        }
    }

    @Nested
    @DisplayName("corporateViewV3 メソッド")
    class CorporateViewV3 {

        @Test
        @DisplayName("デフォルトパラメータの場合 → index-v2 view 名を返し table 属性が設定される")
        void defaultParams_returnsIndexV2() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            final String result = presenter.corporateViewV3(null, null, 0, 25, "code,asc", model);

            assertEquals("index-v2", result);
            verify(model).addAttribute("target", (String) null);
            verify(model).addAttribute("keyword", (String) null);
            verify(model).addAttribute("table", page);
            verify(model).addAttribute("sortParam", "code,asc");
        }

        @Test
        @DisplayName("ViewService.findCompanyTable に target / keyword / pageable が渡される")
        void parametersPassedToService() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("name"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3("quart", "abc", 2, 50, "name,desc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            final CompanyTableQuery query = captor.getValue();
            assertEquals("quart", query.target());
            assertEquals("abc", query.keyword());
            assertEquals(2, query.pageable().getPageNumber());
            assertEquals(50, query.pageable().getPageSize());
            assertEquals(Sort.by(Sort.Direction.DESC, "name"), query.pageable().getSort());
        }

        @Test
        @DisplayName("page が負数の場合 → 0 にクランプされる")
        void negativePage_clampedToZero() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, -5, 25, "code,asc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(0, captor.getValue().pageable().getPageNumber());
        }

        @Test
        @DisplayName("size が 100 を超える場合 → 100 にクランプされる")
        void sizeOver100_clampedTo100() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 100, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 1000, "code,asc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(100, captor.getValue().pageable().getPageSize());
        }

        @Test
        @DisplayName("size が 0 以下の場合 → 1 にクランプされる")
        void sizeZeroOrNegative_clampedToOne() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 1, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 0, "code,asc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(1, captor.getValue().pageable().getPageSize());
        }

        @Test
        @DisplayName("sort のフィールドがホワイトリスト外の場合 → code,asc にフォールバック")
        void sortFieldNotAllowed_fallsBackToCodeAsc() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 25, "secret,asc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(Sort.by(Sort.Direction.ASC, "code"), captor.getValue().pageable().getSort());
        }

        @Test
        @DisplayName("sort が null や空の場合 → code,asc にフォールバック")
        void sortNullOrBlank_fallsBackToCodeAsc() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 25, "", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(Sort.by(Sort.Direction.ASC, "code"), captor.getValue().pageable().getSort());
        }

        @Test
        @DisplayName("sort の direction が指定されていない場合 → asc になる")
        void sortDirectionMissing_defaultsToAsc() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("name"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 25, "name", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(Sort.by(Sort.Direction.ASC, "name"), captor.getValue().pageable().getSort());
        }
    }

    @Nested
    @DisplayName("corporateViewV3Table メソッド")
    class CorporateViewV3Table {

        @Test
        @DisplayName("呼び出された場合 → fragments/index-table :: table を返す")
        void returnsFragmentName() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            final String result = presenter.corporateViewV3Table(null, null, 0, 25, "code,asc", model);

            assertEquals("fragments/index-table :: table", result);
            verify(viewService, times(1)).findCompanyTable(any(CompanyTableQuery.class));
            verify(model).addAttribute("table", page);
        }

        @Test
        @DisplayName("v3 全画面と同じ共通属性が設定される")
        void sameCommonAttributesAsFullPage() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("submitDate"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3Table("favorite", "test", 1, 25, "submitDate,desc", model);

            verify(model).addAttribute("target", "favorite");
            verify(model).addAttribute("keyword", "test");
            verify(model).addAttribute("table", page);
            verify(model).addAttribute("sortParam", "submitDate,desc");
            assertNotNull(page);
        }
    }
}
