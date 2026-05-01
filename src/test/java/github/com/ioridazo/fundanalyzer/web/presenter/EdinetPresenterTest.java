package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
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

@DisplayName("EdinetPresenterのテスト")
class EdinetPresenterTest {

    private ViewService viewService;
    private EdinetPresenter presenter;

    @BeforeEach
    void setUp() {
        this.viewService = mock(ViewService.class);
        this.presenter = new EdinetPresenter(viewService);
    }


    @Nested
    @DisplayName("edinetListViewV3 メソッド")
    class EdinetListViewV3 {

        @Test
        @DisplayName("デフォルトパラメータ → edinet-list-v2 view 名 + table 属性設定")
        void defaultParams_returnsEdinetListV2() {
            final Model model = mock(Model.class);
            final EdinetListTablePage page = new EdinetListTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by(Sort.Direction.DESC, "submitDate"));
            when(viewService.findEdinetListTable(any(EdinetListTableQuery.class))).thenReturn(page);

            final String result = presenter.edinetListViewV3(null, null, 0, 25, "submitDate,desc", model);

            assertEquals("edinet-list-v2", result);
            verify(model).addAttribute("table", page);
            verify(model).addAttribute("sortParam", "submitDate,desc");
        }

        @Test
        @DisplayName("target=all → EdinetListTableQuery に target=all が渡る")
        void targetAll_passedToQuery() {
            final Model model = mock(Model.class);
            final EdinetListTablePage page = new EdinetListTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("submitDate"));
            when(viewService.findEdinetListTable(any(EdinetListTableQuery.class))).thenReturn(page);

            presenter.edinetListViewV3("all", "2025-01", 1, 50, "countAll,asc", model);

            final ArgumentCaptor<EdinetListTableQuery> captor =
                    ArgumentCaptor.forClass(EdinetListTableQuery.class);
            verify(viewService).findEdinetListTable(captor.capture());
            assertEquals("all", captor.getValue().target());
            assertEquals("2025-01", captor.getValue().keyword());
            assertEquals(Sort.by(Sort.Direction.ASC, "countAll"), captor.getValue().pageable().getSort());
        }

        @Test
        @DisplayName("sort field がホワイトリスト外 → submitDate,desc にフォールバック")
        void sortFieldNotAllowed_fallsBackToDefault() {
            final Model model = mock(Model.class);
            final EdinetListTablePage page = new EdinetListTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by(Sort.Direction.DESC, "submitDate"));
            when(viewService.findEdinetListTable(any(EdinetListTableQuery.class))).thenReturn(page);

            presenter.edinetListViewV3(null, null, 0, 25, "secret,asc", model);

            final ArgumentCaptor<EdinetListTableQuery> captor =
                    ArgumentCaptor.forClass(EdinetListTableQuery.class);
            verify(viewService).findEdinetListTable(captor.capture());
            assertEquals(Sort.by(Sort.Direction.DESC, "submitDate"), captor.getValue().pageable().getSort());
        }
    }

    @Nested
    @DisplayName("edinetListViewV3Table メソッド")
    class EdinetListViewV3Table {

        @Test
        @DisplayName("呼び出された場合 → fragments/edinet-list-table :: table を返す")
        void returnsFragmentName() {
            final Model model = mock(Model.class);
            final EdinetListTablePage page = new EdinetListTablePage(
                    List.of(), 0, 0L, 0, 25, Sort.by("submitDate"));
            when(viewService.findEdinetListTable(any(EdinetListTableQuery.class))).thenReturn(page);

            final String result = presenter.edinetListViewV3Table(null, null, 0, 25, "submitDate,desc", model);

            assertEquals("fragments/edinet-list-table :: table", result);
        }
    }
}
