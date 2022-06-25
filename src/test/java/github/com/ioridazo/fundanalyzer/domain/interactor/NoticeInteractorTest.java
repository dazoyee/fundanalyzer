package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

class NoticeInteractorTest {

    private AnalysisResultSpecification analysisResultSpecification;
    private ViewSpecification viewSpecification;
    private SlackClient slackClient;

    private NoticeInteractor noticeInteractor;

    @BeforeEach
    void setUp() {
        analysisResultSpecification = Mockito.mock(AnalysisResultSpecification.class);
        viewSpecification = Mockito.mock(ViewSpecification.class);
        slackClient = Mockito.mock(SlackClient.class);

        noticeInteractor = Mockito.spy(new NoticeInteractor(
                analysisResultSpecification,
                viewSpecification,
                slackClient
        ));
        noticeInteractor.configDiscountRate = BigDecimal.valueOf(120);
    }

    @DisplayName("noticeSlack : Slackに通知する")
    @Test
    void noticeSlack() {
        var inputData = DateInputData.of(LocalDate.parse("2022-06-19"));

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
        var analysisResult = new AnalysisResultEntity(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(viewSpecification.findEdinetListView(inputData)).thenReturn(viewModel);
        when(analysisResultSpecification.findUpdatedList(inputData.getDate())).thenReturn(List.of(analysisResult));

        assertDoesNotThrow(() -> noticeInteractor.noticeSlack(inputData));
        verify(slackClient, times(1))
                .sendMessage(eq("github.com.ioridazo.fundanalyzer.domain.interactor.NoticeInteractor.noticeSlack"), any());
    }
}