package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
