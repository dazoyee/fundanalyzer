package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("NewClassNamingConvention")
class NoticeInteractorTest {

    private ViewSpecification viewSpecification;
    private SlackClient slackClient;

    private NoticeInteractor noticeInteractor;

    @BeforeEach
    void setUp() {
        viewSpecification = Mockito.mock(ViewSpecification.class);
        slackClient = Mockito.mock(SlackClient.class);

        noticeInteractor = Mockito.spy(new NoticeInteractor(
                viewSpecification,
                slackClient
        ));
        noticeInteractor.configDiscountRate = BigDecimal.valueOf(120);
    }

    @Nested
    class noticeSlack {

        DateInputData inputData = DateInputData.of(LocalDate.parse("2021-05-16"));

        @DisplayName("noticeSlack : SlackにINFOを通知する")
        @Test
        void info() {
            var viewModel = EdinetListViewModel.of(
                    null,
                    1,
                    1,
                    1,
                    1,
                    null,
                    null,
                    1
            );
            when(viewSpecification.findEdinetListView(inputData)).thenReturn(viewModel);

            assertDoesNotThrow(() -> noticeInteractor.noticeSlack(inputData));
            verify(slackClient, times(1))
                    .sendMessage(eq("g.c.i.f.domain.service.ViewService.processing.notice.info"), any());
        }

        @DisplayName("noticeSlack : SlackにWARNを通知する")
        @Test
        void warn() {
            var viewModel = EdinetListViewModel.of(
                    null,
                    1,
                    1,
                    0,
                    0,
                    null,
                    null,
                    1
            );
            when(viewSpecification.findEdinetListView(inputData)).thenReturn(viewModel);

            assertDoesNotThrow(() -> noticeInteractor.noticeSlack(inputData));
            verify(slackClient, times(1))
                    .sendMessage(eq("g.c.i.f.domain.service.ViewService.processing.notice.warn"), any());
        }

        @DisplayName("noticeSlack : Slackに優良銘柄を通知する")
        // @Test
        void submitDate() {
            var edinetListViewModel = EdinetListViewModel.of(
                    null,
                    1,
                    1,
                    1,
                    1,
                    null,
                    null,
                    1
            );
            when(viewSpecification.findEdinetListView(inputData)).thenReturn(edinetListViewModel);
            var corporateViewModel = CorporateViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    true,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(120),
                    null,
                    null
            );
            when(viewSpecification.findAllCorporateView(inputData)).thenReturn(List.of(corporateViewModel));

            assertDoesNotThrow(() -> noticeInteractor.noticeSlack(inputData));
            verify(slackClient, times(1))
                    .sendMessage(eq("g.c.i.f.domain.service.ViewService.processing.notice.submitDate"), any());
        }
    }
}