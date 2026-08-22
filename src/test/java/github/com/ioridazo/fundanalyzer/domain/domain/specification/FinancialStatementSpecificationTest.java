package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.Subject;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.CreatedType;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.domain.value.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.PlSubject;
import github.com.ioridazo.fundanalyzer.domain.usecase.SystemEventUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("NewClassNamingConvention")
class FinancialStatementSpecificationTest {

    private FinancialStatementDao financialStatementDao;
    private SubjectSpecification subjectSpecification;
    private SystemEventUseCase systemEventUseCase;

    private FinancialStatementSpecification financialStatementSpecification;

    @BeforeEach
    void setUp() {
        financialStatementDao = Mockito.mock(FinancialStatementDao.class);
        subjectSpecification = Mockito.mock(SubjectSpecification.class);
        systemEventUseCase = Mockito.mock(SystemEventUseCase.class);

        financialStatementSpecification = Mockito.spy(new FinancialStatementSpecification(
                financialStatementDao,
                subjectSpecification,
                systemEventUseCase
        ));
        financialStatementSpecification.validationLowerLimitRatio = BigDecimal.valueOf(0.1d);
        financialStatementSpecification.validationUpperLimitRatio = BigDecimal.TEN;
        financialStatementSpecification.validationMinimumAmount = 100_000_000L;
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
                    .thenReturn(new PlSubject(null, null, null, null, "name"));
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

        @DisplayName("findValue : 当期純利益候補が両方あるときは親会社株主帰属を優先する")
        @Test
        void returnsOwnersNetIncomeWhenBothExist() {
            final FinancialStatementEnum pl = FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT;
            final Subject owners = new PlSubject("7", "11", "7", 1, "親会社株主に帰属する当期純利益");
            final Subject consolidated = new PlSubject("1", "11", "1", 2, "当期純利益");
            when(financialStatementDao.selectByUniqueKey(any(), any(), org.mockito.ArgumentMatchers.eq("7")))
                    .thenReturn(Optional.of(new FinancialStatementEntity(
                            null, null, null, null, null, null, null, 100L, null, null, null, null, null, null)));
            when(financialStatementDao.selectByUniqueKey(any(), any(), org.mockito.ArgumentMatchers.eq("1")))
                    .thenReturn(Optional.of(new FinancialStatementEntity(
                            null, null, null, null, null, null, null, 200L, null, null, null, null, null, null)));

            final Optional<Long> actual = financialStatementSpecification.findValue(pl, document, List.of(owners, consolidated));

            assertEquals(Optional.of(100L), actual);
        }

        @DisplayName("findValue : 親会社株主帰属が無いときは連結全体の当期純利益を返す")
        @Test
        void returnsConsolidatedNetIncomeWhenOwnersMissing() {
            final FinancialStatementEnum pl = FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT;
            final Subject owners = new PlSubject("7", "11", "7", 1, "親会社株主に帰属する当期純利益");
            final Subject consolidated = new PlSubject("1", "11", "1", 2, "当期純利益");
            when(financialStatementDao.selectByUniqueKey(any(), any(), org.mockito.ArgumentMatchers.eq("7")))
                    .thenReturn(Optional.empty());
            when(financialStatementDao.selectByUniqueKey(any(), any(), org.mockito.ArgumentMatchers.eq("1")))
                    .thenReturn(Optional.of(new FinancialStatementEntity(
                            null, null, null, null, null, null, null, 200L, null, null, null, null, null, null)));

            final Optional<Long> actual = financialStatementSpecification.findValue(pl, document, List.of(owners, consolidated));

            assertEquals(Optional.of(200L), actual);
        }
    }

    @Nested
    class insert {

        private final Company company = new Company(
                "1234",
                "company",
                null,
                null,
                "E12345",
                null,
                null,
                null,
                null,
                false,
                false,
                false
        );
        private final Document document = new Document(
                "DOC001",
                DocumentTypeCode.DTC_120,
                QuarterType.QT_4,
                "E12345",
                LocalDate.parse("2026-03-31"),
                LocalDate.parse("2026-06-30"),
                LocalDate.parse("2025-04-01"),
                LocalDate.parse("2026-03-31"),
                DocumentStatus.DONE,
                DocumentStatus.DONE,
                DocumentStatus.DONE,
                null,
                DocumentStatus.DONE,
                null,
                DocumentStatus.DONE,
                null,
                false
        );

        @DisplayName("insert : 比率が閾値内なら登録してSystemEvent記録しない")
        @Test
        void noWarningWithinThreshold() {
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of(
                    previousEntity(FinancialStatementEnum.BALANCE_SHEET, "1", 100_000_000L, LocalDate.parse("2025-03-31"))));

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company, FinancialStatementEnum.BALANCE_SHEET, "1", document, 100_000_000L, CreatedType.AUTO));

            verify(financialStatementDao, times(1)).insert(any(FinancialStatementEntity.class));
            verify(systemEventUseCase, never()).record(any(), anyString(), anyString());
        }

        @DisplayName("insert : 下限閾値ちょうどなら警告しない")
        @Test
        void noWarningAtLowerBoundary() {
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of(
                    previousEntity(FinancialStatementEnum.BALANCE_SHEET, "1", 1_000_000_000L, LocalDate.parse("2025-03-31"))));

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company, FinancialStatementEnum.BALANCE_SHEET, "1", document, 100_000_000L, CreatedType.AUTO));

            verify(systemEventUseCase, never()).record(any(), anyString(), anyString());
        }

        @DisplayName("insert : 上限閾値ちょうどなら警告しない")
        @Test
        void noWarningAtUpperBoundary() {
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of(
                    previousEntity(FinancialStatementEnum.BALANCE_SHEET, "1", 100_000_000L, LocalDate.parse("2025-03-31"))));

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company, FinancialStatementEnum.BALANCE_SHEET, "1", document, 1_000_000_000L, CreatedType.AUTO));

            verify(systemEventUseCase, never()).record(any(), anyString(), anyString());
        }

        @DisplayName("insert : 下限未満なら警告付きで登録してSystemEventを記録する")
        @Test
        void warningAtLowerOutsideBoundary() {
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of(
                    previousEntity(FinancialStatementEnum.BALANCE_SHEET, "1", 1_000_000_000L, LocalDate.parse("2025-03-31"))));

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company, FinancialStatementEnum.BALANCE_SHEET, "1", document, 99_000_000L, CreatedType.AUTO));

            verify(financialStatementDao, times(1)).insert(any(FinancialStatementEntity.class));
            verify(systemEventUseCase, times(1)).record(
                    eq(SystemEventType.WARNING),
                    eq("FinancialStatementSpecification"),
                    eq("企業コード:1234 EDINET:E12345 財務諸表:" + FinancialStatementEnum.BALANCE_SHEET.getName()
                            + " 科目ID:1 書類ID:DOC001 当期末:2026-03-31 前回期末:2025-03-31 前回値:1,000,000,000 今回値:99,000,000 比率:0.099")
            );
        }

        @DisplayName("insert : 上限超なら警告付きで登録してSystemEventを記録する")
        @Test
        void warningAtUpperOutsideBoundary() {
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of(
                    previousEntity(FinancialStatementEnum.BALANCE_SHEET, "1", 100_000_000L, LocalDate.parse("2025-03-31"))));

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company, FinancialStatementEnum.BALANCE_SHEET, "1", document, 1_001_000_000L, CreatedType.AUTO));

            verify(systemEventUseCase, times(1)).record(
                    eq(SystemEventType.WARNING),
                    eq("FinancialStatementSpecification"),
                    eq("企業コード:1234 EDINET:E12345 財務諸表:" + FinancialStatementEnum.BALANCE_SHEET.getName()
                            + " 科目ID:1 書類ID:DOC001 当期末:2026-03-31 前回期末:2025-03-31 前回値:100,000,000 今回値:1,001,000,000 比率:10.01")
            );
        }

        @DisplayName("insert : 前回黒字→今回赤字など符号反転なら比率判定せず警告しない")
        @Test
        void noWarningOnSignReversal() {
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of(
                    previousEntity(FinancialStatementEnum.BALANCE_SHEET, "1", 1_000_000_000L, LocalDate.parse("2025-03-31"))));

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company, FinancialStatementEnum.BALANCE_SHEET, "1", document, -500_000_000L, CreatedType.AUTO));

            verify(financialStatementDao, times(1)).insert(any(FinancialStatementEntity.class));
            verify(systemEventUseCase, never()).record(any(), anyString(), anyString());
        }

        @DisplayName("insert : 初回登録は比較せず登録する")
        @Test
        void skipValidationWhenPreviousValueMissing() {
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of());

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company, FinancialStatementEnum.BALANCE_SHEET, "1", document, 100_000_000L, CreatedType.AUTO));

            verify(financialStatementDao, times(1)).insert(any(FinancialStatementEntity.class));
            verify(systemEventUseCase, never()).record(any(), anyString(), anyString());
        }

        @DisplayName("insert : 前回0で今回非0なら警告付きで登録してSystemEventを記録する")
        @Test
        void warningWhenPreviousZero() {
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of(
                    previousEntity(FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES, "0", 0L, LocalDate.parse("2025-03-31"))));

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company, FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES, "0", document, 100_000_000L, CreatedType.AUTO));

            verify(systemEventUseCase, times(1)).record(
                    eq(SystemEventType.WARNING),
                    eq("FinancialStatementSpecification"),
                    eq("企業コード:1234 EDINET:E12345 財務諸表:" + FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.getName()
                            + " 科目ID:0 書類ID:DOC001 当期末:2026-03-31 前回期末:2025-03-31 前回値:0 今回値:100,000,000 比率:INF"
                            + " （参考: 株式総数は提出日現在発行数を優先して採用する仕様のため、"
                            + "期末後の株式分割・株式併合・新株発行による想定内の乖離である可能性があります）")
            );
        }

        @DisplayName("insertWithoutValidation : 補完登録では乖離チェックもSystemEvent記録も行わない")
        @Test
        void skipValidationForSupplementalInsert() {
            assertDoesNotThrow(() -> financialStatementSpecification.insertWithoutValidation(
                    company, FinancialStatementEnum.BALANCE_SHEET, "1", document, 0L, CreatedType.AUTO));

            verify(financialStatementDao, times(1)).insert(any(FinancialStatementEntity.class));
            verify(financialStatementDao, never()).selectByCode(anyString());
            verify(systemEventUseCase, never()).record(any(), anyString(), anyString());
        }

        @DisplayName("insert : フロー項目で会計期間長が異なる前回値は比較対象外とし警告しない")
        @Test
        void noWarningWhenFlowStatementPeriodLengthDiffers() {
            // 通期12ヶ月の損益計算書に対し、中間6ヶ月の値しか存在しないケース
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of(
                    previousEntity(
                            FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                            "1",
                            100_000_000L,
                            LocalDate.parse("2025-04-01"),
                            LocalDate.parse("2025-09-30"))));

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company,
                    FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                    "1",
                    document,
                    1_001_000_000L,
                    CreatedType.AUTO));

            verify(financialStatementDao, times(1)).insert(any(FinancialStatementEntity.class));
            verify(systemEventUseCase, never()).record(any(), anyString(), anyString());
        }

        @DisplayName("insert : フロー項目で会計期間長が同等なら閾値外で警告する")
        @Test
        void warningWhenFlowStatementPeriodLengthMatches() {
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of(
                    previousEntity(
                            FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                            "1",
                            100_000_000L,
                            LocalDate.parse("2024-04-01"),
                            LocalDate.parse("2025-03-31"))));

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company,
                    FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                    "1",
                    document,
                    1_001_000_000L,
                    CreatedType.AUTO));

            verify(systemEventUseCase, times(1)).record(
                    eq(SystemEventType.WARNING), eq("FinancialStatementSpecification"), anyString());
        }

        @DisplayName("insert : ストック項目は会計期間長が異なっても閾値外で警告する")
        @Test
        void warningWhenStockStatementPeriodLengthDiffers() {
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of(
                    previousEntity(
                            FinancialStatementEnum.BALANCE_SHEET,
                            "1",
                            100_000_000L,
                            LocalDate.parse("2025-04-01"),
                            LocalDate.parse("2025-09-30"))));

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company, FinancialStatementEnum.BALANCE_SHEET, "1", document, 1_001_000_000L, CreatedType.AUTO));

            verify(systemEventUseCase, times(1)).record(
                    eq(SystemEventType.WARNING), eq("FinancialStatementSpecification"), anyString());
        }

        @DisplayName("insert : 前回値・今回値がともに重要性の閾値未満なら警告しない")
        @Test
        void noWarningWhenAmountBelowMinimum() {
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of(
                    previousEntity(FinancialStatementEnum.BALANCE_SHEET, "1", 1_000_000L, LocalDate.parse("2025-03-31"))));

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company, FinancialStatementEnum.BALANCE_SHEET, "1", document, 90_000_000L, CreatedType.AUTO));

            verify(financialStatementDao, times(1)).insert(any(FinancialStatementEntity.class));
            verify(systemEventUseCase, never()).record(any(), anyString(), anyString());
        }

        @DisplayName("insert : いずれかが重要性の閾値以上なら閾値外で警告する")
        @Test
        void warningWhenAmountReachesMinimum() {
            when(financialStatementDao.selectByCode("E12345")).thenReturn(List.of(
                    previousEntity(FinancialStatementEnum.BALANCE_SHEET, "1", 1_000_000L, LocalDate.parse("2025-03-31"))));

            assertDoesNotThrow(() -> financialStatementSpecification.insert(
                    company, FinancialStatementEnum.BALANCE_SHEET, "1", document, 100_000_000L, CreatedType.AUTO));

            verify(systemEventUseCase, times(1)).record(
                    eq(SystemEventType.WARNING), eq("FinancialStatementSpecification"), anyString());
        }

        private FinancialStatementEntity previousEntity(
                final FinancialStatementEnum fs,
                final String subjectId,
                final Long value,
                final LocalDate periodEnd) {
            return previousEntity(fs, subjectId, value, LocalDate.parse("2024-04-01"), periodEnd);
        }

        private FinancialStatementEntity previousEntity(
                final FinancialStatementEnum fs,
                final String subjectId,
                final Long value,
                final LocalDate periodStart,
                final LocalDate periodEnd) {
            return new FinancialStatementEntity(
                    1,
                    "1234",
                    "E12345",
                    fs.getId(),
                    subjectId,
                    periodStart,
                    periodEnd,
                    value,
                    DocumentTypeCode.DTC_120.toValue(),
                    QuarterType.QT_4.toValue(),
                    LocalDate.parse("2025-06-30"),
                    "DOC000",
                    CreatedType.AUTO.toValue(),
                    null
            );
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
