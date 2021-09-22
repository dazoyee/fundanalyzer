package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.Subject;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.domain.value.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.PlSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FinancialStatementSpecificationTest {

    private FinancialStatementDao financialStatementDao;
    private SubjectSpecification subjectSpecification;

    private FinancialStatementSpecification financialStatementSpecification;

    @BeforeEach
    void setUp() {
        financialStatementDao = Mockito.mock(FinancialStatementDao.class);
        subjectSpecification = Mockito.mock(SubjectSpecification.class);

        financialStatementSpecification = Mockito.spy(new FinancialStatementSpecification(
                financialStatementDao,
                subjectSpecification
        ));
    }

    @Nested
    class findValue {

        FinancialStatementEnum fs = FinancialStatementEnum.BALANCE_SHEET;
        Document document = new Document(
                null,
                DocumentTypeCode.DTC_120,
                QuarterType.QT_4,
                null,
                LocalDate.parse("2021-01-01"),
                null,
                null,
                LocalDate.parse("2021-12-01"),
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
        List<Subject> subjectList = List.of(new BsSubject(null, null, null, null));

        @DisplayName("findValue : 値が存在したら値を返す")
        @Test
        void present() {
            when(financialStatementDao.selectByUniqueKey(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Optional.of(new FinancialStatementEntity(
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            100L,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    )));

            var actual = financialStatementSpecification.findValue(fs, document, subjectList);

            assertEquals(100L, actual.orElseThrow());
        }

        @DisplayName("findValue : 値が存在しなかったら空を返す")
        @Test
        void empty() {
            when(financialStatementDao.selectByUniqueKey(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Optional.of(new FinancialStatementEntity(
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
                            null,
                            null
                    )));

            var actual = financialStatementSpecification.findValue(fs, document, subjectList);

            assertNull(actual.orElse(null));
        }
    }

    @Nested
    class parseBsSubjectValue {

        @BeforeEach
        void setUp() {
            when(subjectSpecification.findSubject(any(), any()))
                    .thenReturn(new BsSubject(null, null, null, "name"));
        }

        @DisplayName("parseBsSubjectValue : BSの値だったら返却する")
        @Test
        void present() {
            var entityList = List.of(new FinancialStatementEntity(
                    null,
                    null,
                    null,
                    "1",
                    null,
                    null,
                    null,
                    100L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));

            var actual = financialStatementSpecification.parseBsSubjectValue(entityList);

            assertEquals("name", actual.get(0).getSubject());
            assertEquals(100L, actual.get(0).getValue());
            assertEquals(1, actual.size());
        }

        @DisplayName("parseBsSubjectValue : BSの値でないなら返却しない")
        @Test
        void empty() {
            var entityList = List.of(new FinancialStatementEntity(
                    null,
                    null,
                    null,
                    "2",
                    null,
                    null,
                    null,
                    100L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));

            var actual = financialStatementSpecification.parseBsSubjectValue(entityList);

            assertEquals(0, actual.size());
        }
    }

    @Nested
    class parsePlSubjectValue {

        @BeforeEach
        void setUp() {
            when(subjectSpecification.findSubject(any(), any()))
                    .thenReturn(new PlSubject(null, null, null, "name"));
        }

        @DisplayName("parsePlSubjectValue : PLの値だったら返却する")
        @Test
        void present() {
            var entityList = List.of(new FinancialStatementEntity(
                    null,
                    null,
                    null,
                    "2",
                    null,
                    null,
                    null,
                    100L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));

            var actual = financialStatementSpecification.parsePlSubjectValue(entityList);

            assertEquals("name", actual.get(0).getSubject());
            assertEquals(100L, actual.get(0).getValue());
            assertEquals(1, actual.size());
        }

        @DisplayName("parsePlSubjectValue : PLの値でないなら返却しない")
        @Test
        void empty() {
            var entityList = List.of(new FinancialStatementEntity(
                    null,
                    null,
                    null,
                    "1",
                    null,
                    null,
                    null,
                    100L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));

            var actual = financialStatementSpecification.parsePlSubjectValue(entityList);

            assertEquals(0, actual.size());
        }
    }
}