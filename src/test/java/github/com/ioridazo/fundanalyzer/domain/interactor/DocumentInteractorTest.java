package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.edinet.EdinetClient;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.ListType;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.Metadata;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.Results;
import github.com.ioridazo.fundanalyzer.client.file.FileOperator;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.EdinetDocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.ScrapingUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.Result;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.FinancialStatementInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentInteractorTest {

    private ScrapingUseCase scraping;
    private CompanySpecification companySpecification;
    private DocumentSpecification documentSpecification;
    private EdinetDocumentSpecification edinetDocumentSpecification;
    private FinancialStatementSpecification financialStatementSpecification;
    private FileOperator fileOperator;
    private EdinetClient edinetClient;

    private DocumentInteractor documentInteractor;

    @BeforeEach
    void setUp() {
        scraping = Mockito.mock(ScrapingUseCase.class);
        companySpecification = Mockito.mock(CompanySpecification.class);
        documentSpecification = Mockito.mock(DocumentSpecification.class);
        edinetDocumentSpecification = Mockito.mock(EdinetDocumentSpecification.class);
        financialStatementSpecification = Mockito.mock(FinancialStatementSpecification.class);
        fileOperator = Mockito.mock(FileOperator.class);
        edinetClient = Mockito.mock(EdinetClient.class);

        documentInteractor = Mockito.spy(new DocumentInteractor(
                scraping,
                companySpecification,
                documentSpecification,
                edinetDocumentSpecification,
                financialStatementSpecification,
                fileOperator,
                edinetClient
        ));
        documentInteractor.targetTypeCodes = List.of("120");
    }

    @Nested
    class allProcess {

        DateInputData inputData = DateInputData.of(LocalDate.parse("2021-05-09"));

        @DisplayName("allProcess : ドキュメントを取得してスクレイピングする")
        @Test
        void present() {
            doNothing().when(documentInteractor).saveEdinetList(inputData);
            doNothing().when(documentInteractor).scrape((Document) any());
            when(documentSpecification.targetList(inputData)).thenReturn(List.of(defaultDocument()));

            assertDoesNotThrow(() -> documentInteractor.allProcess(inputData));
            verify(documentInteractor, times(1)).saveEdinetList(inputData);
            verify(documentInteractor, times(1)).scrape((Document) any());
        }

        @DisplayName("allProcess : ドキュメントがないときはなにもしない")
        @Test
        void empty() {
            doNothing().when(documentInteractor).saveEdinetList(inputData);
            doNothing().when(documentInteractor).scrape((Document) any());

            assertDoesNotThrow(() -> documentInteractor.allProcess(inputData));
            verify(documentInteractor, times(1)).saveEdinetList(inputData);
            verify(documentInteractor, times(0)).scrape((Document) any());
        }
    }

    @Nested
    class saveEdinetList {

        @DisplayName("saveEdinetList: EDINETリストを保存する")
        @Test
        void dateInputData_present() {
            var inputData = DateInputData.of(LocalDate.parse("2020-09-19"));

            var resultSet = new Metadata.ResultSet();
            resultSet.setCount("2");
            var metadata = new Metadata();
            metadata.setResultset(resultSet);
            var target = new Results();
            target.setDocId("docId");
            target.setEdinetCode("edinetCode");
            target.setDocTypeCode("120");
            target.setPeriodEnd("2020-12-31");
            var already = new Results();
            already.setDocId("already");
            already.setDocTypeCode("120");
            already.setPeriodEnd("2020-12-31");
            var edinetResponse = new EdinetResponse();
            edinetResponse.setMetadata(metadata);
            edinetResponse.setResults(List.of(target, already));

            when(edinetClient.list(new ListRequestParameter(LocalDate.parse("2020-09-19"), ListType.DEFAULT))).thenReturn(edinetResponse);
            when(edinetClient.list(new ListRequestParameter(LocalDate.parse("2020-09-19"), ListType.GET_LIST))).thenReturn(edinetResponse);

            assertDoesNotThrow(() -> documentInteractor.saveEdinetList(inputData));

            verify(edinetDocumentSpecification, times(1)).insert(edinetResponse);
            verify(companySpecification, times(1)).insertIfNotExist(target);
            verify(companySpecification, times(1)).insertIfNotExist(already);
            verify(documentSpecification, times(1)).insert(LocalDate.parse("2020-09-19"), edinetResponse);
        }

        @DisplayName("saveEdinetList: 対象件数がないときはなにもしない")
        @Test
        void dateInputData_empty() {
            var inputData = DateInputData.of(LocalDate.parse("2020-09-19"));

            var resultSet = new Metadata.ResultSet();
            resultSet.setCount("0");
            var metadata = new Metadata();
            metadata.setResultset(resultSet);
            var edinetResponse = new EdinetResponse();
            edinetResponse.setMetadata(metadata);

            when(edinetClient.list(new ListRequestParameter(LocalDate.parse("2020-09-19"), ListType.DEFAULT))).thenReturn(edinetResponse);

            assertDoesNotThrow(() -> documentInteractor.saveEdinetList(inputData));

            verify(edinetDocumentSpecification, times(0)).insert(edinetResponse);
            verify(companySpecification, times(0)).insertIfNotExist(any());
            verify(documentSpecification, times(0)).insert(LocalDate.parse("2020-09-19"), edinetResponse);
        }
    }

    @Nested
    class scrape {

        @DisplayName("scrape : dateからドキュメントをスクレイピングする")
        @Test
        void dateInputData_present() {
            var inputData = DateInputData.of(LocalDate.parse("2021-05-09"));

            when(documentSpecification.targetList(inputData)).thenReturn(List.of(defaultDocument()));
            doNothing().when(documentInteractor).scrape((Document) any());

            assertDoesNotThrow(() -> documentInteractor.scrape(inputData));
            verify(documentInteractor, times(1)).scrape((Document) any());
        }

        @DisplayName("scrape : ドキュメントがないときはなにもしない")
        @Test
        void dateInputData_empty() {
            var inputData = DateInputData.of(LocalDate.parse("2021-05-09"));
            assertDoesNotThrow(() -> documentInteractor.scrape(inputData));
            verify(documentInteractor, times(0)).scrape((Document) any());
        }

        @DisplayName("scrape : idからドキュメントをスクレイピングする")
        @Test
        void idInputData() {
            var inputData = IdInputData.of("id");

            when(documentSpecification.findDocument("id")).thenReturn(defaultDocument());
            doNothing().when(documentInteractor).scrape((Document) any());

            assertDoesNotThrow(() -> documentInteractor.scrape(inputData));
            verify(documentInteractor, times(1)).scrape((Document) any());
        }

        @Nested
        class execute {

            @BeforeEach
            void setUp() {
                when(documentSpecification.findDocument("notYet")).thenReturn(new Document(
                        "notYet",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        DocumentStatus.DONE,
                        DocumentStatus.DONE,
                        DocumentStatus.NOT_YET,
                        null,
                        DocumentStatus.NOT_YET,
                        null,
                        DocumentStatus.NOT_YET,
                        null,
                        false
                )).thenReturn(new Document(
                        "notYet",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        DocumentStatus.DONE,
                        DocumentStatus.DONE,
                        DocumentStatus.DONE,
                        null,
                        DocumentStatus.DONE,
                        null,
                        DocumentStatus.DONE,
                        null,
                        false
                ));
                when(documentSpecification.findDocument("error")).thenReturn(new Document(
                        "error",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        DocumentStatus.DONE,
                        DocumentStatus.DONE,
                        DocumentStatus.ERROR,
                        null,
                        DocumentStatus.ERROR,
                        null,
                        DocumentStatus.ERROR,
                        null,
                        false
                ));
                when(documentSpecification.findDocument("done")).thenReturn(new Document(
                        "done",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        DocumentStatus.DONE,
                        DocumentStatus.DONE,
                        DocumentStatus.DONE,
                        null,
                        DocumentStatus.DONE,
                        null,
                        DocumentStatus.DONE,
                        null,
                        false
                ));
            }

            @DisplayName("scrape : ダウンロード済であればステータス更新する")
            @Test
            void notYet_download_isPresent() {
                var document = new Document(
                        "notYet",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        DocumentStatus.NOT_YET,
                        DocumentStatus.NOT_YET,
                        DocumentStatus.NOT_YET,
                        null,
                        DocumentStatus.NOT_YET,
                        null,
                        DocumentStatus.NOT_YET,
                        null,
                        false
                );

                when(fileOperator.findDecodedFile(any())).thenReturn(Optional.of(List.of("notYet")));

                assertDoesNotThrow(() -> documentInteractor.scrape(document));
                verify(documentSpecification, times(1)).updateStoreToDone(document);
                verify(scraping, times(0)).download(document);
            }

            @DisplayName("scrape : ダウンロード未済であればファイル取得する")
            @Test
            void notYet_download_isEmpty() {
                var document = new Document(
                        "notYet",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        DocumentStatus.NOT_YET,
                        DocumentStatus.NOT_YET,
                        DocumentStatus.NOT_YET,
                        null,
                        DocumentStatus.NOT_YET,
                        null,
                        DocumentStatus.NOT_YET,
                        null,
                        false
                );

                when(fileOperator.findDecodedFile(any())).thenReturn(Optional.of(List.of()));

                assertDoesNotThrow(() -> documentInteractor.scrape(document));
                verify(documentSpecification, times(0)).updateStoreToDone(document);
                verify(scraping, times(1)).download(document);
            }

            @DisplayName("scrape : ステータス処理前であれば処理する")
            @Test
            void error_decoded() {
                var document = new Document(
                        "error_decoded",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        DocumentStatus.DONE,
                        DocumentStatus.ERROR,
                        DocumentStatus.NOT_YET,
                        null,
                        DocumentStatus.NOT_YET,
                        null,
                        DocumentStatus.NOT_YET,
                        null,
                        false
                );

                when(fileOperator.findDecodedFile(any())).thenReturn(Optional.of(List.of()));
                when(documentSpecification.findDocument("error_decoded")).thenReturn(document);

                assertDoesNotThrow(() -> documentInteractor.scrape(document));
                verify(scraping, times(0)).download(document);
                verify(scraping, times(0)).bs(document);
                verify(scraping, times(0)).pl(document);
                verify(scraping, times(0)).ns(document);
            }

            @DisplayName("scrape : ステータス処理前であれば処理する")
            @Test
            void notYet() {
                var document = new Document(
                        "notYet",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        DocumentStatus.NOT_YET,
                        DocumentStatus.NOT_YET,
                        DocumentStatus.NOT_YET,
                        null,
                        DocumentStatus.NOT_YET,
                        null,
                        DocumentStatus.NOT_YET,
                        null,
                        false
                );

                when(fileOperator.findDecodedFile(any())).thenReturn(Optional.of(List.of()));

                assertDoesNotThrow(() -> documentInteractor.scrape(document));
                verify(scraping, times(1)).download(document);
                verify(scraping, times(1)).bs(document);
                verify(scraping, times(1)).pl(document);
                verify(scraping, times(1)).ns(document);
            }

            @DisplayName("scrape : ステータス失敗であれば処理する")
            @Test
            void error() {
                var document = new Document(
                        "error",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        DocumentStatus.DONE,
                        DocumentStatus.DONE,
                        DocumentStatus.ERROR,
                        null,
                        DocumentStatus.ERROR,
                        null,
                        DocumentStatus.ERROR,
                        null,
                        false
                );

                when(fileOperator.findDecodedFile(any())).thenReturn(Optional.of(List.of()));

                assertDoesNotThrow(() -> documentInteractor.scrape(document));
                verify(scraping, times(0)).download(document);
                verify(scraping, times(1)).bs(document);
                verify(scraping, times(1)).pl(document);
                verify(scraping, times(1)).ns(document);
            }

            @DisplayName("scrape : ステータス処理済であれば処理しない")
            @Test
            void done() {
                var document = new Document(
                        "done",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        DocumentStatus.DONE,
                        DocumentStatus.DONE,
                        DocumentStatus.DONE,
                        null,
                        DocumentStatus.DONE,
                        null,
                        DocumentStatus.DONE,
                        null,
                        false
                );

                when(fileOperator.findDecodedFile(any())).thenReturn(Optional.of(List.of()));

                assertDoesNotThrow(() -> documentInteractor.scrape(document));
                verify(scraping, times(0)).download(document);
                verify(scraping, times(0)).bs(document);
                verify(scraping, times(0)).pl(document);
                verify(scraping, times(0)).ns(document);
            }

            @DisplayName("scrape : 処理ステータスがすべて [9（ERROR）] のときは除外フラグをONにする")
            @Test
            void remove() {
                var document = new Document(
                        "remove",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        DocumentStatus.ERROR,
                        DocumentStatus.ERROR,
                        DocumentStatus.ERROR,
                        null,
                        DocumentStatus.ERROR,
                        null,
                        DocumentStatus.ERROR,
                        null,
                        false
                );

                when(fileOperator.findDecodedFile(any())).thenReturn(Optional.of(List.of()));
                when(documentSpecification.findDocument("remove")).thenReturn(document);

                assertDoesNotThrow(() -> documentInteractor.scrape(document));
                verify(documentSpecification, times(1)).updateRemoved(document);
            }
        }
    }

    @Nested
    class registerFinancialStatementValue {

        private FinancialStatementInputData inputData;

        @BeforeEach
        void setUp() {
            inputData = FinancialStatementInputData.of(
                    "edinetCode",
                    "documentId",
                    "1",
                    "1",
                    1000
            );
        }

        @DisplayName("registerFinancialStatementValue : 財務諸表の値を登録する")
        @Test
        void ok() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(defaultCompany()));
            when(documentSpecification.findDocument("document")).thenReturn(defaultDocument());
            assertEquals(Result.OK, documentInteractor.registerFinancialStatementValue(inputData));
        }

        @DisplayName("registerFinancialStatementValue : 財務諸表の値に失敗したらNGで返却する")
        @Test
        void ng() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenThrow(FundanalyzerNotExistException.class);
            assertEquals(Result.NG, documentInteractor.registerFinancialStatementValue(inputData));
        }
    }

    @Nested
    class updateAllDoneStatus {

        private IdInputData inputData;

        @BeforeEach
        void setUp() {
            inputData = IdInputData.of(
                    "id"
            );
        }

        @DisplayName("updateAllDoneStatus : 処理ステータスを更新する")
        @Test
        void ok() {
            assertEquals(Result.OK, documentInteractor.updateAllDoneStatus(inputData));
        }

        @DisplayName("updateAllDoneStatus : 処理ステータスの更新に失敗したらNGで返却する")
        @Test
        void ng() {
            doThrow(FundanalyzerRuntimeException.class).when(documentSpecification).updateAllDone("id");
            assertEquals(Result.NG, documentInteractor.updateAllDoneStatus(inputData));
        }
    }

    @Nested
    class removeDocument {

        @DisplayName("removeDocument : 指定書類IDを処理対象外にする")
        @Test
        void id_ok() {
            var inputData = IdInputData.of("documentId");
            assertDoesNotThrow(() -> documentInteractor.removeDocument(inputData));
        }

        @DisplayName("removeDocument : 除外条件に合致するドキュメントを処理対象外に更新する")
        @Test
        void date_ok() {
            var inputData = DateInputData.of(LocalDate.parse("2021-09-18"));
            var document = new Document(
                    null,
                    DocumentTypeCode.DTC_140,
                    null,
                    null,
                    null,
                    LocalDate.parse("2021-09-18"),
                    null,
                    null,
                    DocumentStatus.DONE,
                    DocumentStatus.DONE,
                    DocumentStatus.DONE,
                    "path",
                    DocumentStatus.HALF_WAY,
                    null,
                    DocumentStatus.ERROR,
                    null,
                    false
            );

            when(documentSpecification.removeTargetList(inputData)).thenReturn(List.of(document));
            assertDoesNotThrow(() -> documentInteractor.removeDocument(inputData));
            verify(documentSpecification, times(1)).updateRemoved(document);
        }
    }

    private Document defaultDocument() {
        return new Document(
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
                false
        );
    }

    private Company defaultCompany() {
        return new Company(
                "code",
                null,
                null,
                "edinetCode",
                null,
                null,
                null,
                null,
                null
        );
    }
}