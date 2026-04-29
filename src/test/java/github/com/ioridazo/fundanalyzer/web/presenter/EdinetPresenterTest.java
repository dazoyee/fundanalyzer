package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @DisplayName("edinetListView メソッド")
    class EdinetListView {

        @Test
        @DisplayName("target=null かつ message=null の場合 → デフォルトのEDINETリストが表示される")
        void targetNullAndMessageNull_displaysDefaultEdinetList() {
            final Model model = mock(Model.class);
            final List<EdinetListViewModel> expectedList = List.of();
            final String expectedUpdateDate = "2024-01-01";
            when(viewService.getEdinetListView()).thenReturn(expectedList);
            when(viewService.getUpdateDate()).thenReturn(expectedUpdateDate);

            final String result = presenter.edinetListView(null, null, model);

            assertEquals("edinet", result);
            verify(model, never()).addAttribute("message", "");
            verify(model).addAttribute("companyUpdated", expectedUpdateDate);
            verify(model).addAttribute("edinetList", expectedList);
            verify(viewService, times(1)).getEdinetListView();
            verify(viewService, never()).getAllEdinetListView();
        }

        @Test
        @DisplayName("target=all の場合 → 全件EDINETリストが表示される")
        void targetAll_displaysAllEdinetList() {
            final Model model = mock(Model.class);
            final List<EdinetListViewModel> expectedList = List.of();
            final String expectedUpdateDate = "2024-01-02";
            when(viewService.getAllEdinetListView()).thenReturn(expectedList);
            when(viewService.getUpdateDate()).thenReturn(expectedUpdateDate);

            final String result = presenter.edinetListView("all", null, model);

            assertEquals("edinet", result);
            verify(model).addAttribute("target", "all");
            verify(model).addAttribute("companyUpdated", expectedUpdateDate);
            verify(model).addAttribute("edinetList", expectedList);
            verify(viewService, times(1)).getAllEdinetListView();
            verify(viewService, never()).getEdinetListView();
        }

        @Test
        @DisplayName("messageが指定された場合 → URIデコードされてmodelに設定される")
        void messageProvided_addsDecodedMessageToModel() {
            final Model model = mock(Model.class);
            when(viewService.getEdinetListView()).thenReturn(List.of());
            when(viewService.getUpdateDate()).thenReturn("2024-01-03");

            final String encodedMessage = "%E3%81%82";
            final String result = presenter.edinetListView(null, encodedMessage, model);

            assertEquals("edinet", result);
            verify(model).addAttribute("message", "あ");
        }

        @Test
        @DisplayName("target=allかつmessage指定 → 両方modelに設定され全件表示される")
        void targetAllAndMessage_addsBothAndShowsAllList() {
            final Model model = mock(Model.class);
            final List<EdinetListViewModel> expectedList = List.of();
            when(viewService.getAllEdinetListView()).thenReturn(expectedList);
            when(viewService.getUpdateDate()).thenReturn("2024-01-04");

            final String result = presenter.edinetListView("all", "hello", model);

            assertEquals("edinet", result);
            verify(model).addAttribute("message", "hello");
            verify(model).addAttribute("target", "all");
            verify(model).addAttribute("edinetList", expectedList);
        }

        @Test
        @DisplayName("targetが未知の値の場合 → デフォルト分岐に入る")
        void targetUnknown_fallsBackToDefault() {
            final Model model = mock(Model.class);
            final List<EdinetListViewModel> expectedList = List.of();
            when(viewService.getEdinetListView()).thenReturn(expectedList);
            when(viewService.getUpdateDate()).thenReturn("2024-01-05");

            final String result = presenter.edinetListView("unknown", null, model);

            assertEquals("edinet", result);
            verify(model).addAttribute("edinetList", expectedList);
            verify(viewService, times(1)).getEdinetListView();
            verify(viewService, never()).getAllEdinetListView();
        }
    }
}
