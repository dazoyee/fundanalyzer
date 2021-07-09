package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ViewEdinetInteractorTest {

    private CompanySpecification companySpecification;
    private DocumentSpecification documentSpecification;
    private FinancialStatementSpecification financialStatementSpecification;
    private ViewSpecification viewSpecification;
    private SlackClient slackClient;

    private ViewEdinetInteractor viewEdinetInteractor;

    @BeforeEach
    void setUp() {
        companySpecification = Mockito.mock(CompanySpecification.class);
        documentSpecification = Mockito.mock(DocumentSpecification.class);
        financialStatementSpecification = Mockito.mock(FinancialStatementSpecification.class);
        viewSpecification = Mockito.mock(ViewSpecification.class);
        slackClient = Mockito.mock(SlackClient.class);

        viewEdinetInteractor = Mockito.spy(new ViewEdinetInteractor(
                companySpecification,
                documentSpecification,
                financialStatementSpecification,
                viewSpecification,
                slackClient
        ));
        viewEdinetInteractor.edinetListSize = 400;
    }

    @Nested
    class viewMain {

        @DisplayName("viewMain : メインビューを取得する")
        @Test
        void empty() {
            var viewModel = new EdinetListViewModel(
                    null,
                    null,
                    1L,
                    1L,
                    1L,
                    null,
                    null,
                    null,
                    null
            );
            when(viewSpecification.findAllEdinetListView()).thenReturn(List.of(viewModel));

            var actual = viewEdinetInteractor.viewMain();
            assertEquals(0, actual.size());
        }

        @DisplayName("viewMain : メインビューを取得する")
        @Test
        void present() {
            var viewModel = new EdinetListViewModel(
                    null,
                    null,
                    1L,
                    1L,
                    0L,
                    null,
                    null,
                    null,
                    null
            );
            when(viewSpecification.findAllEdinetListView()).thenReturn(List.of(viewModel));

            var actual = viewEdinetInteractor.viewMain();
            assertEquals(1, actual.size());
        }
    }

    @Nested
    class viewEdinetDetail {

        Company company = defaultCompany();
        Document document = defaultDocument();
        FinanceValue financeValue = defaultFinanceValue();
        DateInputData inputData = DateInputData.of(LocalDate.parse("2021-05-16"));

        @DisplayName("viewEdinetDetail : EDINETリスト詳細ビューを取得する")
        @Test
        void of() {
            when(documentSpecification.targetList(inputData)).thenReturn(List.of(document));
            when(documentSpecification.allStatusDone(document)).thenReturn(false);
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(financeValue);

            var actual = viewEdinetInteractor.viewEdinetDetail(inputData);
            assertAll(
                    () -> assertEquals("name", actual.getDocumentDetailList().get(0).getCompanyName()),
                    () -> assertAll(
                            () -> assertEquals("id", actual.getDocumentDetailList().get(0).getDocument().getDocumentId())
                    ),
                    () -> assertAll(
                            () -> assertEquals(100L, actual.getDocumentDetailList().get(0).getFundamentalValue().getNumberOfShares())
                    )
            );

        }
    }

    @Nested
    class updateView {

        Company company = defaultCompany();
        Document document = defaultDocument();
        EdinetListViewModel viewModel = defaultEdinetListViewModel();

        @BeforeEach
        void setUp() {
            doReturn(LocalDate.parse("2021-05-16")).when(viewEdinetInteractor).nowLocalDate();
        }

        @DisplayName("updateView : すべてのビューを更新する")
        @Test
        void all() {
            var inputData = DateInputData.of(LocalDate.parse("2021-05-16"));
            when(documentSpecification.documentList()).thenReturn(List.of(document));
            when(viewSpecification.generateEdinetListView(inputData)).thenReturn(viewModel);

            assertDoesNotThrow(() -> viewEdinetInteractor.updateView());
            verify(viewSpecification, times(1)).upsert(viewModel);
            verify(slackClient, times(1)).sendMessage(any());
        }

        @DisplayName("updateView : 過去の提出日に関してはビューを更新しない")
        @Test
        void old_submitDate() {
            var inputData = DateInputData.of(LocalDate.parse("2019-05-16"));
            when(documentSpecification.documentList()).thenReturn(List.of(document));
            when(viewSpecification.generateEdinetListView(inputData)).thenReturn(viewModel);

            assertDoesNotThrow(() -> viewEdinetInteractor.updateView());
            verify(viewSpecification, times(0)).upsert(viewModel);
            verify(slackClient, times(1)).sendMessage(any());
        }

        @DisplayName("updateView : ビューを更新する")
        @Test
        void submitDate() {
            var inputData = DateInputData.of(LocalDate.parse("2021-05-16"));
            when(viewSpecification.generateEdinetListView(inputData)).thenReturn(viewModel);

            assertDoesNotThrow(() -> viewEdinetInteractor.updateView(inputData));
            verify(viewSpecification, times(1)).upsert(viewModel);
            verify(slackClient, times(0)).sendMessage(any());
        }
    }

    private Company defaultCompany() {
        return new Company(
                "code",
                "name",
                null,
                null,
                "edinetCode",
                null,
                null,
                null,
                null
        );
    }

    private Document defaultDocument() {
        return new Document(
                "id",
                DocumentTypeCode.DTC_120,
                QuarterType.QT_4,
                "edinetCode",
                null,
                LocalDate.parse("2021-05-16"),
                null,
                null,
                DocumentStatus.DONE,
                DocumentStatus.DONE,
                DocumentStatus.ERROR,
                null,
                DocumentStatus.DONE,
                null,
                DocumentStatus.DONE,
                null,
                false
        );
    }

    private FinanceValue defaultFinanceValue() {
        return FinanceValue.of(
                null,
                null,
                null,
                null,
                null,
                100L
        );
    }

    private EdinetListViewModel defaultEdinetListViewModel() {
        return EdinetListViewModel.of(
                null,
                1,
                1,
                1,
                1,
                null,
                null,
                1
        );
    }
}