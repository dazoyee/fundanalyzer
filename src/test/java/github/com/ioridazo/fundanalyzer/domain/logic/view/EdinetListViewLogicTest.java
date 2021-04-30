package github.com.ioridazo.fundanalyzer.domain.logic.view;

import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.master.CompanyEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.master.IndustryEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.DocumentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class EdinetListViewLogicTest {

    private IndustryDao industryDao;
    private CompanyDao companyDao;
    private DocumentDao documentDao;
    private AnalysisResultDao analysisResultDao;

    private EdinetListViewLogic logic;

    @BeforeEach
    void setUp() {
        industryDao = Mockito.mock(IndustryDao.class);
        companyDao = Mockito.mock(CompanyDao.class);
        documentDao = Mockito.mock(DocumentDao.class);
        analysisResultDao = Mockito.mock(AnalysisResultDao.class);

        logic = Mockito.spy(new EdinetListViewLogic(industryDao, companyDao, documentDao, analysisResultDao));
    }

    @DisplayName("counter : 提出日に対する処理状況をカウントする")
    @Test
    void counter_ok() {
        var submitDate = LocalDate.parse("2020-11-08");
        var targetTypeCode = List.of(DocumentTypeCode.DTC_120);

        var documentList = List.of(
                DocumentEntity.builder()
                        .documentId("documentId")
                        .edinetCode("edinetCode")
                        .documentPeriod(LocalDate.parse("2020-01-01"))
                        .documentTypeCode("120")
                        .submitDate(LocalDate.parse("2020-11-08"))
                        .scrapedNumberOfShares("1")
                        .scrapedBs("1")
                        .scrapedPl("1")
                        .removed("0")
                        .build(),
                DocumentEntity.builder()
                        .documentId("docId")
                        .edinetCode("edinetCode")
                        .documentPeriod(LocalDate.parse("2020-01-01"))
                        .documentTypeCode("120")
                        .submitDate(LocalDate.parse("2020-11-08"))
                        .scrapedNumberOfShares("0")
                        .scrapedBs("0")
                        .scrapedPl("0")
                        .removed("0")
                        .build()
        );
        var company = new CompanyEntity(
                "code",
                "会社名",
                1,
                "edinetCode",
                null,
                null,
                null,
                null,
                null,
                null
        );
        var createdAt = LocalDateTime.of(2020, 10, 17, 18, 15);

        when(documentDao.selectByTypeAndSubmitDate(List.of("120"), submitDate)).thenReturn(documentList);
        when(companyDao.selectAll()).thenReturn(List.of(company));
        when(industryDao.selectByName("銀行業")).thenReturn(new IndustryEntity(2, "銀行業", null));
        when(industryDao.selectByName("保険業")).thenReturn(new IndustryEntity(3, "保険業", null));
        when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
        when(analysisResultDao.selectByUniqueKey("code", LocalDate.parse("2020-01-01"), "120", LocalDate.parse("2020-11-08")))
                .thenReturn(Optional.of(new AnalysisResultEntity(null, null, null, null, null, null, null, null)));
        when(logic.nowLocalDateTime()).thenReturn(createdAt);

        var actual = logic.counter(submitDate, targetTypeCode);

        assertAll("EdinetListViewBean",
                () -> assertEquals(LocalDate.parse("2020-11-08"), actual.getSubmitDate(), "submitDate"),
                () -> assertEquals(2L, actual.getCountAll(), "countAll"),
                () -> assertEquals(2L, actual.getCountTarget(), "countTarget"),
                () -> assertEquals(1L, actual.getCountScraped(), "countScraped"),
                () -> assertEquals(1L, actual.getCountAnalyzed(), "countAnalyzed"),
                () -> assertEquals("", actual.getNotAnalyzedId(), "notAnalyzedCode"),
                () -> assertEquals("docId", actual.getCantScrapedId(), "cantScrapedCode"),
                () -> assertEquals(1L, actual.getCountNotScraped(), "countNotScraped"),
                () -> assertEquals(0L, actual.getCountNotTarget(), "countNotTarget"),
                () -> assertEquals(LocalDateTime.of(2020, 10, 17, 18, 15), actual.getCreatedAt()),
                () -> assertEquals(LocalDateTime.of(2020, 10, 17, 18, 15), actual.getUpdatedAt())
        );
    }

    @DisplayName("counter : 提出日に対する処理状況をカウントする")
    @Test
    void counter_ok2() {
        var submitDate = LocalDate.parse("2020-11-08");
        var targetTypeCode = List.of(DocumentTypeCode.DTC_120);

        final DocumentEntity documentEntity = DocumentEntity.builder()
                .documentId("documentId")
                .build();
        var company = new CompanyEntity(
                "code",
                "会社名",
                1,
                "edinetCode",
                null,
                null,
                null,
                null,
                null,
                null
        );
        var createdAt = LocalDateTime.of(2020, 10, 17, 18, 15);

        when(documentDao.selectByTypeAndSubmitDate(List.of("120"), submitDate)).thenReturn(List.of(documentEntity));
        when(companyDao.selectAll()).thenReturn(List.of(company));
        when(industryDao.selectByName("銀行業")).thenReturn(new IndustryEntity(2, "銀行業", null));
        when(industryDao.selectByName("保険業")).thenReturn(new IndustryEntity(3, "保険業", null));

        doReturn(List.of(documentEntity, documentEntity)).when(logic).extractTargetList(any(), any());
        doReturn(Pair.of(List.of(documentEntity), List.of(documentEntity, documentEntity))).when(logic).extractScrapedList(any());
        doReturn(Pair.of(List.of(documentEntity), List.of())).when(logic).extractAnalyzedList(any());
        when(logic.nowLocalDateTime()).thenReturn(createdAt);

        var actual = logic.counter(submitDate, targetTypeCode);

        assertAll("EdinetListViewBean",
                () -> assertEquals(LocalDate.parse("2020-11-08"), actual.getSubmitDate(), "submitDate"),
                () -> assertEquals(1L, actual.getCountAll(), "countAll"),
                () -> assertEquals(2L, actual.getCountTarget(), "countTarget"),
                () -> assertEquals(1L, actual.getCountScraped(), "countScraped"),
                () -> assertEquals(1L, actual.getCountAnalyzed(), "countAnalyzed"),
                () -> assertEquals("", actual.getNotAnalyzedId(), "notAnalyzedCode"),
                () -> assertEquals("documentId,<br>documentId", actual.getCantScrapedId(), "cantScrapedCode"),
                () -> assertEquals(2L, actual.getCountNotScraped(), "countNotScraped"),
                () -> assertEquals(-1L, actual.getCountNotTarget(), "countNotTarget"),
                () -> assertEquals(LocalDateTime.of(2020, 10, 17, 18, 15), actual.getCreatedAt()),
                () -> assertEquals(LocalDateTime.of(2020, 10, 17, 18, 15), actual.getUpdatedAt())
        );
    }

    @DisplayName("extractTargetList : 処理対象書類リストを抽出する")
    @Test
    void extractTargetList() {
        var documentList = List.of(
                DocumentEntity.builder()
                        .edinetCode("edinetCode")
                        .removed("0")
                        .build()
        );
        var allTargetCompanies = List.of(new CompanyEntity(
                "code",
                "会社名",
                1,
                "edinetCode",
                null,
                null,
                null,
                null,
                null,
                null
        ));

        var actual = logic.extractTargetList(documentList, allTargetCompanies);

        assertEquals(1, actual.size());
    }

    @DisplayName("extractScrapedList : 処理済,未処理書類リストを抽出する")
    @Test
    void extractScrapedList() {
        var targetList = List.of(
                DocumentEntity.builder()
                        .scrapedNumberOfShares("1")
                        .scrapedBs("1")
                        .scrapedPl("1")
                        .removed("0")
                        .build(),
                DocumentEntity.builder()
                        .scrapedNumberOfShares("0")
                        .scrapedBs("0")
                        .scrapedPl("0")
                        .removed("0")
                        .build()
        );

        var actual = logic.extractScrapedList(targetList);

        assertEquals(1, actual.getFirst().size());
        assertEquals(1, actual.getSecond().size());
    }

    @DisplayName("extractAnalyzedList : 分析済,未分析書類リストを抽出する")
    @Test
    void extractAnalyzedList() {
        var scrapedList = List.of(
                DocumentEntity.builder()
                        .documentId("documentId")
                        .edinetCode("edinetCode")
                        .documentPeriod(LocalDate.parse("2020-01-01"))
                        .documentTypeCode("120")
                        .submitDate(LocalDate.parse("2020-11-08"))
                        .removed("0")
                        .build(),
                DocumentEntity.builder()
                        .documentId("docId")
                        .edinetCode("edinetCode")
                        .documentPeriod(LocalDate.parse("2021-01-01"))
                        .documentTypeCode("120")
                        .submitDate(LocalDate.parse("2020-11-08"))
                        .removed("0")
                        .build()
        );

        when(companyDao.selectByEdinetCode("edinetCode"))
                .thenReturn(Optional.of(new CompanyEntity(
                        "code",
                        "会社名",
                        1,
                        "edinetCode",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )));
        when(analysisResultDao.selectByUniqueKey("code", LocalDate.parse("2020-01-01"), "120", LocalDate.parse("2020-11-08")))
                .thenReturn(Optional.of(new AnalysisResultEntity(null, null, null, null, null, null, null, null)));


        var actual = logic.extractAnalyzedList(scrapedList);

        assertEquals(1, actual.getFirst().size());
        assertEquals(1, actual.getSecond().size());
    }
}