package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.Flag;
import github.com.ioridazo.fundanalyzer.domain.entity.master.CompanyEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.DocumentEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocumentEntity;
import github.com.ioridazo.fundanalyzer.domain.logic.company.CompanyLogic;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.ScrapingLogic;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.proxy.edinet.EdinetProxy;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request.ListType;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.response.Metadata;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.response.Results;
import github.com.ioridazo.fundanalyzer.proxy.selenium.SeleniumProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.seasar.doma.jdbc.UniqueConstraintException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    private EdinetProxy edinetProxy;
    private SeleniumProxy seleniumProxy;
    private ScrapingLogic scrapingLogic;
    private CompanyDao companyDao;
    private EdinetDocumentDao edinetDocumentDao;
    private DocumentDao documentDao;

    private DocumentService service;

    @BeforeEach
    void before() {
        this.edinetProxy = Mockito.mock(EdinetProxy.class);
        this.seleniumProxy = Mockito.mock(SeleniumProxy.class);
        this.scrapingLogic = Mockito.mock(ScrapingLogic.class);
        this.companyDao = Mockito.mock(CompanyDao.class);
        this.edinetDocumentDao = Mockito.mock(EdinetDocumentDao.class);
        this.documentDao = Mockito.mock(DocumentDao.class);

        this.service = Mockito.spy(new DocumentService(
                "",
                "",
                "C:/test/decode",
                edinetProxy,
                seleniumProxy,
                Mockito.mock(CompanyLogic.class),
                scrapingLogic,
                companyDao,
                Mockito.mock(BsSubjectDao.class),
                Mockito.mock(PlSubjectDao.class),
                edinetDocumentDao,
                documentDao
        ));
    }

    @Nested
    class company {

        @DisplayName("downloadCompanyInfo : Seleniumで会社情報一覧を取得し、業種と会社情報をDBに登録する")
        @Test
        void downloadCompanyInfo_ok() {
            assertDoesNotThrow(() -> service.downloadCompanyInfo());
        }

        @DisplayName("downloadCompanyInfo : Selenium処理中にエラーが発生したときの挙動を確認する")
        @Test
        void downloadCompanyInfo_exception() {
            when(seleniumProxy.edinetCodeList(any())).thenThrow(FundanalyzerRestClientException.class);
            assertThrows(FundanalyzerRuntimeException.class, () -> service.downloadCompanyInfo());
        }
    }

    @Nested
    class execute {

        @DisplayName("execute: 一連処理が正常に処理されることを確認する")
        @Test
        void execute_ok() {
            var date = "2020-09-19";
            var targetTypes = List.of(DocumentTypeCode.DTC_120);

            doNothing().when(service).edinetList(LocalDate.parse(date));
            when(documentDao.selectByTypeAndSubmitDate(List.of("120"), LocalDate.parse(date)))
                    .thenReturn(List.of(
                            DocumentEntity.builder()
                                    .id(1)
                                    .documentId("documentId1")
                                    .documentTypeCode("120")
                                    .edinetCode("E00001")
                                    .submitDate(LocalDate.parse("2020-09-19"))
                                    .downloaded("0")
                                    .decoded("0")
                                    .scrapedNumberOfShares("0")
                                    .scrapedBs("0")
                                    .scrapedPl("0")
                                    .removed("0")
                                    .build(),
                            DocumentEntity.builder()
                                    .id(2)
                                    .documentId("documentId2")
                                    .documentTypeCode("120")
                                    .edinetCode("E00002")
                                    .submitDate(LocalDate.parse("2020-09-19"))
                                    .build(),
                            DocumentEntity.builder()
                                    .id(3)
                                    .documentId("documentId2")
                                    .documentTypeCode("120")
                                    .edinetCode("E00003")
                                    .submitDate(LocalDate.parse("2020-09-19"))
                                    .build()

                    ));
            when(companyDao.selectByEdinetCode("E00001")).thenReturn(Optional.of(new CompanyEntity(
                    "0001",
                    "対象となる会社",
                    1,
                    "E00001",
                    "1",
                    "1",
                    1,
                    "2020-09-19",
                    null,
                    null
            )));
            when(companyDao.selectByEdinetCode("E00002")).thenReturn(Optional.of(new CompanyEntity(
                    null,
                    "対象外となる会社",
                    1,
                    "E00002",
                    "1",
                    "1",
                    1,
                    "2020-09-19",
                    null,
                    null
            )));
            when(documentDao.selectByDocumentId("documentId1")).thenReturn(
                    DocumentEntity.builder()
                            .id(1)
                            .documentId("documentId1")
                            .documentTypeCode("120")
                            .edinetCode("E00001")
                            .submitDate(LocalDate.parse("2020-09-19"))
                            .downloaded("1")
                            .decoded("1")
                            .scrapedNumberOfShares("0")
                            .scrapedBs("0")
                            .scrapedPl("0")
                            .removed("0")
                            .build()
            ).thenReturn(
                    DocumentEntity.builder()
                            .id(1)
                            .documentId("documentId1")
                            .documentTypeCode("120")
                            .edinetCode("E00001")
                            .submitDate(LocalDate.parse("2020-09-19"))
                            .downloaded("1")
                            .decoded("1")
                            .scrapedNumberOfShares("1")
                            .scrapedBs("1")
                            .scrapedPl("1")
                            .removed("0")
                            .build()
            );
            when(documentDao.selectByDocumentId("documentId2")).thenReturn(
                    DocumentEntity.builder()
                            .id(2)
                            .documentId("documentId2")
                            .documentTypeCode("120")
                            .edinetCode("E00002")
                            .submitDate(LocalDate.parse("2020-09-19"))
                            .downloaded("1")
                            .decoded("1")
                            .scrapedNumberOfShares("0")
                            .scrapedBs("0")
                            .scrapedPl("0")
                            .removed("0")
                            .build()
            ).thenReturn(
                    DocumentEntity.builder()
                            .id(2)
                            .documentId("documentId2")
                            .documentTypeCode("120")
                            .edinetCode("E00002")
                            .submitDate(LocalDate.parse("2020-09-19"))
                            .downloaded("1")
                            .decoded("1")
                            .scrapedNumberOfShares("1")
                            .scrapedBs("1")
                            .scrapedPl("1")
                            .removed("0")
                            .build()
            );
            when(documentDao.selectByDocumentId("documentId3")).thenReturn(
                    DocumentEntity.builder()
                            .id(3)
                            .documentId("documentId2")
                            .documentTypeCode("120")
                            .edinetCode("E00003")
                            .submitDate(LocalDate.parse("2020-09-19"))
                            .downloaded("1")
                            .decoded("1")
                            .scrapedNumberOfShares("0")
                            .scrapedBs("0")
                            .scrapedPl("0")
                            .removed("0")
                            .build()
            ).thenReturn(
                    DocumentEntity.builder()
                            .id(3)
                            .documentId("documentId2")
                            .documentTypeCode("120")
                            .edinetCode("E00003")
                            .submitDate(LocalDate.parse("2020-09-19"))
                            .downloaded("1")
                            .decoded("1")
                            .scrapedNumberOfShares("1")
                            .scrapedBs("1")
                            .scrapedPl("1")
                            .removed("0")
                            .build()
            );

            assertDoesNotThrow(() -> service.execute(date, targetTypes));

            verify(service, times(1)).store("documentId1", LocalDate.parse(date));
            verify(scrapingLogic, times(1))
                    .scrape(eq(FinancialStatementEnum.BALANCE_SHEET), eq("documentId1"), eq(LocalDate.parse(date)), any());
            verify(scrapingLogic, times(1))
                    .scrape(eq(FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT), eq("documentId1"), eq(LocalDate.parse(date)), any());
            verify(scrapingLogic, times(1))
                    .scrape(eq(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES), eq("documentId1"), eq(LocalDate.parse(date)), eq(null));
        }

        @DisplayName("execute: 対象が存在しないときの処理を確認する")
        @Test
        void execute_isEmpty() {
            var date = "2020-09-19";
            var targetTypes = List.of(DocumentTypeCode.DTC_120);

            doNothing().when(service).edinetList(LocalDate.parse(date));
            when(documentDao.selectByTypeAndSubmitDate(List.of("120"), LocalDate.parse(date)))
                    .thenReturn(List.of(DocumentEntity.builder()
                            .id(1)
                            .documentId("documentId1")
                            .documentTypeCode("120")
                            .edinetCode("E00001")
                            .submitDate(LocalDate.parse("2020-09-19"))
                            .downloaded("0")
                            .decoded("0")
                            .scrapedNumberOfShares("0")
                            .scrapedBs("0")
                            .scrapedPl("0")
                            .removed("0")
                            .build()
                    ));
            when(companyDao.selectByEdinetCode("E00001")).thenReturn(Optional.of(new CompanyEntity(
                    null,
                    "対象となる会社",
                    1,
                    "E00001",
                    "1",
                    "1",
                    1,
                    "2020-09-19",
                    null,
                    null
            )));

            assertDoesNotThrow(() -> service.execute(date, targetTypes));

            verify(service, times(0)).store(any(), any());
            verify(scrapingLogic, times(0)).scrape(any(), any(), any(), any());
            verify(scrapingLogic, times(0)).scrape(any(), any(), any(), any());
            verify(scrapingLogic, times(0)).scrape(any(), any(), any(), any());
        }

        @DisplayName("execute: ZIP解凍に失敗したときはスクレイピング処理しない")
        @Test
        void execute_decode_error() {
            var date = "2020-09-19";
            var targetTypes = List.of(DocumentTypeCode.DTC_120);

            doNothing().when(service).edinetList(LocalDate.parse(date));
            when(documentDao.selectByTypeAndSubmitDate(List.of("120"), LocalDate.parse(date)))
                    .thenReturn(List.of(
                            DocumentEntity.builder()
                                    .id(1)
                                    .documentId("documentId1")
                                    .documentTypeCode("120")
                                    .edinetCode("E00001")
                                    .submitDate(LocalDate.parse("2020-09-19"))
                                    .downloaded("0")
                                    .decoded("0")
                                    .scrapedNumberOfShares("0")
                                    .scrapedBs("0")
                                    .scrapedPl("0")
                                    .removed("0")
                                    .build()
                    ));
            when(companyDao.selectByEdinetCode("E00001")).thenReturn(Optional.of(new CompanyEntity(
                    "0001",
                    "対象となる会社",
                    1,
                    "E00001",
                    "1",
                    "1",
                    1,
                    "2020-09-19",
                    null,
                    null
            )));
            when(documentDao.selectByDocumentId("documentId1")).thenReturn(
                    DocumentEntity.builder()
                            .id(1)
                            .documentId("documentId1")
                            .documentTypeCode("120")
                            .edinetCode("E00001")
                            .submitDate(LocalDate.parse("2020-09-19"))
                            .downloaded("1")
                            .decoded("9")
                            .scrapedNumberOfShares("0")
                            .scrapedBs("0")
                            .scrapedPl("0")
                            .removed("0")
                            .build()
            );

            assertDoesNotThrow(() -> service.execute(date, targetTypes));

            verify(service, times(1)).store("documentId1", LocalDate.parse(date));
            verify(scrapingLogic, times(0))
                    .scrape(eq(FinancialStatementEnum.BALANCE_SHEET), eq("documentId1"), eq(LocalDate.parse(date)), any());
            verify(scrapingLogic, times(0))
                    .scrape(eq(FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT), eq("documentId1"), eq(LocalDate.parse(date)), any());
            verify(scrapingLogic, times(0))
                    .scrape(eq(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES), eq("documentId1"), eq(LocalDate.parse(date)), eq(null));
        }

        @DisplayName("execute: 処理ステータスが完了しているときの処理を確認する")
        @Test
        void execute_status_done() {
            var date = "2020-09-19";
            var targetTypes = List.of(DocumentTypeCode.DTC_120);

            doNothing().when(service).edinetList(LocalDate.parse(date));
            when(documentDao.selectByTypeAndSubmitDate(List.of("120"), LocalDate.parse(date)))
                    .thenReturn(List.of(
                            DocumentEntity.builder()
                                    .id(1)
                                    .documentId("documentId1")
                                    .documentTypeCode("120")
                                    .edinetCode("E00001")
                                    .submitDate(LocalDate.parse("2020-09-19"))
                                    .downloaded("1")
                                    .decoded("1")
                                    .scrapedNumberOfShares("1")
                                    .scrapedBs("1")
                                    .scrapedPl("1")
                                    .removed("1")
                                    .build()));
            when(companyDao.selectByEdinetCode("E00001")).thenReturn(Optional.of(new CompanyEntity(
                    "0001",
                    "対象となる会社",
                    1,
                    "E00001",
                    "1",
                    "1",
                    1,
                    "2020-09-19",
                    null,
                    null
            )));

            assertDoesNotThrow(() -> service.execute(date, targetTypes));

            verify(service, times(0)).store("documentId1", LocalDate.parse(date));
            verify(scrapingLogic, times(0))
                    .scrape(eq(FinancialStatementEnum.BALANCE_SHEET), eq("documentId1"), eq(LocalDate.parse(date)), any());
            verify(scrapingLogic, times(0))
                    .scrape(eq(FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT), eq("documentId1"), eq(LocalDate.parse(date)), any());
            verify(scrapingLogic, times(0))
                    .scrape(eq(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES), eq("documentId1"), eq(LocalDate.parse(date)), eq(null));
        }

        @DisplayName("execute: 処理ステータスがエラーのときに除外フラグONにすることを確認する")
        @Test
        void execute_status_error() {
            var date = "2020-09-19";
            var targetTypes = List.of(DocumentTypeCode.DTC_120);
            var updatedAt = LocalDateTime.of(2020, 9, 19, 17, 39);

            doNothing().when(service).edinetList(LocalDate.parse(date));
            when(documentDao.selectByTypeAndSubmitDate(List.of("120"), LocalDate.parse(date)))
                    .thenReturn(List.of(
                            DocumentEntity.builder()
                                    .id(1)
                                    .documentId("documentId1")
                                    .documentTypeCode("120")
                                    .edinetCode("E00001")
                                    .submitDate(LocalDate.parse("2020-09-19"))
                                    .downloaded("0")
                                    .decoded("0")
                                    .scrapedNumberOfShares("0")
                                    .scrapedBs("0")
                                    .scrapedPl("0")
                                    .removed("0")
                                    .build()));
            when(companyDao.selectByEdinetCode("E00001")).thenReturn(Optional.of(new CompanyEntity(
                    "0001",
                    "対象となる会社",
                    1,
                    "E00001",
                    "1",
                    "1",
                    1,
                    "2020-09-19",
                    null,
                    null
            )));
            when(service.nowLocalDateTime()).thenReturn(updatedAt);
            when(documentDao.selectByDocumentId("documentId1")).thenReturn(
                    DocumentEntity.builder()
                            .id(1)
                            .documentId("documentId1")
                            .documentTypeCode("120")
                            .edinetCode("E00001")
                            .submitDate(LocalDate.parse("2020-09-19"))
                            .downloaded("1")
                            .decoded("1")
                            .scrapedNumberOfShares("0")
                            .scrapedBs("0")
                            .scrapedPl("0")
                            .removed("0")
                            .build()
            ).thenReturn(
                    DocumentEntity.builder()
                            .id(1)
                            .documentId("documentId1")
                            .documentTypeCode("120")
                            .edinetCode("E00001")
                            .submitDate(LocalDate.parse("2020-09-19"))
                            .downloaded("1")
                            .decoded("1")
                            .scrapedNumberOfShares("9")
                            .scrapedBs("9")
                            .scrapedPl("9")
                            .removed("0")
                            .build()
            );

            assertDoesNotThrow(() -> service.execute(date, targetTypes));

            verify(documentDao, times(1)).update(DocumentEntity.builder()
                    .documentId("documentId1")
                    .removed(Flag.ON.toValue())
                    .updatedAt(updatedAt)
                    .build());
        }
    }

    @Nested
    class edinetList {

        @DisplayName("edinetList: 指定日付分だけ登録処理が実行されることを確認する")
        @Test
        void edinetList_ok_start_end() {
            var startDate = "2020-09-15";
            var endDate = "2020-09-19";

            doNothing().when(service).edinetList(any());

            assertDoesNotThrow(() -> service.edinetList(startDate, endDate));

            verify(service, times(5)).edinetList(any());
        }

        @DisplayName("edinetList: 一連処理が正常に処理されることを確認する")
        @Test
        void edinetList_ok_date() {
            var date = LocalDate.parse("2020-09-19");
            var edinetDocument = new EdinetDocumentEntity();
            edinetDocument.setDocId("already");
            var document = DocumentEntity.builder().documentId("already").build();
            var resultSet = new Metadata.ResultSet();
            resultSet.setCount("2");
            var metadata = new Metadata();
            metadata.setResultset(resultSet);
            var resultsInserted = new Results();
            resultsInserted.setDocId("docId");
            resultsInserted.setEdinetCode("edinetCode");
            resultsInserted.setDocTypeCode("120");
            resultsInserted.setPeriodEnd("2020-12-31");
            var resultsAlready = new Results();
            resultsAlready.setDocId("already");
            resultsAlready.setDocTypeCode("120");
            resultsAlready.setPeriodEnd("2020-12-31");
            var edinetResponse = new EdinetResponse();
            edinetResponse.setMetadata(metadata);
            edinetResponse.setResults(List.of(resultsInserted, resultsAlready));
            var createdAt = LocalDateTime.of(2020, 9, 19, 17, 39);

            when(edinetDocumentDao.selectAll()).thenReturn(List.of(edinetDocument));
            when(documentDao.selectBySubmitDate(date)).thenReturn(List.of(document));
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.DEFAULT))).thenReturn(edinetResponse);
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.GET_LIST))).thenReturn(edinetResponse);
            when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.of(new CompanyEntity(
                    null,
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )));
            when(service.nowLocalDateTime()).thenReturn(createdAt);

            assertDoesNotThrow(() -> service.edinetList(date));

            verify(edinetDocumentDao, times(1)).insert(EdinetDocumentEntity.of(resultsInserted, createdAt));
            verify(edinetDocumentDao, times(0)).insert(EdinetDocumentEntity.of(resultsAlready, createdAt));
            verify(documentDao, times(1)).insert(DocumentEntity.builder()
                    .documentId(resultsInserted.getDocId())
                    .documentTypeCode(resultsInserted.getDocTypeCode().orElseThrow())
                    .edinetCode(resultsInserted.getEdinetCode().orElse(null))
                    .documentPeriod(LocalDate.of(Integer.parseInt(resultsInserted.getPeriodEnd().substring(0, 4)), 1, 1))
                    .submitDate(date)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build());
            verify(documentDao, times(0)).insert(DocumentEntity.builder()
                    .documentId(resultsAlready.getDocId())
                    .documentTypeCode(resultsAlready.getDocTypeCode().orElseThrow())
                    .edinetCode(resultsAlready.getEdinetCode().orElse(null))
                    .documentPeriod(LocalDate.of(Integer.parseInt(resultsAlready.getPeriodEnd().substring(0, 4)), 1, 1))
                    .submitDate(date)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build());
        }

        @DisplayName("edinetList: 対象件数がないときの確認する")
        @Test
        void edinetList_nothing() {
            var date = LocalDate.parse("2020-09-19");
            var resultSet = new Metadata.ResultSet();
            resultSet.setCount("0");
            var metadata = new Metadata();
            metadata.setResultset(resultSet);
            var edinetResponse = new EdinetResponse();
            edinetResponse.setMetadata(metadata);

            when(edinetDocumentDao.selectAll()).thenReturn(List.of(new EdinetDocumentEntity()));
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.DEFAULT))).thenReturn(edinetResponse);

            assertDoesNotThrow(() -> service.edinetList(date));

            verify(edinetDocumentDao, times(0)).insert(any());
            verify(documentDao, times(0)).insert(any());
        }

        @DisplayName("edinetList: companyに存在しなかったら仮登録することを確認する")
        @Test
        void edinetList_insertCompanyForSqlForeignKey() {
            var date = LocalDate.parse("2020-09-19");
            var resultSet = new Metadata.ResultSet();
            resultSet.setCount("1");
            var metadata = new Metadata();
            metadata.setResultset(resultSet);
            var resultsInserted = new Results();
            resultsInserted.setDocId("docId");
            resultsInserted.setFilerName("filerName");
            resultsInserted.setEdinetCode("edinetCode");
            resultsInserted.setDocTypeCode("120");
            resultsInserted.setPeriodEnd("2020-12-31");
            var edinetResponse = new EdinetResponse();
            edinetResponse.setMetadata(metadata);
            edinetResponse.setResults(List.of(resultsInserted));
            var createdAt = LocalDateTime.of(2020, 9, 19, 17, 39);

            when(edinetDocumentDao.selectAll()).thenReturn(List.of());
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.DEFAULT))).thenReturn(edinetResponse);
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.GET_LIST))).thenReturn(edinetResponse);
            when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.empty());
            when(service.nowLocalDateTime()).thenReturn(createdAt);

            assertDoesNotThrow(() -> service.edinetList(date));

            verify(companyDao, times(1)).insert(CompanyEntity.ofSqlForeignKey("edinetCode", "filerName", createdAt));
        }

        @Disabled("catchできない")
        @DisplayName("edinetList: エラー発生したときの処理を確認する")
//        @Test
        void edinetList_UniqueConstraintException() {
            var date = LocalDate.parse("2020-09-19");
            var resultSet = new Metadata.ResultSet();
            resultSet.setCount("1");
            var metadata = new Metadata();
            metadata.setResultset(resultSet);
            var resultsInserted = new Results();
            resultsInserted.setDocId("docId");
            resultsInserted.setEdinetCode("edinetCode");
            resultsInserted.setPeriodEnd("2020-12-31");
            var edinetResponse = new EdinetResponse();
            edinetResponse.setMetadata(metadata);
            edinetResponse.setResults(List.of(resultsInserted));
            var createdAt = LocalDateTime.of(2020, 9, 19, 17, 39);

            when(edinetDocumentDao.selectAll()).thenReturn(List.of());
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.DEFAULT))).thenReturn(edinetResponse);
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.GET_LIST))).thenReturn(edinetResponse);
            when(edinetDocumentDao.insert(any())).thenThrow(UniqueConstraintException.class);
            when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.of(new CompanyEntity(
                    null,
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )));
            when(documentDao.insert(any())).thenThrow(UniqueConstraintException.class);
            when(service.nowLocalDateTime()).thenReturn(createdAt);

//            assertDoesNotThrow(() -> service.edinetList(date));
            assertThrows(UniqueConstraintException.class, () -> service.edinetList(date));

            verify(edinetDocumentDao, times(0)).insert(EdinetDocumentEntity.of(resultsInserted, createdAt));
            verify(documentDao, times(0)).insert(DocumentEntity.builder()
                    .documentId(resultsInserted.getDocId())
                    .documentTypeCode(resultsInserted.getDocTypeCode().orElseThrow())
                    .edinetCode(resultsInserted.getEdinetCode().orElse(null))
                    .documentPeriod(LocalDate.of(Integer.parseInt(resultsInserted.getPeriodEnd().substring(0, 4)), 1, 1))
                    .submitDate(date)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build());
        }

        @DisplayName("edinetList : periodEndが存在するときはパースしてdocumentPeriodを生成する")
        @Test
        void edinetList_documentPeriod_present() {
            var date = LocalDate.parse("2021-03-22");
            var edinetDocument = new EdinetDocumentEntity();
            edinetDocument.setDocId("already");
            var resultSet = new Metadata.ResultSet();
            resultSet.setCount("2");
            var metadata = new Metadata();
            metadata.setResultset(resultSet);
            var resultsInserted = new Results();
            resultsInserted.setDocId("docId");
            resultsInserted.setEdinetCode("edinetCode");
            resultsInserted.setDocTypeCode("120");
            resultsInserted.setPeriodEnd("2020-12-31");
            var edinetResponse = new EdinetResponse();
            edinetResponse.setMetadata(metadata);
            edinetResponse.setResults(List.of(resultsInserted));
            var createdAt = LocalDateTime.of(2020, 9, 19, 17, 39);

            when(edinetDocumentDao.selectAll()).thenReturn(List.of());
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.DEFAULT))).thenReturn(edinetResponse);
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.GET_LIST))).thenReturn(edinetResponse);
            when(service.nowLocalDateTime()).thenReturn(createdAt);

            service.edinetList(date);

            verify(documentDao, times(1)).insert(DocumentEntity.builder()
                    .documentId("docId")
                    .documentTypeCode("120")
                    .edinetCode("edinetCode")
                    .documentPeriod(LocalDate.parse("2020-01-01"))
                    .submitDate(date)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build());

        }

        @DisplayName("edinetList : periodEndが存在しないときは親書類からdocumentPeriodを生成する")
        @Test
        void edinetList_documentPeriod_null_parentDocument_present() {
            var date = LocalDate.parse("2021-03-22");
            var edinetDocument = new EdinetDocumentEntity();
            edinetDocument.setDocId("already");
            var resultSet = new Metadata.ResultSet();
            resultSet.setCount("2");
            var metadata = new Metadata();
            metadata.setResultset(resultSet);
            var resultsInserted = new Results();
            resultsInserted.setDocId("docId2");
            resultsInserted.setEdinetCode("edinetCode");
            resultsInserted.setDocTypeCode("120");
            resultsInserted.setParentDocID("docId");
            var edinetResponse = new EdinetResponse();
            edinetResponse.setMetadata(metadata);
            edinetResponse.setResults(List.of(resultsInserted));
            var createdAt = LocalDateTime.of(2020, 9, 19, 17, 39);

            when(edinetDocumentDao.selectAll()).thenReturn(List.of());
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.DEFAULT))).thenReturn(edinetResponse);
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.GET_LIST))).thenReturn(edinetResponse);
            when(documentDao.selectByDocumentId("docId")).thenReturn(DocumentEntity.builder().documentPeriod(LocalDate.parse("2020-01-01")).build());
            when(service.nowLocalDateTime()).thenReturn(createdAt);

            service.edinetList(date);

            verify(documentDao, times(1)).insert(DocumentEntity.builder()
                    .documentId("docId2")
                    .documentTypeCode("120")
                    .edinetCode("edinetCode")
                    .documentPeriod(LocalDate.parse("2020-01-01"))
                    .submitDate(date)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build());
        }

        @DisplayName("edinetList : periodEndも親書類も存在しないときはnullの意をこめて1970-01-01にする（手パッチ対象）")
        @Test
        void edinetList_documentPeriod_null_parentDocument_null() {
            var date = LocalDate.parse("2021-03-22");
            var edinetDocument = new EdinetDocumentEntity();
            edinetDocument.setDocId("already");
            var resultSet = new Metadata.ResultSet();
            resultSet.setCount("2");
            var metadata = new Metadata();
            metadata.setResultset(resultSet);
            var resultsInserted = new Results();
            resultsInserted.setDocId("docId");
            resultsInserted.setEdinetCode("edinetCode");
            resultsInserted.setDocTypeCode("130");
            var edinetResponse = new EdinetResponse();
            edinetResponse.setMetadata(metadata);
            edinetResponse.setResults(List.of(resultsInserted));
            var createdAt = LocalDateTime.of(2020, 9, 19, 17, 39);

            when(edinetDocumentDao.selectAll()).thenReturn(List.of());
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.DEFAULT))).thenReturn(edinetResponse);
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.GET_LIST))).thenReturn(edinetResponse);
            when(service.nowLocalDateTime()).thenReturn(createdAt);

            service.edinetList(date);

            verify(documentDao, times(1)).insert(DocumentEntity.builder()
                    .documentId("docId")
                    .documentTypeCode("130")
                    .edinetCode("edinetCode")
                    .documentPeriod(LocalDate.EPOCH)
                    .submitDate(date)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build());

        }

        @DisplayName("edinetList : 対象外の書類種別コードならdocumentPeriodはnullで登録する")
        @Test
        void edinetList_documentPeriod_no_target() {
            var date = LocalDate.parse("2021-03-22");
            var edinetDocument = new EdinetDocumentEntity();
            edinetDocument.setDocId("already");
            var resultSet = new Metadata.ResultSet();
            resultSet.setCount("2");
            var metadata = new Metadata();
            metadata.setResultset(resultSet);
            var resultsInserted = new Results();
            resultsInserted.setDocId("docId");
            resultsInserted.setEdinetCode("edinetCode");
            resultsInserted.setDocTypeCode("140");
            var edinetResponse = new EdinetResponse();
            edinetResponse.setMetadata(metadata);
            edinetResponse.setResults(List.of(resultsInserted));
            var createdAt = LocalDateTime.of(2020, 9, 19, 17, 39);

            when(edinetDocumentDao.selectAll()).thenReturn(List.of());
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.DEFAULT))).thenReturn(edinetResponse);
//            when(edinetProxy.list(new ListRequestParameter(date.toString(), ListType.GET_LIST))).thenReturn(edinetResponse);
            when(service.nowLocalDateTime()).thenReturn(createdAt);

            service.edinetList(date);

            verify(documentDao, times(1)).insert(DocumentEntity.builder()
                    .documentId("docId")
                    .documentTypeCode("140")
                    .edinetCode("edinetCode")
                    .documentPeriod(null)
                    .submitDate(date)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build());

        }
    }

    @Nested
    class store {

        @DisplayName("store: フォルダにファイルが存在していないときにダウンロードすることを確認する")
        @Test
        void store_download() {
            var docId = "docId";
            var targetDate = LocalDate.parse("2020-09-20");

            doNothing().when(scrapingLogic).download(docId, targetDate);

            assertDoesNotThrow(() -> service.store(docId, targetDate));

            verify(scrapingLogic, times(1)).download(docId, targetDate);
        }

        @DisplayName("store: フォルダにファイルが存在しているときにステータス更新することを確認する")
        @Test
        void store_update() {
            var docId = "docId";
            var targetDate = LocalDate.parse("2020-09-20");
            var updatedAt = LocalDateTime.of(2020, 9, 20, 18, 23);

            when(service.fileListAlready(targetDate)).thenReturn(Optional.of(List.of("docId")));
            when(service.nowLocalDateTime()).thenReturn(updatedAt);

            assertDoesNotThrow(() -> service.store(docId, targetDate));

            verify(documentDao, times(1)).update(DocumentEntity.builder()
                    .documentId(docId)
                    .downloaded(DocumentStatus.DONE.toValue())
                    .decoded(DocumentStatus.DONE.toValue())
                    .updatedAt(updatedAt)
                    .build());
            verify(scrapingLogic, times(0)).download(docId, targetDate);
        }
    }

    @Nested
    class scrape {

        @DisplayName("scrape: 対象日付に対してステータス処理前であれば処理することを確認する")
        @Test
        void scrape_submitDate_not_yet() {
            var submitDate = LocalDate.parse("2020-09-22");
            var targetTypes = List.of(DocumentTypeCode.DTC_120);

            when(documentDao.selectByTypeAndSubmitDate(List.of("120"), submitDate))
                    .thenReturn(List.of(
                            DocumentEntity.builder()
                                    .documentId("id")
                                    .edinetCode("target")
                                    .submitDate(LocalDate.parse("2020-09-22"))
                                    .downloaded("0")
                                    .decoded("0")
                                    .scrapedBs("0")
                                    .scrapedPl("0")
                                    .scrapedNumberOfShares("0")
                                    .removed("0")
                                    .build(),
                            DocumentEntity.builder()
                                    .edinetCode("noTarget")
                                    .submitDate(LocalDate.parse("2020-09-22"))
                                    .downloaded("0")
                                    .decoded("0")
                                    .scrapedBs("0")
                                    .scrapedPl("0")
                                    .scrapedNumberOfShares("0")
                                    .build(),
                            DocumentEntity.builder()
                                    .edinetCode("removed")
                                    .submitDate(LocalDate.parse("2020-09-22"))
                                    .downloaded("0")
                                    .decoded("0")
                                    .scrapedBs("0")
                                    .scrapedPl("0")
                                    .scrapedNumberOfShares("0")
                                    .removed("1")
                                    .build()
                    ));
            when(companyDao.selectByEdinetCode(eq("target"))).thenReturn(Optional.of(new CompanyEntity(
                    "code",
                    null,
                    null,
                    "target",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )));
            when(documentDao.selectByDocumentId("id")).thenReturn(
                    DocumentEntity.builder()
                            .documentId("id")
                            .edinetCode("target")
                            .downloaded("1")
                            .decoded("1")
                            .scrapedBs("0")
                            .scrapedPl("0")
                            .scrapedNumberOfShares("0")
                            .removed("0")
                            .build()
            );

            assertDoesNotThrow(() -> service.scrape(submitDate, targetTypes));

            verify(scrapingLogic, times(1)).scrape(
                    eq(FinancialStatementEnum.BALANCE_SHEET),
                    eq("id"),
                    eq(submitDate),
                    any());
            verify(scrapingLogic, times(1)).scrape(
                    eq(FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT),
                    eq("id"),
                    eq(submitDate),
                    any());
            verify(scrapingLogic, times(1)).scrape(
                    eq(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES),
                    eq("id"),
                    eq(submitDate),
                    eq(null));
        }

        @DisplayName("scrape: 対象日付に対してステータス処理済であれば処理しないことを確認する")
        @Test
        void scrape_submitDate_done() {
            var submitDate = LocalDate.parse("2020-09-22");
            var targetTypes = List.of(DocumentTypeCode.DTC_120);

            when(documentDao.selectByTypeAndSubmitDate(List.of("120"), submitDate))
                    .thenReturn(List.of(
                            DocumentEntity.builder()
                                    .documentId("id")
                                    .edinetCode("target")
                                    .submitDate(LocalDate.parse("2020-09-22"))
                                    .downloaded("1")
                                    .decoded("1")
                                    .scrapedBs("1")
                                    .scrapedPl("1")
                                    .scrapedNumberOfShares("0")
                                    .removed("0")
                                    .build()
                    ));
            when(companyDao.selectByEdinetCode(eq("target"))).thenReturn(Optional.of(new CompanyEntity(
                    "code",
                    null,
                    null,
                    "target",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )));
            when(documentDao.selectByDocumentId("id")).thenReturn(
                    DocumentEntity.builder()
                            .documentId("id")
                            .edinetCode("target")
                            .submitDate(LocalDate.parse("2020-09-22"))
                            .downloaded("1")
                            .decoded("1")
                            .scrapedBs("1")
                            .scrapedPl("1")
                            .scrapedNumberOfShares("0")
                            .removed("0")
                            .build()
            );

            assertDoesNotThrow(() -> service.scrape(submitDate, targetTypes));

            verify(scrapingLogic, times(0)).scrape(
                    eq(FinancialStatementEnum.BALANCE_SHEET),
                    eq("id"),
                    eq(submitDate),
                    any());
            verify(scrapingLogic, times(0)).scrape(
                    eq(FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT),
                    eq("id"),
                    eq(submitDate),
                    any());
            verify(scrapingLogic, times(1)).scrape(
                    eq(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES),
                    eq("id"),
                    eq(submitDate),
                    eq(null));
        }

        @DisplayName("scrape: 対象日付に対して処理対象が存在しないときはなにもしない")
        @Test
        void scrape_submitDate_nothing() {
            var submitDate = LocalDate.parse("2020-09-22");
            var targetTypes = List.of(DocumentTypeCode.DTC_120);

            when(documentDao.selectByTypeAndSubmitDate(List.of("120"), submitDate))
                    .thenReturn(List.of());

            assertDoesNotThrow(() -> service.scrape(submitDate, targetTypes));

            verify(scrapingLogic, times(0)).scrape(any(), any(), any(), any());
            verify(scrapingLogic, times(0)).scrape(any(), any(), any(), any());
            verify(scrapingLogic, times(0)).scrape(any(), any(), any(), any());
        }

        @DisplayName("scrape: 対象documentIdに対して処理することを確認する")
        @Test
        void scrape_documentId_ok() {
            var documentId = "id";

            when(documentDao.selectByDocumentId("id")).thenReturn(
                    DocumentEntity.builder()
                            .documentId(documentId)
                            .edinetCode("target")
                            .submitDate(LocalDate.parse("2020-09-22"))
                            .downloaded("0")
                            .decoded("0")
                            .scrapedBs("0")
                            .scrapedPl("0")
                            .scrapedNumberOfShares("0")
                            .removed("0")
                            .build()
            ).thenReturn(
                    DocumentEntity.builder()
                            .documentId(documentId)
                            .edinetCode("target")
                            .submitDate(LocalDate.parse("2020-09-22"))
                            .downloaded("1")
                            .decoded("1")
                            .scrapedBs("0")
                            .scrapedPl("0")
                            .scrapedNumberOfShares("0")
                            .removed("0")
                            .build()
            );

            assertDoesNotThrow(() -> service.scrape(documentId));

            verify(scrapingLogic, times(1)).download(documentId, LocalDate.parse("2020-09-22"));
            verify(scrapingLogic, times(1)).scrape(
                    eq(FinancialStatementEnum.BALANCE_SHEET),
                    eq("id"),
                    eq(LocalDate.parse("2020-09-22")),
                    any());
            verify(scrapingLogic, times(1)).scrape(
                    eq(FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT),
                    eq("id"),
                    eq(LocalDate.parse("2020-09-22")),
                    any());
            verify(scrapingLogic, times(1)).scrape(
                    eq(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES),
                    eq("id"),
                    eq(LocalDate.parse("2020-09-22")),
                    eq(null));
        }

        @DisplayName("scrape: 対象documentIdが処理済みのときはなにもしない")
        @Test
        void scrape_documentId_nothing() {
            var documentId = "id";
            var documentTargeted = DocumentEntity.builder()
                    .documentId(documentId)
                    .edinetCode("target")
                    .submitDate(LocalDate.parse("2020-09-22"))
                    .downloaded(DocumentStatus.DONE.toValue())
                    .decoded(DocumentStatus.DONE.toValue())
                    .scrapedBs(DocumentStatus.DONE.toValue())
                    .scrapedPl(DocumentStatus.DONE.toValue())
                    .scrapedNumberOfShares(DocumentStatus.DONE.toValue())
                    .removed("0")
                    .build();

            when(documentDao.selectByDocumentId("id")).thenReturn(documentTargeted);

            assertDoesNotThrow(() -> service.scrape(documentId));

            verify(scrapingLogic, times(0)).download(any(), any());
            verify(scrapingLogic, times(0)).scrape(
                    eq(FinancialStatementEnum.BALANCE_SHEET), any(), any(), any());
            verify(scrapingLogic, times(0)).scrape(
                    eq(FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT), any(), any(), any());
            verify(scrapingLogic, times(0)).scrape(
                    eq(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES), any(), any(), any());
        }
    }

    @Nested
    class resetForRetry {

        @Test
        void resetForRetry_ok() {
            var targetTypes = List.of(DocumentTypeCode.DTC_120);
            var updatedAt = LocalDateTime.of(2020, 9, 22, 13, 9);

            when(documentDao.selectByDocumentTypeCode(List.of("120"))).thenReturn(List.of(DocumentEntity.builder()
                    .documentId("id")
                    .scrapedBs("9")
                    .scrapedPl("9")
                    .scrapedNumberOfShares("1")
                    .removed("0")
                    .build()));
            when(service.nowLocalDateTime()).thenReturn(updatedAt);

            assertDoesNotThrow(() -> service.resetForRetry(targetTypes));

            verify(documentDao, times(1)).update(DocumentEntity.builder()
                    .documentId("id")
                    .scrapedBs(DocumentStatus.NOT_YET.toValue())
                    .updatedAt(updatedAt)
                    .build());
            verify(documentDao, times(1)).update(DocumentEntity.builder()
                    .documentId("id")
                    .scrapedPl(DocumentStatus.NOT_YET.toValue())
                    .updatedAt(updatedAt)
                    .build());
        }
    }

    @Nested
    class removeDocument {

        @DisplayName("removeDocument : 指定書類IDを処理対象外にする")
        @Test
        void removeDocument_ok() {
            var documentId = "documentId";
            assertDoesNotThrow(() -> service.removeDocument(documentId));
        }
    }
}