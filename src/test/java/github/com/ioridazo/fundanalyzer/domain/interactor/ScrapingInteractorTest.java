package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.edinet.EdinetClient;
import github.com.ioridazo.fundanalyzer.client.file.FileOperator;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.ScrapingKeywordDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.ScrapingKeywordEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.CreatedType;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.XbrlScraping;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean.FinancialTableResultBean;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean.Unit;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.SubjectSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.PlSubject;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScrapingInteractorTest {

    private ScrapingKeywordDao scrapingKeywordDao;
    private CompanySpecification companySpecification;
    private SubjectSpecification subjectSpecification;
    private DocumentSpecification documentSpecification;
    private FinancialStatementSpecification financialStatementSpecification;
    private XbrlScraping xbrlScraping;
    private FileOperator fileOperator;
    private EdinetClient edinetClient;

    private ScrapingInteractor scrapingInteractor;

    @BeforeEach
    void setUp() {
        scrapingKeywordDao = Mockito.mock(ScrapingKeywordDao.class);
        companySpecification = Mockito.mock(CompanySpecification.class);
        subjectSpecification = Mockito.mock(SubjectSpecification.class);
        documentSpecification = Mockito.mock(DocumentSpecification.class);
        financialStatementSpecification = Mockito.mock(FinancialStatementSpecification.class);
        xbrlScraping = Mockito.mock(XbrlScraping.class);
        fileOperator = Mockito.mock(FileOperator.class);
        edinetClient = Mockito.mock(EdinetClient.class);

        scrapingInteractor = Mockito.spy(new ScrapingInteractor(
                scrapingKeywordDao,
                companySpecification,
                subjectSpecification,
                documentSpecification,
                financialStatementSpecification,
                xbrlScraping,
                fileOperator,
                edinetClient
        ));
        scrapingInteractor.pathEdinet = "pathEdinet";
        scrapingInteractor.pathDecode = "pathDecode";
    }

    @Nested
    class download {

        Document document = defaultDocument(DocumentTypeCode.DTC_120);

        @DisplayName("download : ファイル取得・解凍する")
        @Test
        void execute() throws IOException {
            assertDoesNotThrow(() -> scrapingInteractor.download(document));
            verify(edinetClient, times(1)).acquisition(any(), any());
            verify(documentSpecification, times(1)).updateDownloadToDone(document);
            verify(fileOperator, times(1)).decodeZipFile(any(), any());
            verify(documentSpecification, times(1)).updateDecodeToDone(document);
        }

        @DisplayName("download : ダウンロード処理に失敗したとき")
        @Test
        void fundanalyzerRestClientException() throws IOException {
            doThrow(new FundanalyzerRestClientException("")).when(edinetClient).acquisition(any(), any());
            assertDoesNotThrow(() -> scrapingInteractor.download(document));
            verify(edinetClient, times(1)).acquisition(any(), any());
            verify(documentSpecification, times(1)).updateDownloadToError(document);
            verify(documentSpecification, times(0)).updateDownloadToDone(document);
            verify(fileOperator, times(0)).decodeZipFile(any(), any());
            verify(documentSpecification, times(0)).updateDecodeToDone(document);
        }

        @DisplayName("download : zipファイルの解凍処理に失敗したとき")
        @Test
        void iOException() throws IOException {
            doThrow(new IOException()).when(fileOperator).decodeZipFile(any(), any());
            assertDoesNotThrow(() -> scrapingInteractor.download(document));
            verify(edinetClient, times(1)).acquisition(any(), any());
            verify(documentSpecification, times(0)).updateDownloadToError(document);
            verify(documentSpecification, times(1)).updateDownloadToDone(document);
            verify(fileOperator, times(1)).decodeZipFile(any(), any());
            verify(documentSpecification, times(0)).updateDecodeToDone(document);
            verify(documentSpecification, times(1)).updateDecodeToError(document);
        }
    }

    @Nested
    class bs {

        Document document = defaultDocument(DocumentTypeCode.DTC_120);
        Document documentOfDTC140 = defaultDocument(DocumentTypeCode.DTC_140);
        Company company = defaultCompany();

        File file = new File("file");
        ScrapingKeywordEntity scrapingKeyword = new ScrapingKeywordEntity(null, null, "keyword", null, null, null);

        @BeforeEach
        void setUp() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            doReturn(Pair.of(file, scrapingKeyword)).when(scrapingInteractor).findTargetFile(any(), eq(FinancialStatementEnum.BALANCE_SHEET), eq(document));
        }

        @DisplayName("bs : 貸借対照表をスクレイピングする")
        @Test
        void insert() {
            var bsSubject = new BsSubject("id", null, null, null);
            var resultBean = FinancialTableResultBean.of("subject", null, "1", Unit.THOUSANDS_OF_YEN);

            when(xbrlScraping.scrapeFinancialStatement(file, "keyword")).thenReturn(List.of(resultBean));
            when(subjectSpecification.findBsSubject("subject")).thenReturn(Optional.of(bsSubject));
            doNothing().when(scrapingInteractor).doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document);

            assertDoesNotThrow(() -> scrapingInteractor.bs(document));
            verify(financialStatementSpecification, times(1))
                    .insert(company, FinancialStatementEnum.BALANCE_SHEET, "id", document, 1000L, CreatedType.AUTO);
            verify(scrapingInteractor, times(1)).doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document);
            verify(documentSpecification, times(1)).updateFsToDone(document, FinancialStatementEnum.BALANCE_SHEET, "file");
        }

        @DisplayName("bs : 貸借対照表をスクレイピングする")
        @Test
        void noInsert() {
            var resultBean = FinancialTableResultBean.of("subject", null, "1", Unit.THOUSANDS_OF_YEN);

            when(xbrlScraping.scrapeFinancialStatement(file, "keyword")).thenReturn(List.of(resultBean));
            when(subjectSpecification.findBsSubject("subject")).thenReturn(Optional.empty());
            doNothing().when(scrapingInteractor).doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document);

            assertDoesNotThrow(() -> scrapingInteractor.bs(document));
            verify(financialStatementSpecification, times(0))
                    .insert(company, FinancialStatementEnum.BALANCE_SHEET, "id", document, 1000L, CreatedType.AUTO);
            verify(scrapingInteractor, times(1)).doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document);
            verify(documentSpecification, times(1)).updateFsToDone(document, FinancialStatementEnum.BALANCE_SHEET, "file");
        }

        @DisplayName("bs : キーワードに合致するファイルが存在しないときはエラーにする")
        @Test
        void fundanalyzerFileException() {
            doThrow(new FundanalyzerFileException()).when(scrapingInteractor).findTargetFile(any(), any(), any());

            assertDoesNotThrow(() -> scrapingInteractor.bs(document));
            verify(scrapingInteractor, times(0)).doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document);
            verify(documentSpecification, times(0)).updateFsToDone(document, FinancialStatementEnum.BALANCE_SHEET, "file");
            verify(documentSpecification, times(1)).updateFsToError(document, FinancialStatementEnum.BALANCE_SHEET);
        }

        @DisplayName("bs : 企業情報が存在しないときはエラーにする")
        @Test
        void fundanalyzerRuntimeException() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.empty());
            assertThrows(FundanalyzerRuntimeException.class, () -> scrapingInteractor.bs(document));
        }

        @DisplayName("doBsOptionOfTotalFixedLiabilitiesIfTarget : 既に登録した流動負債合計と負債合計の金額が一致していたら、固定負債合計に0としてDBに登録する")
        @Test
        void doBsOptionOfTotalFixedLiabilitiesIfTarget_insert() {
            var bsSubject1 = new BsSubject("1", null, null, "TOTAL_CURRENT_LIABILITIES");
            var bsSubject2 = new BsSubject("2", null, null, "TOTAL_LIABILITIES");
            var bsSubject3 = new BsSubject("3", null, null, "TOTAL_FIXED_LIABILITIES");

            when(subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_CURRENT_LIABILITIES)).thenReturn(bsSubject1);
            when(financialStatementSpecification.findValue(FinancialStatementEnum.BALANCE_SHEET, document, bsSubject1)).thenReturn(Optional.of(1000L));
            when(subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_LIABILITIES)).thenReturn(bsSubject2);
            when(financialStatementSpecification.findValue(FinancialStatementEnum.BALANCE_SHEET, document, bsSubject2)).thenReturn(Optional.of(1000L));
            when(subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_FIXED_LIABILITIES)).thenReturn(bsSubject3);

            assertDoesNotThrow(() -> scrapingInteractor.doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document));
            verify(financialStatementSpecification, times(1))
                    .insert(company, FinancialStatementEnum.BALANCE_SHEET, "3", document, 0L, CreatedType.AUTO);
        }

        @DisplayName("doBsOptionOfTotalFixedLiabilitiesIfTarget : 既に登録した流動負債合計と負債合計の金額が一致していなかったら、DBに登録しない")
        @Test
        void doBsOptionOfTotalFixedLiabilitiesIfTarget_noInsert() {
            var bsSubject1 = new BsSubject("1", null, null, "TOTAL_CURRENT_LIABILITIES");
            var bsSubject2 = new BsSubject("2", null, null, "TOTAL_LIABILITIES");
            var bsSubject3 = new BsSubject("3", null, null, "TOTAL_FIXED_LIABILITIES");

            when(subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_CURRENT_LIABILITIES)).thenReturn(bsSubject1);
            when(financialStatementSpecification.findValue(FinancialStatementEnum.BALANCE_SHEET, document, bsSubject1)).thenReturn(Optional.of(1000L));
            when(subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_LIABILITIES)).thenReturn(bsSubject2);
            when(financialStatementSpecification.findValue(FinancialStatementEnum.BALANCE_SHEET, document, bsSubject2)).thenReturn(Optional.of(100L));
            when(subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_FIXED_LIABILITIES)).thenReturn(bsSubject3);

            assertDoesNotThrow(() -> scrapingInteractor.doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document));
            verify(financialStatementSpecification, times(0))
                    .insert(company, FinancialStatementEnum.BALANCE_SHEET, "3", document, 0L, CreatedType.AUTO);
        }

        @DisplayName("doBsOptionOfTotalInvestmentsAndOtherAssetsIfTarget : 四半期報告書で投資その他の資産合計が存在しないとき、投資その他の資産合計に0としてDBに登録する")
        @Test
        void doBsOptionOfTotalInvestmentsAndOtherAssetsIfTarget_insert() {
            var bsSubject1 = new BsSubject("1", null, null, "TOTAL_INVESTMENTS_AND_OTHER_ASSETS");

            when(financialStatementSpecification.isPresentTotalInvestmentsAndOtherAssets(documentOfDTC140)).thenReturn(false);
            when(subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS)).thenReturn(bsSubject1);

            assertDoesNotThrow(() -> scrapingInteractor.doBsOptionOfTotalInvestmentsAndOtherAssetsIfTarget(company, documentOfDTC140));
            verify(financialStatementSpecification, times(1))
                    .insert(company, FinancialStatementEnum.BALANCE_SHEET, "1", documentOfDTC140, 0L, CreatedType.AUTO);
        }

        @DisplayName("doBsOptionOfTotalInvestmentsAndOtherAssetsIfTarget : 四半期報告書で投資その他の資産合計が存在したら、DBに登録しない")
        @Test
        void doBsOptionOfTotalInvestmentsAndOtherAssetsIfTarget_noInsert_140() {
            var bsSubject1 = new BsSubject("1", null, null, "TOTAL_INVESTMENTS_AND_OTHER_ASSETS");

            when(financialStatementSpecification.isPresentTotalInvestmentsAndOtherAssets(documentOfDTC140)).thenReturn(true);
            when(subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS)).thenReturn(bsSubject1);

            assertDoesNotThrow(() -> scrapingInteractor.doBsOptionOfTotalInvestmentsAndOtherAssetsIfTarget(company, documentOfDTC140));
            verify(financialStatementSpecification, times(0))
                    .insert(company, FinancialStatementEnum.BALANCE_SHEET, "1", documentOfDTC140, 0L, CreatedType.AUTO);
        }

        @DisplayName("doBsOptionOfTotalInvestmentsAndOtherAssetsIfTarget : 四半期報告書以外のときはなにもしない")
        @Test
        void doBsOptionOfTotalInvestmentsAndOtherAssetsIfTarget_noInsert_120() {
            var bsSubject1 = new BsSubject("1", null, null, "TOTAL_INVESTMENTS_AND_OTHER_ASSETS");

            when(financialStatementSpecification.isPresentTotalInvestmentsAndOtherAssets(document)).thenReturn(true);
            when(subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS)).thenReturn(bsSubject1);

            assertDoesNotThrow(() -> scrapingInteractor.doBsOptionOfTotalInvestmentsAndOtherAssetsIfTarget(company, document));
            verify(financialStatementSpecification, times(0))
                    .insert(company, FinancialStatementEnum.BALANCE_SHEET, "1", document, 0L, CreatedType.AUTO);
        }
    }

    @Nested
    class pl {

        Document document = defaultDocument(DocumentTypeCode.DTC_120);
        Company company = defaultCompany();

        File file = new File("file");
        ScrapingKeywordEntity scrapingKeyword = new ScrapingKeywordEntity(null, null, "keyword", null, null, null);

        @BeforeEach
        void setUp() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            doReturn(Pair.of(file, scrapingKeyword)).when(scrapingInteractor).findTargetFile(any(), eq(FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT), eq(document));
        }

        @DisplayName("pl : 損益計算書をスクレイピングする")
        @Test
        void insert() {
            var plSubject = new PlSubject("id", null, null, null);
            var resultBean = FinancialTableResultBean.of("subject", null, "1", Unit.THOUSANDS_OF_YEN);

            when(xbrlScraping.scrapeFinancialStatement(file, "keyword")).thenReturn(List.of(resultBean));
            when(subjectSpecification.findPlSubject("subject")).thenReturn(Optional.of(plSubject));

            assertDoesNotThrow(() -> scrapingInteractor.pl(document));
            verify(financialStatementSpecification, times(1))
                    .insert(company, FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT, "id", document, 1000L, CreatedType.AUTO);
            verify(scrapingInteractor, times(0)).doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document);
            verify(documentSpecification, times(1)).updateFsToDone(document, FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT, "file");
        }

        @DisplayName("pl : 損益計算書をスクレイピングする")
        @Test
        void noInsert() {
            var resultBean = FinancialTableResultBean.of("subject", null, "1", Unit.THOUSANDS_OF_YEN);

            when(xbrlScraping.scrapeFinancialStatement(file, "keyword")).thenReturn(List.of(resultBean));
            when(subjectSpecification.findPlSubject("subject")).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> scrapingInteractor.pl(document));
            verify(financialStatementSpecification, times(0))
                    .insert(company, FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT, "id", document, 1000L, CreatedType.AUTO);
            verify(scrapingInteractor, times(0)).doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document);
            verify(documentSpecification, times(1)).updateFsToDone(document, FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT, "file");
        }

        @DisplayName("pl : キーワードに合致するファイルが存在しないときはエラーにする")
        @Test
        void fundanalyzerFileException() {
            doThrow(new FundanalyzerFileException()).when(scrapingInteractor).findTargetFile(any(), any(), any());

            assertDoesNotThrow(() -> scrapingInteractor.pl(document));
            verify(scrapingInteractor, times(0)).doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document);
            verify(documentSpecification, times(0)).updateFsToDone(document, FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT, "file");
            verify(documentSpecification, times(1)).updateFsToError(document, FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT);
        }

        @DisplayName("pl : 企業情報が存在しないときはエラーにする")
        @Test
        void fundanalyzerRuntimeException() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.empty());
            assertThrows(FundanalyzerRuntimeException.class, () -> scrapingInteractor.pl(document));
        }
    }

    @Nested
    class ns {

        Document document = defaultDocument(DocumentTypeCode.DTC_120);
        Company company = defaultCompany();

        File file = new File("file");
        ScrapingKeywordEntity scrapingKeyword = new ScrapingKeywordEntity(null, null, "keyword", null, null, null);

        @BeforeEach
        void setUp() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            doReturn(Pair.of(file, scrapingKeyword)).when(scrapingInteractor).findTargetFile(any(), eq(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES), eq(document));
        }

        @DisplayName("ns : 株式総数をスクレイピングする")
        @Test
        void insert() {
            when(xbrlScraping.scrapeNumberOfShares(file, "keyword")).thenReturn("1000");

            assertDoesNotThrow(() -> scrapingInteractor.ns(document));
            verify(financialStatementSpecification, times(1))
                    .insert(company, FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES, "0", document, 1000L, CreatedType.AUTO);
            verify(scrapingInteractor, times(0)).doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document);
            verify(documentSpecification, times(1)).updateFsToDone(document, FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES, "file");
        }

        @DisplayName("ns : スクレイピング結果にスペースが含まれていたとき")
        @Test
        void parseValue() {
            when(xbrlScraping.scrapeNumberOfShares(file, "keyword")).thenReturn("1000　");

            assertDoesNotThrow(() -> scrapingInteractor.ns(document));
            verify(financialStatementSpecification, times(1))
                    .insert(company, FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES, "0", document, 1000L, CreatedType.AUTO);
            verify(scrapingInteractor, times(0)).doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document);
            verify(documentSpecification, times(1)).updateFsToDone(document, FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES, "file");
        }

        @DisplayName("ns : キーワードに合致するファイルが存在しないときはエラーにする")
        @Test
        void fundanalyzerFileException() {
            doThrow(new FundanalyzerFileException()).when(scrapingInteractor).findTargetFile(any(), any(), any());

            assertDoesNotThrow(() -> scrapingInteractor.ns(document));
            verify(scrapingInteractor, times(0)).doBsOptionOfTotalFixedLiabilitiesIfTarget(company, document);
            verify(documentSpecification, times(0)).updateFsToDone(document, FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES, "file");
            verify(documentSpecification, times(1)).updateFsToError(document, FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES);
        }

        @DisplayName("ns : 企業情報が存在しないときはエラーにする")
        @Test
        void fundanalyzerRuntimeException() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.empty());
            assertThrows(FundanalyzerRuntimeException.class, () -> scrapingInteractor.ns(document));
        }
    }

    @Nested
    class findTargetFile {

        private final Document document = defaultDocument(DocumentTypeCode.DTC_120);

        ScrapingKeywordEntity scrapingKeyword = new ScrapingKeywordEntity(
                null,
                null,
                "keyword",
                null,
                "remarks",
                null
        );
        File targetFile = new File("");

        @DisplayName("findTargetFile : フォルダから処理対象のファイルを取得する")
        @Test
        void ok() {
            when(scrapingKeywordDao.selectByFinancialStatementId("1")).thenReturn(List.of(scrapingKeyword));
            when(xbrlScraping.findFile(targetFile, scrapingKeyword, document)).thenReturn(Optional.of(new File("actual")));

            var actual = scrapingInteractor.findTargetFile(targetFile, FinancialStatementEnum.BALANCE_SHEET, document);

            assertEquals(new File("actual"), actual.getFirst());
            assertEquals(scrapingKeyword, actual.getSecond());
        }

        @DisplayName("findTargetFile : 対象のファイルが存在しなかったらエラー発生する")
        @Test
        void noFile() {
            when(scrapingKeywordDao.selectByFinancialStatementId("1")).thenReturn(List.of(scrapingKeyword));
            when(xbrlScraping.findFile(targetFile, scrapingKeyword, document)).thenThrow(FundanalyzerFileException.class);

            assertThrows(FundanalyzerFileException.class, () -> scrapingInteractor.findTargetFile(targetFile, FinancialStatementEnum.BALANCE_SHEET, document));
        }

        @DisplayName("findTargetFile : キーワードに合致するファイルが存在しなかったらエラー発生する")
        @Test
        void noKeyword() {
            when(scrapingKeywordDao.selectByFinancialStatementId("1")).thenReturn(List.of(scrapingKeyword));
            when(xbrlScraping.findFile(targetFile, scrapingKeyword, document)).thenReturn(Optional.empty());

            assertThrows(FundanalyzerFileException.class, () -> scrapingInteractor.findTargetFile(targetFile, FinancialStatementEnum.BALANCE_SHEET, document));
        }
    }

    @Nested
    class sortedScrapingKeywordList {

        @DisplayName("priority : スクレイピングするためのキーワードを並び替える")
        @Test
        void priority() {
            final List<ScrapingKeywordEntity> list = List.of(
                    new ScrapingKeywordEntity(
                            1,
                            null,
                            "keyword",
                            null,
                            "remarks",
                            null
                    ),
                    new ScrapingKeywordEntity(
                            2,
                            null,
                            "keyword",
                            1,
                            "remarks",
                            null
                    ),
                    new ScrapingKeywordEntity(
                            3,
                            null,
                            "keyword",
                            2,
                            "remarks",
                            null
                    )
            );

            var actual = scrapingInteractor.sortedScrapingKeywordList(list);
            assertAll(
                    () -> assertEquals(2, actual.get(0).getId()),
                    () -> assertEquals(3, actual.get(1).getId()),
                    () -> assertEquals(1, actual.get(2).getId())
            );
        }
    }

    private Document defaultDocument(DocumentTypeCode documentTypeCode) {
        return new Document(
                "documentId",
                documentTypeCode,
                null,
                "edinetCode",
                null,
                LocalDate.now(),
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
                null,
                false,
                true
        );
    }
}