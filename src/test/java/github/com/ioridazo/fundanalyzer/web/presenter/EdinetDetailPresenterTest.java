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
    @DisplayName("edinetListDetailV3 メソッド")
    class EdinetListDetailV3 {

        @Test
        @DisplayName("submitDate指定 → edinet-list-detail-v2 view 名 + 属性設定")
        void validSubmitDate_returnsV2View() {
            final Model model = mock(Model.class);

            final String result = presenter.edinetListDetailV3("2025-01-15", model);

            assertEquals("edinet-list-detail-v2", result);
            verify(model).addAttribute("submitDate", "2025-01-15");
            verify(model).addAttribute(eq("edinetDetail"), any());
        }
    }
}
