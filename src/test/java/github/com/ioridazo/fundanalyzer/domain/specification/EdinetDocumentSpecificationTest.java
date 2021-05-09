package github.com.ioridazo.fundanalyzer.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocumentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class EdinetDocumentSpecificationTest {

    private EdinetDocumentDao edinetDocumentDao;

    private EdinetDocumentSpecification edinetDocumentSpecification;

    @BeforeEach
    void setUp() {
        edinetDocumentDao = Mockito.mock(EdinetDocumentDao.class);

        edinetDocumentSpecification = Mockito.spy(new EdinetDocumentSpecification(edinetDocumentDao));
    }

    @Nested
    class parsePeriod {

        String documentId = "documentId";

        @DisplayName("parsePeriod : periodが存在するときはそのまま返却する")
        @Test
        void period_isPresent() {
            var edinetDocument = new EdinetDocumentEntity();
            edinetDocument.setPeriodStart("2020-01-01");
            edinetDocument.setPeriodEnd("2020-12-31");

            when(edinetDocumentDao.selectByDocId(documentId)).thenReturn(edinetDocument);

            var actual = edinetDocumentSpecification.parsePeriod(documentId);

            assertEquals(LocalDate.parse("2020-01-01"), actual.getPeriodStart());
            assertEquals(LocalDate.parse("2020-12-31"), actual.getPeriodEnd());
        }

        @DisplayName("parsePeriod : periodが存在しないときは親書類から生成する")
        @Test
        void parentDocId_isPresent() {
            var edinetDocument = new EdinetDocumentEntity();
            edinetDocument.setParentDocId("docId");
            var parentEdinetDocument = new EdinetDocumentEntity();
            parentEdinetDocument.setPeriodStart("2020-01-01");
            parentEdinetDocument.setPeriodEnd("2020-12-31");

            when(edinetDocumentDao.selectByDocId(documentId)).thenReturn(edinetDocument);
            when(edinetDocumentDao.selectByDocId("docId")).thenReturn(parentEdinetDocument);

            var actual = edinetDocumentSpecification.parsePeriod(documentId);

            assertEquals(LocalDate.parse("2020-01-01"), actual.getPeriodStart());
            assertEquals(LocalDate.parse("2020-12-31"), actual.getPeriodEnd());

        }

        @DisplayName("parsePeriod : 親書類にも存在しないときはnullの意をこめて1970-01-01にする（手パッチ対象）")
        @Test
        void parentDocId_isPresent_period_isEmpty() {
            var edinetDocument = new EdinetDocumentEntity();
            edinetDocument.setParentDocId("docId");
            var parentEdinetDocument = new EdinetDocumentEntity();

            when(edinetDocumentDao.selectByDocId(documentId)).thenReturn(edinetDocument);
            when(edinetDocumentDao.selectByDocId("docId")).thenReturn(parentEdinetDocument);

            var actual = edinetDocumentSpecification.parsePeriod(documentId);

            assertEquals(LocalDate.EPOCH, actual.getPeriodStart());
            assertEquals(LocalDate.EPOCH, actual.getPeriodEnd());

        }

        @DisplayName("parsePeriod : periodも親書類も存在しないときはnullの意をこめて1970-01-01にする（手パッチ対象）")
        @Test
        void parentDocId_isEmpty() {
            var edinetDocument = new EdinetDocumentEntity();
            edinetDocument.setParentDocId("docId");

            when(edinetDocumentDao.selectByDocId(documentId)).thenReturn(edinetDocument);

            var actual = edinetDocumentSpecification.parsePeriod(documentId);

            assertEquals(LocalDate.EPOCH, actual.getPeriodStart());
            assertEquals(LocalDate.EPOCH, actual.getPeriodEnd());
        }

        @DisplayName("parsePeriod : periodも親書類も存在しないときはnullの意をこめて1970-01-01にする（手パッチ対象）")
        @Test
        void all_empty() {
            var actual = edinetDocumentSpecification.parsePeriod(documentId);

            assertEquals(LocalDate.EPOCH, actual.getPeriodStart());
            assertEquals(LocalDate.EPOCH, actual.getPeriodEnd());
        }
    }
}