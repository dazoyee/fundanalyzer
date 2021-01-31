package github.com.ioridazo.fundanalyzer.domain.scraping;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.ScrapingKeywordDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BsEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.ScrapingKeyword;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocument;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatement;
import github.com.ioridazo.fundanalyzer.domain.file.FileOperator;
import github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.XbrlScraping;
import github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.bean.FinancialTableResultBean;
import github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.bean.Unit;
import github.com.ioridazo.fundanalyzer.proxy.edinet.EdinetProxy;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request.AcquisitionType;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.data.util.Pair;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScrapingLogicTest {

    private String pathEdinet;
    private String pathDecode;
    private EdinetProxy proxy;
    private FileOperator fileOperator;
    private XbrlScraping xbrlScraping;
    private CompanyDao companyDao;
    private DocumentDao documentDao;
    private EdinetDocumentDao edinetDocumentDao;
    private BsSubjectDao bsSubjectDao;
    private FinancialStatementDao financialStatementDao;
    private ScrapingKeywordDao scrapingKeywordDao;

    private ScrapingLogic scrapingLogic;

    @BeforeEach
    void before() {
        this.pathEdinet = "C:/test/edinet";
        this.pathDecode = "C:/test/decode";
        this.proxy = Mockito.mock(EdinetProxy.class);
        this.fileOperator = Mockito.mock(FileOperator.class);
        this.xbrlScraping = Mockito.mock(XbrlScraping.class);
        this.companyDao = Mockito.mock(CompanyDao.class);
        this.documentDao = Mockito.mock(DocumentDao.class);
        this.edinetDocumentDao = Mockito.mock(EdinetDocumentDao.class);
        this.bsSubjectDao = Mockito.mock(BsSubjectDao.class);
        this.financialStatementDao = Mockito.mock(FinancialStatementDao.class);
        this.scrapingKeywordDao = Mockito.mock(ScrapingKeywordDao.class);

        this.scrapingLogic = Mockito.spy(new ScrapingLogic(
                pathEdinet,
                pathDecode,
                proxy,
                fileOperator,
                xbrlScraping,
                companyDao,
                documentDao,
                edinetDocumentDao,
                bsSubjectDao,
                financialStatementDao,
                scrapingKeywordDao
        ));
    }

    @Nested
    class download {

        @DisplayName("download : 書類がダウンロードされステータス更新されることを確認する")
        @Test
        void download_ok() throws IOException {
            var docId = "docId";
            var targetDate = LocalDate.parse("2020-09-26");
            var updated = LocalDateTime.of(2020, 9, 26, 12, 18);

            when(scrapingLogic.nowLocalDateTime()).thenReturn(updated);

            assertDoesNotThrow(() -> scrapingLogic.download(docId, targetDate));

            verify(proxy, times(1)).acquisition(
                    new File(pathEdinet + "/" + targetDate.getYear() + "/" + targetDate.getMonth() + "/" + targetDate),
                    new AcquisitionRequestParameter(docId, AcquisitionType.DEFAULT)
            );
            verify(documentDao, times(1)).update(Document.builder()
                    .documentId(docId)
                    .downloaded(DocumentStatus.DONE.toValue())
                    .updatedAt(updated)
                    .build());
            verify(fileOperator, times(1)).decodeZipFile(
                    new File(pathEdinet + "/" + targetDate.getYear() + "/" + targetDate.getMonth() + "/" + targetDate + "/" + docId),
                    new File(pathDecode + "/" + targetDate.getYear() + "/" + targetDate.getMonth() + "/" + targetDate + "/" + docId)
            );
            verify(documentDao, times(1)).update(Document.builder()
                    .documentId(docId)
                    .decoded(DocumentStatus.DONE.toValue())
                    .updatedAt(updated)
                    .build());
        }

        @DisplayName("download : 書類のダウンロードが失敗したときにステータス更新されることを確認する")
        @Test
        void download_FundanalyzerRestClientException() {
            var docId = "docId";
            var targetDate = LocalDate.parse("2020-09-26");
            var updated = LocalDateTime.of(2020, 9, 26, 12, 18);

            doThrow(FundanalyzerRestClientException.class).when(proxy).acquisition(
                    new File(pathEdinet + "/" + targetDate.getYear() + "/" + targetDate.getMonth() + "/" + targetDate),
                    new AcquisitionRequestParameter(docId, AcquisitionType.DEFAULT)
            );
            when(scrapingLogic.nowLocalDateTime()).thenReturn(updated);

            assertDoesNotThrow(() -> scrapingLogic.download(docId, targetDate));

            verify(documentDao, times(1)).update(Document.builder()
                    .documentId(docId)
                    .downloaded(DocumentStatus.ERROR.toValue())
                    .updatedAt(updated)
                    .build());
        }

        @DisplayName("download : 書類のzip解凍が失敗したときにステータス更新されることを確認する")
        @Test
        void download_IOException() throws IOException {
            var docId = "docId";
            var targetDate = LocalDate.parse("2020-09-26");
            var updated = LocalDateTime.of(2020, 9, 26, 12, 18);

            doThrow(IOException.class).when(fileOperator).decodeZipFile(
                    new File(pathEdinet + "/" + targetDate.getYear() + "/" + targetDate.getMonth() + "/" + targetDate + "/" + docId),
                    new File(pathDecode + "/" + targetDate.getYear() + "/" + targetDate.getMonth() + "/" + targetDate + "/" + docId)
            );
            when(scrapingLogic.nowLocalDateTime()).thenReturn(updated);

            assertDoesNotThrow(() -> scrapingLogic.download(docId, targetDate));

            verify(documentDao, times(1)).update(Document.builder()
                    .documentId(docId)
                    .decoded(DocumentStatus.ERROR.toValue())
                    .updatedAt(updated)
                    .build());
        }
    }

    @Nested
    class scrape {

        @DisplayName("scrape : 貸借対照表のスクレイピング処理が行われ、ステータス更新することを確認する")
        @Test
        void scrape_ok_BALANCE_SHEET() throws FundanalyzerFileException {
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var documentId = "documentId";
            var date = LocalDate.parse("2020-09-26");
            var edinetDocument = new EdinetDocument();
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var file = new File("");
            var scrapingKeyword = new ScrapingKeyword(null, null, null, null, null);
            var targetFile = Pair.of(file, scrapingKeyword);
            var updated = LocalDateTime.of(2020, 9, 26, 12, 18);

            when(edinetDocumentDao.selectByDocId(documentId)).thenReturn(edinetDocument);
            when(companyDao.selectByEdinetCode(edinetDocument.getEdinetCode().orElse(null))).thenReturn(Optional.of(company));
            doReturn(true).when(scrapingLogic).beforeCheck(eq(company), eq(fs), any());
            doReturn(targetFile).when(scrapingLogic).findTargetFile(
                    new File(pathDecode + "/" + date.getYear() + "/" + date.getMonth() + "/" + date + "/" + documentId + "/XBRL/PublicDoc"),
                    fs
            );
            doNothing().when(scrapingLogic).insertFinancialStatement(
                    eq(targetFile.getFirst()),
                    eq(targetFile.getSecond()),
                    eq(fs),
                    eq(company),
                    any(),
                    eq(edinetDocument)
            );
            doNothing().when(scrapingLogic).checkBs(company, edinetDocument);
            when(scrapingLogic.nowLocalDateTime()).thenReturn(updated);

            assertDoesNotThrow(() -> scrapingLogic.scrape(fs, documentId, date, null));

            verify(scrapingLogic, times(1)).insertFinancialStatement(
                    eq(targetFile.getFirst()),
                    eq(targetFile.getSecond()),
                    eq(fs),
                    eq(company),
                    any(),
                    eq(edinetDocument)
            );
            verify(scrapingLogic, times(1)).checkBs(company, edinetDocument);
            verify(documentDao, times(1)).update(Document.ofUpdated(
                    fs,
                    documentId,
                    DocumentStatus.DONE,
                    targetFile.getFirst().getPath(),
                    updated
            ));
        }

        @DisplayName("scrape : 損益計算書のスクレイピング処理が行われ、ステータス更新することを確認する")
        @Test
        void scrape_ok_PROFIT_AND_LESS_STATEMENT() throws FundanalyzerFileException {
            var fs = FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT;
            var documentId = "documentId";
            var date = LocalDate.parse("2020-09-26");
            var edinetDocument = new EdinetDocument();
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var file = new File("");
            var scrapingKeyword = new ScrapingKeyword(null, null, null, null, null);
            var targetFile = Pair.of(file, scrapingKeyword);
            var updated = LocalDateTime.of(2020, 9, 26, 12, 18);

            when(edinetDocumentDao.selectByDocId(documentId)).thenReturn(edinetDocument);
            when(companyDao.selectByEdinetCode(edinetDocument.getEdinetCode().orElse(null))).thenReturn(Optional.of(company));
            doReturn(true).when(scrapingLogic).beforeCheck(eq(company), eq(fs), any());
            doReturn(targetFile).when(scrapingLogic).findTargetFile(
                    new File(pathDecode + "/" + date.getYear() + "/" + date.getMonth() + "/" + date + "/" + documentId + "/XBRL/PublicDoc"),
                    fs
            );
            doNothing().when(scrapingLogic).insertFinancialStatement(
                    eq(targetFile.getFirst()),
                    eq(targetFile.getSecond()),
                    eq(fs),
                    eq(company),
                    any(),
                    eq(edinetDocument)
            );
            when(scrapingLogic.nowLocalDateTime()).thenReturn(updated);

            assertDoesNotThrow(() -> scrapingLogic.scrape(fs, documentId, date, null));

            verify(scrapingLogic, times(1)).insertFinancialStatement(
                    eq(targetFile.getFirst()),
                    eq(targetFile.getSecond()),
                    eq(fs),
                    eq(company),
                    any(),
                    eq(edinetDocument)
            );
            verify(scrapingLogic, times(0)).checkBs(company, edinetDocument);
            verify(documentDao, times(1)).update(Document.ofUpdated(
                    fs,
                    documentId,
                    DocumentStatus.DONE,
                    targetFile.getFirst().getPath(),
                    updated
            ));
        }

        @DisplayName("scrape : 株式総数のスクレイピング処理が行われ、ステータス更新することを確認する")
        @Test
        void scrape_ok_TOTAL_NUMBER_OF_SHARES() throws FundanalyzerFileException {
            var fs = FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES;
            var documentId = "documentId";
            var date = LocalDate.parse("2020-09-26");
            var edinetDocument = new EdinetDocument();
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var file = new File("");
            var scrapingKeyword = new ScrapingKeyword(null, null, "keyword", null, null);
            var targetFile = Pair.of(file, scrapingKeyword);
            var updated = LocalDateTime.of(2020, 9, 26, 12, 18);

            when(edinetDocumentDao.selectByDocId(documentId)).thenReturn(edinetDocument);
            when(companyDao.selectByEdinetCode(edinetDocument.getEdinetCode().orElse(null))).thenReturn(Optional.of(company));
            doReturn(true).when(scrapingLogic).beforeCheck(eq(company), eq(fs), any());
            doReturn(targetFile).when(scrapingLogic).findTargetFile(
                    new File(pathDecode + "/" + date.getYear() + "/" + date.getMonth() + "/" + date + "/" + documentId + "/XBRL/PublicDoc"),
                    fs
            );
            when(xbrlScraping.scrapeNumberOfShares(targetFile.getFirst(), targetFile.getSecond().getKeyword())).thenReturn("100");
            doNothing().when(scrapingLogic).insertFinancialStatement(
                    eq(company),
                    eq(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES),
                    eq("0"),
                    eq(edinetDocument),
                    any()
            );
            when(scrapingLogic.nowLocalDateTime()).thenReturn(updated);

            assertDoesNotThrow(() -> scrapingLogic.scrape(fs, documentId, date, null));

            verify(scrapingLogic, times(1)).insertFinancialStatement(
                    eq(company),
                    eq(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES),
                    eq("0"),
                    eq(edinetDocument),
                    any()
            );
            verify(scrapingLogic, times(0)).checkBs(company, edinetDocument);
            verify(documentDao, times(1)).update(Document.ofUpdated(
                    fs,
                    documentId,
                    DocumentStatus.DONE,
                    targetFile.getFirst().getPath(),
                    updated
            ));
        }

        @DisplayName("scrape : 財務諸表登録年（period_endの年）が重複していたら処理しないことを確認する")
        @Test
        void scrape_beforeCheck() throws FundanalyzerFileException {
            var fs = FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES;
            var documentId = "documentId";
            var date = LocalDate.parse("2020-09-26");
            var edinetDocument = new EdinetDocument();
            edinetDocument.setPeriodEnd("2020-09-30");
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            when(edinetDocumentDao.selectByDocId(documentId)).thenReturn(edinetDocument);
            when(companyDao.selectByEdinetCode(edinetDocument.getEdinetCode().orElse(null))).thenReturn(Optional.of(company));
            doReturn(false).when(scrapingLogic).beforeCheck(eq(company), eq(fs), any());

            assertDoesNotThrow(() -> scrapingLogic.scrape(fs, documentId, date, null));

            verify(scrapingLogic, times(0))
                    .insertFinancialStatement(any(), any(), any(), any(), any(), any());
            verify(scrapingLogic, times(0)).checkBs(company, edinetDocument);
            verify(documentDao, times(1)).update(any());
        }

        @DisplayName("scrape : スクレイピング処理時にエラー発生したときの処理を確認する")
        @Test
        void scrape_FundanalyzerFileException() throws FundanalyzerFileException {
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var documentId = "documentId";
            var date = LocalDate.parse("2020-09-26");
            var edinetDocument = new EdinetDocument();
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var updated = LocalDateTime.of(2020, 9, 26, 12, 18);

            when(edinetDocumentDao.selectByDocId(documentId)).thenReturn(edinetDocument);
            when(companyDao.selectByEdinetCode(edinetDocument.getEdinetCode().orElse(null))).thenReturn(Optional.of(company));
            doReturn(true).when(scrapingLogic).beforeCheck(eq(company), eq(fs), any());
            doThrow(FundanalyzerFileException.class).when(scrapingLogic).findTargetFile(
                    new File(pathDecode + "/" + date.getYear() + "/" + date.getMonth() + "/" + date + "/" + documentId + "/XBRL/PublicDoc"),
                    fs
            );
            when(scrapingLogic.nowLocalDateTime()).thenReturn(updated);

            assertDoesNotThrow(() -> scrapingLogic.scrape(fs, documentId, date, null));

            verify(scrapingLogic, times(0))
                    .insertFinancialStatement(any(), any(), any(), any(), any(), any());
            verify(scrapingLogic, times(0)).checkBs(company, edinetDocument);
            verify(documentDao, times(1)).update(Document.ofUpdated(
                    fs,
                    documentId,
                    DocumentStatus.ERROR,
                    null,
                    updated
            ));
        }

        @DisplayName("beforeCheck : DBに対象のデータがなければtrueとなることを確認する")
        @Test
        void beforeCheck_true_isEmpty() {
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var edinetDocument = new EdinetDocument();
            edinetDocument.setPeriodEnd("2020-09-30");

            when(financialStatementDao.selectByEdinetCodeAndFsAndYear(company.getEdinetCode(), fs.toValue(), "2020"))
                    .thenReturn(List.of());

            assertTrue(() -> scrapingLogic.beforeCheck(company, fs, edinetDocument));
        }

        @DisplayName("beforeCheck : 対象年が重複していなければtrueとなることを確認する")
        @Test
        void beforeCheck_true_none() {
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var edinetDocument = new EdinetDocument();
            edinetDocument.setPeriodEnd("2020-09-30");
            var financialStatement = new FinancialStatement(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDate.parse("2019-09-30"),
                    null,
                    null
            );

            when(financialStatementDao.selectByEdinetCodeAndFsAndYear(company.getEdinetCode(), fs.toValue(), "2020"))
                    .thenReturn(List.of(financialStatement));

            assertTrue(() -> scrapingLogic.beforeCheck(company, fs, edinetDocument));
        }

        @DisplayName("beforeCheck : 対象年が重複していなければtrueとなることを確認する")
        @Test
        void beforeCheck_false_none() {
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var edinetDocument = new EdinetDocument();
            edinetDocument.setPeriodEnd("2020-09-30");
            var financialStatement = new FinancialStatement(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDate.parse("2020-03-30"),
                    null,
                    null
            );

            when(financialStatementDao.selectByEdinetCodeAndFsAndYear(company.getEdinetCode(), fs.toValue(), "2020"))
                    .thenReturn(List.of(financialStatement));

            assertFalse(() -> scrapingLogic.beforeCheck(company, fs, edinetDocument));
        }

        @DisplayName("findTargetFile : 対象のファイルとスクレイピングキーワードを見つけることを確認する")
        @Test
        void findTargetFile_ok() throws FundanalyzerFileException {
            var targetFile = new File("");
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var scrapingKeyword = new ScrapingKeyword(
                    null,
                    null,
                    "keyword",
                    "remarks",
                    null
            );

            when(scrapingKeywordDao.selectByFinancialStatementId(fs.toValue())).thenReturn(List.of(scrapingKeyword));
            when(xbrlScraping.findFile(targetFile, scrapingKeyword)).thenReturn(Optional.of(new File("actual")));

            var actual = scrapingLogic.findTargetFile(targetFile, fs);

            assertEquals(new File("actual"), actual.getFirst());
            assertEquals(scrapingKeyword, actual.getSecond());
        }

        @DisplayName("findTargetFile : 対象のファイルが存在しなかったらエラー発生することを確認する")
        @Test
        void findTargetFile_FundanalyzerFileException() throws FundanalyzerFileException {
            var targetFile = new File("");
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var scrapingKeyword = new ScrapingKeyword(
                    null,
                    null,
                    "keyword",
                    "remarks",
                    null
            );

            when(scrapingKeywordDao.selectByFinancialStatementId(fs.toValue())).thenReturn(List.of(scrapingKeyword));
            when(xbrlScraping.findFile(targetFile, scrapingKeyword)).thenThrow(FundanalyzerFileException.class);

            assertThrows(FundanalyzerFileException.class, () -> scrapingLogic.findTargetFile(targetFile, fs));
        }

        @DisplayName("insertFinancialStatement : スクレイピング結果とマスタから一致するものをフィルターにかけ、" +
                "一致するものが存在したらDBに登録することを確認する")
        @Test
        void insertFinancialStatement_insert_foreach() throws FundanalyzerFileException {
            var targetFile = new File("");
            var scrapingKeyword = new ScrapingKeyword(
                    null,
                    null,
                    "keyword",
                    "remarks",
                    null
            );
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var detailList = List.of(new BsSubject(
                    "1",
                    null,
                    null,
                    "科目"
            ));
            var edinetDocument = new EdinetDocument();
            edinetDocument.setPeriodStart("2020-01-01");
            edinetDocument.setPeriodEnd("2020-12-31");
            var resultBean = FinancialTableResultBean.of("科目", null, "1", Unit.MILLIONS_OF_YEN);
            var createdAt = LocalDateTime.of(2020, 9, 26, 12, 18);

            when(xbrlScraping.scrapeFinancialStatement(targetFile, scrapingKeyword.getKeyword()))
                    .thenReturn(List.of(resultBean));
            when(scrapingLogic.nowLocalDateTime()).thenReturn(createdAt);

            assertDoesNotThrow(() -> scrapingLogic.insertFinancialStatement(targetFile, scrapingKeyword, fs, company, detailList, edinetDocument));

            verify(financialStatementDao, times(1)).insert(new FinancialStatement(
                    null,
                    company.getCode().orElse(null),
                    company.getEdinetCode(),
                    fs.toValue(),
                    "1",
                    LocalDate.parse("2020-01-01"),
                    LocalDate.parse("2020-12-31"),
                    1000000L,
                    createdAt
            ));
        }

        @DisplayName("insertFinancialStatement : スクレイピング結果とマスタから一致するものをフィルターにかけ、" +
                "一致するものが存在しなかったらDBに登録しないことを確認する")
        @Test
        void insertFinancialStatement_not_insert() throws FundanalyzerFileException {
            var targetFile = new File("");
            var scrapingKeyword = new ScrapingKeyword(
                    null,
                    null,
                    "keyword",
                    "remarks",
                    null
            );
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var detailList = List.of(new BsSubject(
                    "1",
                    null,
                    null,
                    "科目"
            ));
            var edinetDocument = new EdinetDocument();
            edinetDocument.setPeriodStart("2020-01-01");
            edinetDocument.setPeriodEnd("2020-12-31");
            var resultBean = FinancialTableResultBean.of("一致しない科目", null, "1", Unit.MILLIONS_OF_YEN);
            var createdAt = LocalDateTime.of(2020, 9, 26, 12, 18);


            when(xbrlScraping.scrapeFinancialStatement(targetFile, scrapingKeyword.getKeyword()))
                    .thenReturn(List.of(resultBean));
            when(scrapingLogic.nowLocalDateTime()).thenReturn(createdAt);

            assertDoesNotThrow(() -> scrapingLogic.insertFinancialStatement(targetFile, scrapingKeyword, fs, company, detailList, edinetDocument));

            verify(financialStatementDao, times(0)).insert(new FinancialStatement(
                    null,
                    company.getCode().orElse(null),
                    company.getEdinetCode(),
                    fs.toValue(),
                    "1",
                    LocalDate.parse("2020-01-01"),
                    LocalDate.parse("2020-12-31"),
                    1000000L,
                    createdAt
            ));
        }

        @DisplayName("insertFinancialStatement : DB登録することを確認する")
        @Test
        void insertFinancialStatement_insert() {
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var dId = "0";
            var edinetDocument = new EdinetDocument();
            edinetDocument.setPeriodStart("2020-01-01");
            edinetDocument.setPeriodEnd("2020-12-31");
            var value = 1000L;
            var createdAt = LocalDateTime.of(2020, 9, 26, 12, 18);


            when(scrapingLogic.nowLocalDateTime()).thenReturn(createdAt);

            assertDoesNotThrow(() -> scrapingLogic.insertFinancialStatement(company, fs, dId, edinetDocument, value));

            verify(financialStatementDao, times(1)).insert(new FinancialStatement(
                    null,
                    company.getCode().orElse(null),
                    company.getEdinetCode(),
                    fs.toValue(),
                    "0",
                    LocalDate.parse("2020-01-01"),
                    LocalDate.parse("2020-12-31"),
                    1000L,
                    createdAt
            ));
        }

        @Disabled("catchできない")
        @DisplayName("insertFinancialStatement : 一意制約違反のときはDB登録しないことを確認する")
        @Test
        void insertFinancialStatement_UniqueConstraintException() {
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var dId = "0";
            var edinetDocument = new EdinetDocument();
            edinetDocument.setPeriodStart("2020-01-01");
            edinetDocument.setPeriodEnd("2020-12-31");
            var value = 1000L;
            var createdAt = LocalDateTime.of(2020, 9, 26, 12, 18);

            when(financialStatementDao.insert(any())).thenThrow(UniqueConstraintException.class);
            when(scrapingLogic.nowLocalDateTime()).thenReturn(createdAt);

            assertDoesNotThrow(() -> scrapingLogic.insertFinancialStatement(company, fs, dId, edinetDocument, value));

            verify(financialStatementDao, times(0)).insert(new FinancialStatement(
                    null,
                    company.getCode().orElse(null),
                    company.getEdinetCode(),
                    fs.toValue(),
                    "0",
                    LocalDate.parse("2020-01-01"),
                    LocalDate.parse("2020-12-31"),
                    1000L,
                    createdAt
            ));
        }

        @DisplayName("insertFinancialStatement : throwされることを確認する")
        @Test
        void insertFinancialStatement_Exception() {
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var dId = "0";
            var edinetDocument = new EdinetDocument();
            edinetDocument.setPeriodStart("2020-01-01");
            edinetDocument.setPeriodEnd("2020-12-31");
            var value = 1000L;

            when(financialStatementDao.insert(any())).thenThrow(RuntimeException.class);

            assertThrows(RuntimeException.class, () -> scrapingLogic.insertFinancialStatement(company, fs, dId, edinetDocument, value));
        }

        @DisplayName("checkBs : 既に登録した流動負債合計と負債合計の金額が一致していたら、固定負債合計に0としてDBに登録することを確認する")
        @Test
        void checkBs_insert() {
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var edinetDocument = new EdinetDocument();
            var totalCurrentLiabilities = new BsSubject("1", null, null, null);
            var fsTotalCurrentLiabilities = new FinancialStatement(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1000L,
                    null
            );
            var totalLiabilities = new BsSubject("2", null, null, null);
            var fsTotalLiabilities = new FinancialStatement(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1000L,
                    null
            );

            when(bsSubjectDao.selectByOutlineSubjectId(BsEnum.TOTAL_CURRENT_LIABILITIES.getOutlineSubjectId()))
                    .thenReturn(List.of(totalCurrentLiabilities));
            when(financialStatementDao.selectByUniqueKey(
                    edinetDocument.getEdinetCode().orElse(null),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    totalCurrentLiabilities.getId(),
                    edinetDocument.getPeriodEnd().map(d -> d.substring(0, 4)).orElse(null)
            )).thenReturn(Optional.of(fsTotalCurrentLiabilities));
            when(bsSubjectDao.selectByOutlineSubjectId(BsEnum.TOTAL_LIABILITIES.getOutlineSubjectId()))
                    .thenReturn(List.of(totalLiabilities));
            when(financialStatementDao.selectByUniqueKey(
                    edinetDocument.getEdinetCode().orElse(null),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    totalLiabilities.getId(),
                    edinetDocument.getPeriodEnd().map(d -> d.substring(0, 4)).orElse(null)
            )).thenReturn(Optional.of(fsTotalLiabilities));
            doNothing().when(scrapingLogic).insertFinancialStatement(eq(company), any(), any(), eq(edinetDocument), eq(0L));
            when(bsSubjectDao.selectByUniqueKey(any(), any())).thenReturn(new BsSubject("1", null, null, null));

            assertDoesNotThrow(() -> scrapingLogic.checkBs(company, edinetDocument));

            verify(scrapingLogic, times(1)).insertFinancialStatement(eq(company), any(), any(), eq(edinetDocument), eq(0L));
        }

        @DisplayName("checkBs : 既に登録した流動負債合計と負債合計の金額が一致していなかったら、DBに登録しないことを確認する")
        @Test
        void checkBs_not_insert() {
            var company = new Company(
                    "code",
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var edinetDocument = new EdinetDocument();
            var totalCurrentLiabilities = new BsSubject("1", null, null, null);
            var fsTotalCurrentLiabilities = new FinancialStatement(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    2000L,
                    null
            );
            var totalLiabilities = new BsSubject("2", null, null, null);
            var fsTotalLiabilities = new FinancialStatement(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    3000L,
                    null
            );

            when(bsSubjectDao.selectByOutlineSubjectId(BsEnum.TOTAL_CURRENT_LIABILITIES.getOutlineSubjectId()))
                    .thenReturn(List.of(totalCurrentLiabilities));
            when(financialStatementDao.selectByUniqueKey(
                    edinetDocument.getEdinetCode().orElse(null),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    totalCurrentLiabilities.getId(),
                    edinetDocument.getPeriodEnd().map(d -> d.substring(0, 4)).orElse(null)
            )).thenReturn(Optional.of(fsTotalCurrentLiabilities));
            when(bsSubjectDao.selectByOutlineSubjectId(BsEnum.TOTAL_LIABILITIES.getOutlineSubjectId()))
                    .thenReturn(List.of(totalLiabilities));
            when(financialStatementDao.selectByUniqueKey(
                    edinetDocument.getEdinetCode().orElse(null),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    totalLiabilities.getId(),
                    edinetDocument.getPeriodEnd().map(d -> d.substring(0, 4)).orElse(null)
            )).thenReturn(Optional.of(fsTotalLiabilities));
            when(bsSubjectDao.selectByUniqueKey(any(), any())).thenReturn(new BsSubject("1", null, null, null));

            assertDoesNotThrow(() -> scrapingLogic.checkBs(company, edinetDocument));

            verify(scrapingLogic, times(0)).insertFinancialStatement(eq(company), any(), any(), eq(edinetDocument), eq(0L));
        }
    }
}