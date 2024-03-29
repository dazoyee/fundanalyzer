package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.Results;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.EdinetDocument;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
        documentSpecification.targetTypeCodes = List.of("120", "130", "140", "150");
    }

    @Nested
    class findLatestDocument {

        private final Company company = new Company(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                true
        );

        @DisplayName("findLatestDocument : 最新のドキュメント情報を取得する")
        @Test
        void ok() {
            var document1 = DocumentEntity.builder()
                    .documentTypeCode("120")
                    .documentPeriod(LocalDate.parse("2022-01-01"))
                    .submitDate(LocalDate.parse("2022-05-08"))
                    .downloaded("0")
                    .decoded("0")
                    .scrapedNumberOfShares("0")
                    .scrapedBs("0")
                    .scrapedPl("0")
                    .removed("0")
                    .build();
            var document2 = DocumentEntity.builder()
                    .documentTypeCode("120")
                    .documentPeriod(LocalDate.parse("2022-01-01"))
                    .submitDate(LocalDate.parse("2022-12-08"))
                    .downloaded("0")
                    .decoded("0")
                    .scrapedNumberOfShares("0")
                    .scrapedBs("0")
                    .scrapedPl("0")
                    .removed("0")
                    .build();
            var document3 = DocumentEntity.builder()
                    .documentTypeCode("120")
                    .documentPeriod(LocalDate.parse("2020-01-01"))
                    .submitDate(LocalDate.parse("2022-12-25"))
                    .downloaded("0")
                    .decoded("0")
                    .scrapedNumberOfShares("0")
                    .scrapedBs("0")
                    .scrapedPl("0")
                    .removed("0")
                    .build();

            when(documentDao.selectByEdinetCodeAndType(any(), any())).thenReturn(List.of(document1, document2, document3));
            when(edinetDocumentSpecification.inquiryLimitedEdinetDocument(any())).thenReturn(new EdinetDocument());

            var actual = documentSpecification.findLatestDocument(company);

            assertAll(
                    () -> assertEquals(LocalDate.parse("2022-01-01"), actual.flatMap(Document::getDocumentPeriod).orElseThrow()),
                    () -> assertEquals(LocalDate.parse("2022-12-08"), actual.map(Document::getSubmitDate).orElseThrow())
            );
        }

        @DisplayName("findLatestDocument : ドキュメント情報が存在しないとき")
        @Test
        void isEmpty() {
            when(documentDao.selectByEdinetCodeAndType(any(), any())).thenReturn(List.of());
            when(edinetDocumentSpecification.inquiryLimitedEdinetDocument(any())).thenReturn(new EdinetDocument());
            var actual = documentSpecification.findLatestDocument(company);
            assertNull(actual.orElse(null));
        }
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
            doReturn(List.of(document)).when(documentSpecification).inquiryTargetDocuments(any());

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
            doReturn(List.of(document)).when(documentSpecification).inquiryTargetDocuments(any());

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
    class insert {

        String documentId = "documentId";
        LocalDate submitDate = LocalDate.parse("2021-09-21");

        @DisplayName("insert : データベースに存在しなければ登録する")
        @Test
        void _insert() {
            var results = new Results();
            results.setDocId(documentId);
            var edinetResponse = new EdinetResponse();
            edinetResponse.setResults(List.of(results));

            when(documentDao.selectBySubmitDate(submitDate)).thenReturn(List.of());

            assertDoesNotThrow(() -> documentSpecification.insert(submitDate, edinetResponse));
            verify(documentDao, times(1)).insert(any());
        }

        @DisplayName("insert : データベースに存在すれば登録しない")
        @Test
        void not_insert() {
            var results = new Results();
            results.setDocId(documentId);
            var edinetResponse = new EdinetResponse();
            edinetResponse.setResults(List.of(results));

            var document = DocumentEntity.builder().documentId(documentId).build();

            when(documentDao.selectBySubmitDate(submitDate)).thenReturn(List.of(document));

            assertDoesNotThrow(() -> documentSpecification.insert(submitDate, edinetResponse));
            verify(documentDao, times(0)).insert(any());
        }
    }

    @Nested
    class recoverDocumentPeriod {

        Document document = new Document(
                "documentId",
                null,
                null,
                "edinetCode",
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

        @DisplayName("recoverDocumentPeriod : documentPeriodが存在するときはそのまま返却する")
        @Test
        void documentPeriod_isPresent() {
            var document = new Document(
                    "documentId",
                    null,
                    null,
                    "edinetCode",
                    LocalDate.of(2021, 1, 1),
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
            var actual = documentSpecification.recoverDocumentPeriod(document);
            assertEquals(LocalDate.parse("2021-01-01"), actual);
        }

        @DisplayName("recoverDocumentPeriod : periodEndが存在するときはパースしてdocumentPeriodを生成する")
        @Test
        void periodEnd_isPresent() {
            var edinetDocument = new EdinetDocument();
            edinetDocument.setPeriodEnd(LocalDate.parse("2020-10-02"));

            when(edinetDocumentSpecification.inquiryLimitedEdinetDocument("documentId")).thenReturn(edinetDocument);
            var actual = documentSpecification.recoverDocumentPeriod(document);
            assertEquals(LocalDate.parse("2020-01-01"), actual);
        }

        @DisplayName("recoverDocumentPeriod : periodEndが存在しないときは親書類からdocumentPeriodを生成する")
        @Test
        void periodEnd_isEmpty_parentDocument_isPresent() {
            var edinetDocument = new EdinetDocument();
            edinetDocument.setParentDocId("parentDocId");

            when(edinetDocumentSpecification.inquiryLimitedEdinetDocument("documentId")).thenReturn(edinetDocument);
            when(documentDao.selectByDocumentId("parentDocId")).thenReturn(
                    Optional.of(DocumentEntity.builder()
                            .documentPeriod(LocalDate.parse("2019-01-01"))
                            .build()
                    ));
            var actual = documentSpecification.recoverDocumentPeriod(document);
            assertEquals(LocalDate.parse("2019-01-01"), actual);
        }

        @DisplayName("recoverDocumentPeriod : periodEndも親書類も存在しないときはnullの意をこめて1970-01-01にする（手パッチ対象）")
        @Test
        void periodEnd_isEmpty_parentDocument_isEmpty() {
            var edinetDocument = new EdinetDocument();
            edinetDocument.setParentDocId("parentDocId");

            when(edinetDocumentSpecification.inquiryLimitedEdinetDocument("documentId")).thenReturn(edinetDocument);
            when(documentDao.selectByDocumentId("parentDocId")).thenReturn(Optional.of(DocumentEntity.builder().build()));
            var actual = documentSpecification.recoverDocumentPeriod(document);
            assertEquals(LocalDate.parse("1970-01-01"), actual);
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
                    .thenReturn(Optional.of(DocumentEntity.builder().documentPeriod(LocalDate.parse("2020-01-01")).build()));

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
            results.setDocTypeCode("135");

            var actual = documentSpecification.parseDocumentPeriod(results);

            assertNull(actual.orElse(null));
        }
    }
}