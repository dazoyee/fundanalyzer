package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationViewModel;
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
}
