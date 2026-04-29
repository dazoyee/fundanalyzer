package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail.EdinetDetailViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ui.Model;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EdinetDetailPresenterのテスト")
class EdinetDetailPresenterTest {

    private ViewService viewService;
    private EdinetDetailPresenter presenter;

    @BeforeEach
    void setUp() {
        this.viewService = mock(ViewService.class);
        this.presenter = new EdinetDetailPresenter(viewService);
    }

    @Nested
    @DisplayName("edinetListDetail メソッド")
    class EdinetListDetail {

        @Test
        @DisplayName("正常系：submitDateが妥当な日付文字列 → edinet-detailテンプレートを返す")
        void validSubmitDate_returnsEdinetDetailTemplate() {
            final Model model = mock(Model.class);
            final EdinetDetailViewModel expectedView = new EdinetDetailViewModel(null, java.util.List.of());
            when(viewService.getEdinetDetailView(any(DateInputData.class))).thenReturn(expectedView);

            final String result = presenter.edinetListDetail("2024-05-01", null, model);

            assertEquals("edinet-detail", result);
            verify(model).addAttribute(eq("edinetDetail"), eq(expectedView));
            verify(viewService, times(1)).getEdinetDetailView(any(DateInputData.class));
        }

        @Test
        @DisplayName("submitDateの値がDateInputDataに正しく変換されてViewServiceに渡される")
        void submitDate_isPassedAsDateInputData() {
            final Model model = mock(Model.class);
            final EdinetDetailViewModel expectedView = new EdinetDetailViewModel(null, java.util.List.of());
            final ArgumentCaptor<DateInputData> captor = ArgumentCaptor.forClass(DateInputData.class);
            when(viewService.getEdinetDetailView(captor.capture())).thenReturn(expectedView);

            presenter.edinetListDetail("2023-12-31", "msg", model);

            final DateInputData captured = captor.getValue();
            assertEquals(LocalDate.of(2023, 12, 31), captured.getDate());
        }

        @Test
        @DisplayName("messageが指定されていてもedinet-detailテンプレートを返す")
        void messageProvided_stillReturnsEdinetDetailTemplate() {
            final Model model = mock(Model.class);
            final EdinetDetailViewModel expectedView = new EdinetDetailViewModel(null, java.util.List.of());
            when(viewService.getEdinetDetailView(any(DateInputData.class))).thenReturn(expectedView);

            final String result = presenter.edinetListDetail("2024-06-15", "some message", model);

            assertEquals("edinet-detail", result);
            verify(model).addAttribute("edinetDetail", expectedView);
        }

        @Test
        @DisplayName("submitDateが不正な日付フォーマット → DateTimeParseExceptionをスローする")
        void invalidSubmitDate_throwsDateTimeParseException() {
            final Model model = mock(Model.class);

            assertThrows(java.time.format.DateTimeParseException.class,
                    () -> presenter.edinetListDetail("invalid-date", null, model));
        }
    }
}
