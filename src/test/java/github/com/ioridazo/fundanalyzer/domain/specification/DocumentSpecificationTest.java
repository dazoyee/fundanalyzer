package github.com.ioridazo.fundanalyzer.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.DocumentEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.EdinetDocument;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.Results;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class DocumentSpecificationTest {

    private DocumentDao documentDao;
    private EdinetDocumentSpecification edinetDocumentSpecification;
    private AnalysisResultSpecification analysisResultSpecification;

    private DocumentSpecification documentSpecification;

    @BeforeEach
    void setUp() {
        documentDao = Mockito.mock(DocumentDao.class);
        edinetDocumentSpecification = Mockito.mock(EdinetDocumentSpecification.class);
        analysisResultSpecification = Mockito.mock(AnalysisResultSpecification.class);

        documentSpecification = Mockito.spy(new DocumentSpecification(
                documentDao,
                Mockito.mock(IndustrySpecification.class),
                Mockito.mock(CompanySpecification.class),
                edinetDocumentSpecification,
                analysisResultSpecification
        ));
        documentSpecification.targetTypeCodes = List.of("120");
    }

    @DisplayName("latestDocument : 直近提出日のドキュメント情報を取得する")
    @Test
    void latestDocument() {
        var company = new Company(
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
        var document1 = defaultDocumentEntity(LocalDate.parse("2021-05-08"));
        var document2 = defaultDocumentEntity(LocalDate.parse("2021-05-09"));

        when(documentDao.selectByEdinetCodeAndType(any(), any())).thenReturn(List.of(document1, document2));
        when(edinetDocumentSpecification.parsePeriod(any())).thenReturn(new EdinetDocument());

        var actual = documentSpecification.latestDocument(company);

        assertEquals(LocalDate.parse("2021-05-09"), actual.map(Document::getSubmitDate).orElseThrow());
    }

    @Nested
    class analysisTargetList {

        DateInputData inputData = DateInputData.of(LocalDate.now());

        @BeforeEach
        void setUp() {
            when(analysisResultSpecification.isAnalyzed(any())).thenReturn(false);
        }

        @DisplayName("analysisTargetList : すべてDONEならば対象にする")
        @Test
        void present() {
            var document = new Document(
                    null,
                    null,
                    null,
                    LocalDate.parse("2021-01-01"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    DocumentStatus.DONE,
                    null,
                    DocumentStatus.DONE,
                    null,
                    DocumentStatus.DONE,
                    null,
                    false
            );
            doReturn(List.of(document)).when(documentSpecification).targetList(any());

            var actual = documentSpecification.analysisTargetList(inputData);

            assertEquals(1, actual.size());
        }

        @DisplayName("analysisTargetList : すべてDONEないならば対象にしない")
        @Test
        void empty() {
            var document = new Document(
                    null,
                    null,
                    null,
                    LocalDate.parse("2021-01-01"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    DocumentStatus.ERROR,
                    null,
                    DocumentStatus.DONE,
                    null,
                    DocumentStatus.DONE,
                    null,
                    false
            );
            doReturn(List.of(document)).when(documentSpecification).targetList(any());

            var actual = documentSpecification.analysisTargetList(inputData);

            assertEquals(0, actual.size());
        }
    }

    @Nested
    class extractScrapedList {

        @DisplayName("extractScrapedList : 処理済ドキュメントを抽出する")
        @Test
        void scraped() {
            var document = new Document(
                    null,
                    null,
                    null,
                    LocalDate.parse("2021-01-01"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    DocumentStatus.DONE,
                    null,
                    DocumentStatus.DONE,
                    null,
                    DocumentStatus.DONE,
                    null,
                    false
            );

            var actual = documentSpecification.extractScrapedList(List.of(document));

            assertEquals(1, actual.getFirst().size());
            assertEquals(0, actual.getSecond().size());
        }

        @DisplayName("extractScrapedList : 未処理ドキュメントを抽出する")
        @Test
        void not_scraped() {
            var document = new Document(
                    null,
                    null,
                    null,
                    LocalDate.parse("2021-01-01"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    DocumentStatus.DONE,
                    null,
                    DocumentStatus.DONE,
                    null,
                    DocumentStatus.HALF_WAY,
                    null,
                    false
            );

            var actual = documentSpecification.extractScrapedList(List.of(document));

            assertEquals(0, actual.getFirst().size());
            assertEquals(1, actual.getSecond().size());
        }
    }

    @Nested
    class extractAnalyzedList {

        @DisplayName("extractAnalyzedList : 分析済ドキュメントを抽出する")
        @Test
        void analyzed() {
            var document = new Document(
                    null,
                    null,
                    null,
                    LocalDate.parse("2021-01-01"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    DocumentStatus.DONE,
                    null,
                    DocumentStatus.DONE,
                    null,
                    DocumentStatus.DONE,
                    null,
                    false
            );
            when(analysisResultSpecification.isAnalyzed(document)).thenReturn(true);

            var actual = documentSpecification.extractAnalyzedList(List.of(document));

            assertEquals(1, actual.getFirst().size());
            assertEquals(0, actual.getSecond().size());
        }

        @DisplayName("extractAnalyzedList : 未分析ドキュメントを抽出する")
        @Test
        void not_analyzed() {
            var document = new Document(
                    null,
                    null,
                    null,
                    LocalDate.parse("2021-01-01"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    DocumentStatus.DONE,
                    null,
                    DocumentStatus.DONE,
                    null,
                    DocumentStatus.DONE,
                    null,
                    false
            );
            when(analysisResultSpecification.isAnalyzed(document)).thenReturn(false);

            var actual = documentSpecification.extractAnalyzedList(List.of(document));

            assertEquals(0, actual.getFirst().size());
            assertEquals(1, actual.getSecond().size());
        }
    }

    @Nested
    class parseDocumentPeriod {

        @DisplayName("parseDocumentPeriod : periodEndが存在するときはパースしてdocumentPeriodを生成する")
        @Test
        void documentPeriod_isPresent() {
            var results = new Results();
            results.setDocTypeCode("120");
            results.setPeriodEnd("2020-12-31");

            var actual = documentSpecification.parseDocumentPeriod(results);

            assertEquals(LocalDate.parse("2020-01-01"), actual.orElseThrow());
        }

        @DisplayName("parseDocumentPeriod : periodEndが存在しないときは親書類からdocumentPeriodを生成する")
        @Test
        void documentPeriod_isEmpty_parentDocument_isPresent() {
            var results = new Results();
            results.setDocTypeCode("120");
            results.setParentDocID("docId");

            when(documentDao.selectByDocumentId("docId"))
                    .thenReturn(DocumentEntity.builder().documentPeriod(LocalDate.parse("2020-01-01")).build());

            var actual = documentSpecification.parseDocumentPeriod(results);

            assertEquals(LocalDate.parse("2020-01-01"), actual.orElseThrow());
        }

        @DisplayName("parseDocumentPeriod : periodEndも親書類も存在しないときはnullの意をこめて1970-01-01にする（手パッチ対象）")
        @Test
        void documentPeriod_isEmpty_parentDocument_isEmpty() {
            var results = new Results();
            results.setDocTypeCode("120");
            results.setParentDocID("docId");

            var actual = documentSpecification.parseDocumentPeriod(results);

            assertEquals(LocalDate.EPOCH, actual.orElseThrow());
        }

        @DisplayName("parseDocumentPeriod : 対象外の書類種別コードならdocumentPeriodはnullで登録する")
        @Test
        void documentPeriod_noTarget() {
            var results = new Results();
            results.setDocTypeCode("140");

            var actual = documentSpecification.parseDocumentPeriod(results);

            assertNull(actual.orElse(null));
        }

    }

    private DocumentEntity defaultDocumentEntity(LocalDate submitDate) {
        return DocumentEntity.builder()
                .id(1)
                .documentId("documentId1")
                .documentTypeCode("120")
                .edinetCode("E00001")
                .submitDate(submitDate)
                .downloaded("0")
                .decoded("0")
                .scrapedNumberOfShares("0")
                .scrapedBs("0")
                .scrapedPl("0")
                .removed("0")
                .build();
    }
}