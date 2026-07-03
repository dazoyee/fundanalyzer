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

@SuppressWarnings("NewClassNamingConvention")
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
            when(financialStatementDao.selectByUniqueKey(any(), any(), any()))
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
            when(financialStatementDao.selectByUniqueKey(any(), any(), any()))
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

    @Nested
    @DisplayName("findValue (subjectList) メソッド")
    class FindValueWithSubjectList {

        private final FinancialStatementEnum fs = FinancialStatementEnum.BALANCE_SHEET;
        private final Document document = new Document(
                null, DocumentTypeCode.DTC_120, QuarterType.QT_4,
                null, LocalDate.parse("2021-01-01"), null, null,
                LocalDate.parse("2021-12-01"), null, null,
                DocumentStatus.DONE, null, DocumentStatus.DONE, null, DocumentStatus.DONE, null, false);

        @DisplayName("findValue : 候補科目を順に検索し最初に見つかった値を返す")
        @Test
        void returnsFirstMatch() {
            final Subject subject1 = new BsSubject("1", null, null, null);
            final Subject subject2 = new BsSubject("2", null, null, null);
            when(financialStatementDao.selectByUniqueKey(any(), any(), org.mockito.ArgumentMatchers.eq("1")))
                    .thenReturn(Optional.empty());
            when(financialStatementDao.selectByUniqueKey(any(), any(), org.mockito.ArgumentMatchers.eq("2")))
                    .thenReturn(Optional.of(new FinancialStatementEntity(
                            null, null, null, null, null, null, null, 200L, null, null, null, null, null, null)));

            final Optional<Long> actual = financialStatementSpecification.findValue(fs, document, List.of(subject1, subject2));

            assertEquals(Optional.of(200L), actual);
        }

        @DisplayName("findValue : 候補科目すべてに値が無ければ空を返す")
        @Test
        void emptyWhenNoMatch() {
            final Subject subject = new BsSubject("1", null, null, null);
            when(financialStatementDao.selectByUniqueKey(any(), any(), any())).thenReturn(Optional.empty());

            final Optional<Long> actual = financialStatementSpecification.findValue(fs, document, List.of(subject));

            org.junit.jupiter.api.Assertions.assertTrue(actual.isEmpty());
        }
    }

    @Nested
    @DisplayName("findValue 例外ハンドリング")
    class FindValueException {

        @DisplayName("findValue : DAO が NestedRuntimeException を投げたら FundanalyzerBadDataException でラップする")
        @Test
        void wrapsAsBadData() {
            final FinancialStatementEnum fs = FinancialStatementEnum.BALANCE_SHEET;
            final Document document = new Document(
                    null, DocumentTypeCode.DTC_120, QuarterType.QT_4,
                    null, LocalDate.parse("2021-01-01"), null, null,
                    LocalDate.parse("2021-12-01"), null, null,
                    DocumentStatus.DONE, null, DocumentStatus.DONE, null, DocumentStatus.DONE, null, false);
            final Subject subject = new BsSubject("1", null, null, null);

            when(financialStatementDao.selectByUniqueKey(any(), any(), any()))
                    .thenThrow(new org.springframework.dao.DuplicateKeyException("dup"));

            org.junit.jupiter.api.Assertions.assertThrows(
                    github.com.ioridazo.fundanalyzer.exception.FundanalyzerBadDataException.class,
                    () -> financialStatementSpecification.findValue(fs, document, subject));
        }
    }

    @Nested
    @DisplayName("findNsValue メソッド")
    class FindNsValue {

        @DisplayName("findNsValue : 株式総数を返す")
        @Test
        void returnsValue() {
            final Document document = new Document(
                    "doc-1", DocumentTypeCode.DTC_120, QuarterType.QT_4,
                    null, null, null, null, null, null, null,
                    DocumentStatus.DONE, null, DocumentStatus.DONE, null, DocumentStatus.DONE, null, false);
            when(financialStatementDao.selectByUniqueKey(
                    org.mockito.ArgumentMatchers.eq("doc-1"),
                    org.mockito.ArgumentMatchers.eq(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.getId()),
                    org.mockito.ArgumentMatchers.eq("0")))
                    .thenReturn(Optional.of(new FinancialStatementEntity(
                            null, null, null, null, null, null, null, 9999L, null, null, null, null, null, null)));

            assertEquals(Optional.of(9999L), financialStatementSpecification.findNsValue(document));
        }
    }

    @Nested
    @DisplayName("findByCompany メソッド")
    class FindByCompany {

        @DisplayName("findByCompany : EDINET コードで DAO を呼び結果を返す")
        @Test
        void delegates() {
            final github.com.ioridazo.fundanalyzer.domain.value.Company company = new github.com.ioridazo.fundanalyzer.domain.value.Company(
                    "1234", "テスト企業", null, null, "edinet1", null, null, null, null, false, false, true);
            when(financialStatementDao.selectByCode("edinet1")).thenReturn(List.of());

            assertEquals(0, financialStatementSpecification.findByCompany(company).size());
        }
    }

    @Nested
    @DisplayName("isPresentTotalInvestmentsAndOtherAssets メソッド")
    class IsPresentTotalInvestmentsAndOtherAssets {

        @DisplayName("isPresent : 投資その他の資産合計が存在する場合 true")
        @Test
        void truthy() {
            final Document document = new Document(
                    "doc-1", DocumentTypeCode.DTC_120, QuarterType.QT_4,
                    null, null, null, null, null, null, null,
                    DocumentStatus.DONE, null, DocumentStatus.DONE, null, DocumentStatus.DONE, null, false);
            when(subjectSpecification.findBsSubjectList(BsSubject.BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS))
                    .thenReturn(List.of(new BsSubject("1", null, null, null)));
            when(financialStatementDao.selectByUniqueKey(any(), any(), any()))
                    .thenReturn(Optional.of(new FinancialStatementEntity(
                            null, null, null, null, null, null, null, 100L, null, null, null, null, null, null)));

            org.junit.jupiter.api.Assertions.assertTrue(
                    financialStatementSpecification.isPresentTotalInvestmentsAndOtherAssets(document));
        }

        @DisplayName("isPresent : 値が存在しない場合 false")
        @Test
        void falsy() {
            final Document document = new Document(
                    "doc-1", DocumentTypeCode.DTC_120, QuarterType.QT_4,
                    null, null, null, null, null, null, null,
                    DocumentStatus.DONE, null, DocumentStatus.DONE, null, DocumentStatus.DONE, null, false);
            when(subjectSpecification.findBsSubjectList(BsSubject.BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS))
                    .thenReturn(List.of(new BsSubject("1", null, null, null)));
            when(financialStatementDao.selectByUniqueKey(any(), any(), any())).thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertFalse(
                    financialStatementSpecification.isPresentTotalInvestmentsAndOtherAssets(document));
        }
    }
}
