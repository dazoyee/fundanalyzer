package github.com.ioridazo.fundanalyzer.domain.logic.analysis;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BsEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.DocTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.PlEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.PlSubject;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatement;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCalculateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisLogicTest {

    private CompanyDao companyDao;
    private BsSubjectDao bsSubjectDao;
    private PlSubjectDao plSubjectDao;
    private DocumentDao documentDao;
    private FinancialStatementDao financialStatementDao;
    private AnalysisResultDao analysisResultDao;

    private AnalysisLogic logic;

    @BeforeEach
    void setUp() {
        companyDao = Mockito.mock(CompanyDao.class);
        bsSubjectDao = Mockito.mock(BsSubjectDao.class);
        plSubjectDao = Mockito.mock(PlSubjectDao.class);
        documentDao = Mockito.mock(DocumentDao.class);
        financialStatementDao = Mockito.mock(FinancialStatementDao.class);
        analysisResultDao = Mockito.mock(AnalysisResultDao.class);

        logic = Mockito.spy(new AnalysisLogic(
                companyDao,
                bsSubjectDao,
                plSubjectDao,
                documentDao,
                financialStatementDao,
                analysisResultDao
        ));
    }

    @Nested
    class analyze {

        @DisplayName("analyze : 書類IDで価値を分析してDBに登録することを確認する")
        @Test
        void analyze_documentId_ok() {
            var documentId = "docId";
            var period = LocalDate.parse("2020-12-31");
            var code = "code";
            var document = Document.builder()
                    .edinetCode("edinetCode")
                    .documentTypeCode("120")
                    .period(period)
                    .build();
            var company = new Company(
                    code,
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
            var createdAt = LocalDateTime.of(2020, 10, 4, 12, 48);

            when(documentDao.selectByDocumentId(documentId)).thenReturn(document);
            when(companyDao.selectAll()).thenReturn(List.of(company));
            doReturn(BigDecimal.valueOf(1)).when(logic).calculate("code", period, DocTypeCode.ANNUAL_SECURITIES_REPORT);
            when(logic.nowLocalDateTime()).thenReturn(createdAt);

            assertDoesNotThrow(() -> logic.analyze(documentId));

            verify(analysisResultDao, times(1)).insert(new AnalysisResult(
                    null,
                    code,
                    period,
                    BigDecimal.valueOf(1),
                    createdAt
            ));
        }

        @DisplayName("analyze : 分析時にエラーが発生したときの処理を確認する")
        @Test
        void analyze_FundanalyzerCalculateException() {
            var documentId = "docId";
            var period = LocalDate.parse("2020-12-31");
            var code = "code";
            var document = Document.builder()
                    .edinetCode("edinetCode")
                    .documentTypeCode("120")
                    .period(period)
                    .build();
            var company = new Company(
                    code,
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
            var createdAt = LocalDateTime.of(2020, 10, 4, 12, 48);

            when(documentDao.selectByDocumentId(documentId)).thenReturn(document);
            when(companyDao.selectAll()).thenReturn(List.of(company));
            doThrow(FundanalyzerCalculateException.class).when(logic).calculate("code", period, DocTypeCode.ANNUAL_SECURITIES_REPORT);
            when(logic.nowLocalDateTime()).thenReturn(createdAt);

            assertDoesNotThrow(() -> logic.analyze(documentId));

            verify(analysisResultDao, times(0)).insert(new AnalysisResult(
                    null,
                    code,
                    period,
                    BigDecimal.valueOf(1),
                    createdAt
            ));
        }
    }

    @Nested
    class calculate {

        @DisplayName("calculate : 各種値を取得して、計算することを確認する")
        @Test
        void calculate_ok() {
            var companyCode = "code";
            var period = LocalDate.parse("2020-10-10");
            var annualSecuritiesReport = DocTypeCode.ANNUAL_SECURITIES_REPORT;
            var company = new Company(
                    null,
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
            var parameter = AnalysisLogic.FsValueParameter.of(company, period, annualSecuritiesReport);

            when(companyDao.selectByCode(companyCode)).thenReturn(Optional.of(company));
            doReturn(1000L).when(logic).bsValue(BsEnum.TOTAL_CURRENT_ASSETS, parameter);
            doReturn(1000L).when(logic).bsValue(BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS, parameter);
            doReturn(1000L).when(logic).bsValue(BsEnum.TOTAL_CURRENT_LIABILITIES, parameter);
            doReturn(1000L).when(logic).bsValue(BsEnum.TOTAL_FIXED_LIABILITIES, parameter);
            doReturn(10000L).when(logic).plValue(PlEnum.OPERATING_PROFIT, parameter);
            doReturn(1000L).when(logic).nsValue(parameter);

            var expected = BigDecimal.valueOf((10000L * 10 + 1000 - (1000 * 1.2) + 1000 - 1000) / 1000);

            assertEquals(expected, logic.calculate(companyCode, period, annualSecuritiesReport));
        }
    }

    @Nested
    class getValue {

        @DisplayName("bsValues : 貸借対照表の値を正常に取得することを確認する")
        @Test
        void bsValues_ok() {
            var company = new Company(
                    null,
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
            var bsEnum = BsEnum.TOTAL_CURRENT_ASSETS;
            var period = LocalDate.parse("2020-10-10");
            var annualSecuritiesReport = DocTypeCode.ANNUAL_SECURITIES_REPORT;
            var bsSubject = new BsSubject("1", "1", "1", "流動資産合計");
            var expected = 1000L;
            var financialStatement = new FinancialStatement(
                    1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    expected,
                    null,
                    null
            );

            when(bsSubjectDao.selectByOutlineSubjectId(bsEnum.getOutlineSubjectId()))
                    .thenReturn(List.of(bsSubject));
            when(financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    bsSubject.getId(),
                    String.valueOf(period.getYear()),
                    annualSecuritiesReport.toValue()
            )).thenReturn(Optional.of(financialStatement));

            assertEquals(expected, logic.bsValue(bsEnum, AnalysisLogic.FsValueParameter.of(company, period, annualSecuritiesReport)));
        }

        @DisplayName("bsValues : 貸借対照表の値の取得に失敗したときの処理を確認する")
        @Test
        void bsValues_FundanalyzerCalculateException() {
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
            var bsEnum = BsEnum.TOTAL_CURRENT_ASSETS;
            var period = LocalDate.parse("2020-10-10");
            var annualSecuritiesReport = DocTypeCode.ANNUAL_SECURITIES_REPORT;
            var bsSubject = new BsSubject("1", "1", "1", "流動資産合計");
            var documentId = "docId";
            var document = Document.builder().documentId(documentId).bsDocumentPath("bsDocumentPath").build();
            var updated = LocalDateTime.of(2020, 10, 10, 14, 32);

            when(bsSubjectDao.selectByOutlineSubjectId(bsEnum.getOutlineSubjectId()))
                    .thenReturn(List.of(bsSubject));
            when(financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    bsSubject.getId(),
                    String.valueOf(period.getYear()),
                    annualSecuritiesReport.toValue()
            )).thenReturn(Optional.empty());
            when(companyDao.selectAll()).thenReturn(List.of(company));
            when(documentDao.selectDocumentIdBy(
                    "edinetCode",
                    "120",
                    "2020"
            )).thenReturn(document);
            when(logic.nowLocalDateTime()).thenReturn(updated);
            when(documentDao.selectByDocumentId(documentId)).thenReturn(document);

            assertThrows(FundanalyzerCalculateException.class, () -> logic.bsValue(bsEnum, AnalysisLogic.FsValueParameter.of(company, period, annualSecuritiesReport)));

            verify(documentDao, times(1)).update(Document.builder()
                    .documentId(documentId)
                    .scrapedBs(DocumentStatus.HALF_WAY.toValue())
                    .updatedAt(updated)
                    .build());
        }

        @DisplayName("plValues : 損益計算書の値を正常に取得することを確認する")
        @Test
        void plValues_ok() {
            var company = new Company(
                    null,
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
            var plEnum = PlEnum.OPERATING_PROFIT;
            var period = LocalDate.parse("2020-10-10");
            var annualSecuritiesReport = DocTypeCode.ANNUAL_SECURITIES_REPORT;
            var plSubject = new PlSubject("1", "1", "1", "流動資産合計");
            var expected = 1000L;
            var financialStatement = new FinancialStatement(
                    1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    expected,
                    null,
                    null
            );

            when(plSubjectDao.selectByOutlineSubjectId(plEnum.getOutlineSubjectId()))
                    .thenReturn(List.of(plSubject));
            when(financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.toValue(),
                    plSubject.getId(),
                    String.valueOf(period.getYear()),
                    annualSecuritiesReport.toValue()
            )).thenReturn(Optional.of(financialStatement));

            assertEquals(expected, logic.plValue(plEnum, AnalysisLogic.FsValueParameter.of(company, period, annualSecuritiesReport)));
        }

        @DisplayName("plValues : 損益計算書の値の取得に失敗したときの処理を確認する")
        @Test
        void plValues_FundanalyzerCalculateException() {
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
            var plEnum = PlEnum.OPERATING_PROFIT;
            var period = LocalDate.parse("2020-10-10");
            var annualSecuritiesReport = DocTypeCode.ANNUAL_SECURITIES_REPORT;
            var plSubject = new PlSubject("1", "1", "1", "流動資産合計");
            var documentId = "docId";
            var document = Document.builder().documentId(documentId).plDocumentPath("plDocumentPath").build();
            var updated = LocalDateTime.of(2020, 10, 10, 14, 32);

            when(plSubjectDao.selectByOutlineSubjectId(plEnum.getOutlineSubjectId()))
                    .thenReturn(List.of(plSubject));
            when(financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    plSubject.getId(),
                    String.valueOf(period.getYear()),
                    annualSecuritiesReport.toValue()
            )).thenReturn(Optional.empty());
            when(companyDao.selectAll()).thenReturn(List.of(company));
            when(documentDao.selectDocumentIdBy(
                    "edinetCode",
                    "120",
                    "2020"
            )).thenReturn(document);
            when(logic.nowLocalDateTime()).thenReturn(updated);
            when(documentDao.selectByDocumentId(documentId)).thenReturn(document);

            assertThrows(FundanalyzerCalculateException.class, () -> logic.plValue(plEnum, AnalysisLogic.FsValueParameter.of(company, period, annualSecuritiesReport)));

            verify(documentDao, times(1)).update(Document.builder()
                    .documentId(documentId)
                    .scrapedPl(DocumentStatus.HALF_WAY.toValue())
                    .updatedAt(updated)
                    .build());
        }

        @DisplayName("nsValue : 株式総数の値を正常に取得することを確認する")
        @Test
        void nsValue_ok() {
            var company = new Company(
                    null,
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
            var period = LocalDate.parse("2020-10-10");
            var annualSecuritiesReport = DocTypeCode.ANNUAL_SECURITIES_REPORT;
            var expected = 1000L;
            var financialStatement = new FinancialStatement(
                    1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    expected,
                    null,
                    null
            );

            when(financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.toValue(),
                    "0",
                    String.valueOf(period.getYear()),
                    annualSecuritiesReport.toValue()
            )).thenReturn(Optional.of(financialStatement));

            assertEquals(expected, logic.nsValue(AnalysisLogic.FsValueParameter.of(company, period, annualSecuritiesReport)));
        }

        @DisplayName("nsValue : 株式総数の値の取得に失敗したときの処理を確認する")
        @Test
        void nsValue_FundanalyzerCalculateException() {
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
            var period = LocalDate.parse("2020-10-10");
            var annualSecuritiesReport = DocTypeCode.ANNUAL_SECURITIES_REPORT;
            var documentId = "docId";
            var document = Document.builder().documentId(documentId).numberOfSharesDocumentPath("numberOfSharesDocumentPath").build();
            var updated = LocalDateTime.of(2020, 10, 10, 14, 32);

            when(financialStatementDao.selectByUniqueKey(
                    company.getEdinetCode(),
                    FinancialStatementEnum.BALANCE_SHEET.toValue(),
                    "0",
                    String.valueOf(period.getYear()),
                    annualSecuritiesReport.toValue()
            )).thenReturn(Optional.empty());
            when(companyDao.selectAll()).thenReturn(List.of(company));
            when(documentDao.selectDocumentIdBy(
                    "edinetCode",
                    "120",
                    "2020"
            )).thenReturn(document);
            when(logic.nowLocalDateTime()).thenReturn(updated);
            when(documentDao.selectByDocumentId(documentId)).thenReturn(document);

            assertThrows(FundanalyzerCalculateException.class, () -> logic.nsValue(AnalysisLogic.FsValueParameter.of(company, period, annualSecuritiesReport)));

            verify(documentDao, times(1)).update(Document.builder()
                    .documentId(documentId)
                    .scrapedNumberOfShares(DocumentStatus.HALF_WAY.toValue())
                    .updatedAt(updated)
                    .build());
        }
    }
}
