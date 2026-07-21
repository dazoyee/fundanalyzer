package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class DocumentDaoTest {

    @Autowired
    private DocumentDao documentDao;

    @DisplayName("selectByTypeAndNoPeriodAndSubmitDate : document_period が真のNULLの書類も対象期間なしとして取得できる")
    @Test
    void findsTrueNullDocumentPeriod() {
        documentDao.insert(DocumentEntity.builder()
                .documentId("S100NULL")
                .documentTypeCode("160")
                .documentPeriod(null)
                .submitDate(LocalDate.parse("2026-01-14"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        final List<DocumentEntity> actual = documentDao.selectByTypeAndNoPeriodAndSubmitDate(
                List.of("120", "130", "140", "150", "160", "170"), LocalDate.parse("2026-01-14"));

        assertEquals(1, actual.size());
        assertEquals("S100NULL", actual.get(0).getDocumentId());
    }

    @DisplayName("selectByTypeAndNoPeriodAndSubmitDate : document_period が1970-01-01(EPOCHプレースホルダ)の書類も対象期間なしとして取得できる")
    @Test
    void findsEpochPlaceholderDocumentPeriod() {
        documentDao.insert(DocumentEntity.builder()
                .documentId("S100EPOC")
                .documentTypeCode("160")
                .documentPeriod(LocalDate.parse("1970-01-01"))
                .submitDate(LocalDate.parse("2026-01-14"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        final List<DocumentEntity> actual = documentDao.selectByTypeAndNoPeriodAndSubmitDate(
                List.of("120", "130", "140", "150", "160", "170"), LocalDate.parse("2026-01-14"));

        assertEquals(1, actual.size());
        assertEquals("S100EPOC", actual.get(0).getDocumentId());
    }

    @DisplayName("selectByTypeAndNoPeriodAndSubmitDate : document_period が設定済みの書類は対象外")
    @Test
    void excludesDocumentWithPeriod() {
        documentDao.insert(DocumentEntity.builder()
                .documentId("S100HAVE")
                .documentTypeCode("160")
                .documentPeriod(LocalDate.parse("2026-01-01"))
                .submitDate(LocalDate.parse("2026-01-14"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        final List<DocumentEntity> actual = documentDao.selectByTypeAndNoPeriodAndSubmitDate(
                List.of("120", "130", "140", "150", "160", "170"), LocalDate.parse("2026-01-14"));

        assertEquals(0, actual.size());
    }
}
