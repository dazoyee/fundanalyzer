package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.logic.analysis.AnalysisLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private AnalysisLogic analysisLogic;
    private IndustryDao industryDao;
    private CompanyDao companyDao;
    private DocumentDao documentDao;

    private AnalysisService service;

    @BeforeEach
    void before() {
        analysisLogic = Mockito.mock(AnalysisLogic.class);
        industryDao = Mockito.mock(IndustryDao.class);
        companyDao = Mockito.mock(CompanyDao.class);
        documentDao = Mockito.mock(DocumentDao.class);

        service = Mockito.spy(new AnalysisService(
                analysisLogic,
                industryDao,
                companyDao,
                documentDao,
                Mockito.mock(AnalysisResultDao.class)
        ));
    }

    @Nested
    class analyze {

        @DisplayName("analyze : 書類ID単位で企業価値を分析することを確認する")
        @Test
        void analyze_documentId_ok() {
            var documentId = "docId";

            assertDoesNotThrow(() -> service.analyze(documentId));
        }

        @DisplayName("analyze : 提出日単位で企業価値を分析することを確認する")
        @Test
        void analyze_submitDate_ok() {
            var targetTypes = List.of(DocumentTypeCode.DTC_120);
            var submitDate = LocalDate.parse("2020-10-04");
            var code = "code";
            var period = LocalDate.parse("2020-12-31");
            var docId = "docId";
            var companyAll = List.of(
                    new Company(
                            null,
                            null,
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            "not null",
                            null,
                            2,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            code,
                            "ターゲット",
                            3,
                            "edinetCode",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    )
            );
            var targetDocument = Document.builder()
                    .documentId(docId)
                    .edinetCode("edinetCode")
                    .documentPeriod(period)
                    .scrapedBs("1")
                    .scrapedPl("1")
                    .scrapedNumberOfShares("1")
                    .build();

            when(companyDao.selectAll()).thenReturn(companyAll);
            when(industryDao.selectByName("銀行業")).thenReturn(new Industry(1, "銀行業", null));
            when(industryDao.selectByName("保険業")).thenReturn(new Industry(2, "保険業", null));
            when(documentDao.selectByTypeAndSubmitDate(List.of("120"), submitDate)).thenReturn(List.of(targetDocument));

            assertDoesNotThrow(() -> service.analyze(submitDate, targetTypes));

            verify(analysisLogic, times(1)).analyze(docId);
        }

        @DisplayName("analyze : 処理対象が存在しないときはなにもしない")
        @Test
        void analyze_submitDate_nothing() {
            var targetTypes = List.of(DocumentTypeCode.DTC_120);
            var submitDate = LocalDate.parse("2020-10-04");
            var code = "code";
            var companyAll = List.of(
                    new Company(
                            null,
                            null,
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            "not null",
                            null,
                            2,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            code,
                            "ターゲット",
                            3,
                            "edinetCode",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    )
            );

            when(companyDao.selectAll()).thenReturn(companyAll);
            when(industryDao.selectByName("銀行業")).thenReturn(new Industry(1, "銀行業", null));
            when(industryDao.selectByName("保険業")).thenReturn(new Industry(2, "保険業", null));
            when(documentDao.selectByTypeAndSubmitDate(List.of("120"), submitDate)).thenReturn(List.of());

            assertDoesNotThrow(() -> service.analyze(submitDate, targetTypes));

            verify(analysisLogic, times(0)).analyze(any());
        }

        @DisplayName("analyze : スクレイピング処理ステータスがDONEでないなら処理しない")
        @Test
        void analyze_allMatch_status_error() {
            var targetTypes = List.of(DocumentTypeCode.DTC_120);
            var submitDate = LocalDate.parse("2020-10-04");
            var code = "code";
            var period = LocalDate.parse("2020-12-31");
            var docId = "docId";
            var companyAll = List.of(
                    new Company(
                            null,
                            null,
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            "not null",
                            null,
                            2,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            code,
                            "ターゲット",
                            3,
                            "edinetCode",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    )
            );
            var targetDocument = Document.builder()
                    .documentId(docId)
                    .edinetCode("edinetCode")
                    .documentPeriod(period)
                    .scrapedBs("5")
                    .scrapedPl("1")
                    .scrapedNumberOfShares("1")
                    .build();

            when(companyDao.selectAll()).thenReturn(companyAll);
            when(industryDao.selectByName("銀行業")).thenReturn(new Industry(1, "銀行業", null));
            when(industryDao.selectByName("保険業")).thenReturn(new Industry(2, "保険業", null));
            when(documentDao.selectByTypeAndSubmitDate(List.of("120"), submitDate)).thenReturn(List.of(targetDocument));

            assertDoesNotThrow(() -> service.analyze(submitDate, targetTypes));

            verify(analysisLogic, times(0)).analyze(docId);
        }
    }
}