package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.InvestmentIndicatorDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.InvestmentIndicatorEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.seasar.doma.jdbc.Sql;
import org.seasar.doma.jdbc.SqlLogType;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("InvestmentIndicatorSpecification のテスト")
class InvestmentIndicatorSpecificationTest {

    private InvestmentIndicatorDao dao;
    private CompanySpecification companySpecification;
    private CorporateActionSpecification corporateActionSpecification;
    private InvestmentIndicatorSpecification specification;

    @BeforeEach
    void setUp() {
        dao = mock(InvestmentIndicatorDao.class);
        companySpecification = mock(CompanySpecification.class);
        corporateActionSpecification = mock(CorporateActionSpecification.class);
        when(corporateActionSpecification.adjustToBasis(any(), any(), any(), any(), eq(true)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        specification = spy(new InvestmentIndicatorSpecification(dao, companySpecification, corporateActionSpecification));
        doReturn(LocalDateTime.parse("2024-04-01T00:00:00")).when(specification).nowLocalDateTime();
    }

    private InvestmentIndicatorEntity entity(final String code, final LocalDate targetDate) {
        return InvestmentIndicatorEntity.of(
                1, 2, code, targetDate,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                "doc-1", LocalDateTime.parse("2024-04-01T00:00:00"));
    }

    private AnalysisResultEntity analysisResult() {
        return analysisResult(BigDecimal.valueOf(1500), BigDecimal.valueOf(120));
    }

    private AnalysisResultEntity analysisResult(final BigDecimal bps, final BigDecimal eps) {
        return new AnalysisResultEntity(
                10, "1234", LocalDate.parse("2024-03-01"),
                BigDecimal.valueOf(3000), null,
                "120", "year-1",
                LocalDate.parse("2024-04-01"), "doc-1",
                LocalDateTime.parse("2024-04-01T00:00:00"));
    }

    private AnalysisResult computedResult(final BigDecimal bps, final BigDecimal eps) {
        return new AnalysisResult(
                BigDecimal.valueOf(3000), null, bps, eps, null, null,
                LocalDate.parse("2024-04-01"), "doc-1");
    }

    private StockPriceEntity stockPrice() {
        return new StockPriceEntity(
                20, "1234", LocalDate.parse("2024-04-01"),
                500.0, null, null, null, null, null, null, null, null, null, null, null,
                "import-source",
                LocalDateTime.parse("2024-04-01T00:00:00"),
                LocalDateTime.parse("2024-04-01T00:00:00"));
    }

    @Nested
    @DisplayName("findIndicatorValue メソッド")
    class FindIndicatorValue {

        @DisplayName("findIndicatorValue : 対象企業の最新 targetDate の指標を返す")
        @Test
        void returnsLatest() {
            final InvestmentIndicatorEntity older = entity("1234", LocalDate.parse("2024-01-01"));
            final InvestmentIndicatorEntity newer = entity("1234", LocalDate.parse("2024-04-01"));
            when(dao.selectByCode("1234")).thenReturn(List.of(older, newer));

            final Optional<IndicatorValue> actual = specification.findIndicatorValue("1234");

            assertTrue(actual.isPresent());
        }

        @DisplayName("findIndicatorValue : エンティティが空のときは Optional.empty を返す")
        @Test
        void emptyWhenNoEntity() {
            when(dao.selectByCode("1234")).thenReturn(List.of());
            assertTrue(specification.findIndicatorValue("1234").isEmpty());
        }
    }

    @Nested
    @DisplayName("findEntity メソッド")
    class FindEntity {

        @DisplayName("findEntity : DAO にそのまま委譲する")
        @Test
        void delegates() {
            final InvestmentIndicatorEntity ent = entity("1234", LocalDate.parse("2024-04-01"));
            when(dao.selectByCodeAndTargetDate("1234", LocalDate.parse("2024-04-01")))
                    .thenReturn(Optional.of(ent));

            final Optional<InvestmentIndicatorEntity> actual = specification.findEntity("1234", LocalDate.parse("2024-04-01"));

            assertTrue(actual.isPresent());
        }
    }

    @Nested
    @DisplayName("findIndicatorValueList メソッド")
    class FindIndicatorValueList {

        @DisplayName("findIndicatorValueList(code) : DAO の戻り値を IndicatorValue に変換して返す")
        @Test
        void byCode() {
            when(dao.selectByCode("1234")).thenReturn(
                    List.of(entity("1234", LocalDate.parse("2024-04-01"))));

            final List<IndicatorValue> actual = specification.findIndicatorValueList("1234");

            assertEquals(1, actual.size());
        }

        @DisplayName("findIndicatorValueList(analysisResultId) : DAO の戻り値を IndicatorValue に変換して返す")
        @Test
        void byAnalysisResultId() {
            when(dao.selectByAnalysisResultId(2)).thenReturn(
                    List.of(entity("1234", LocalDate.parse("2024-04-01"))));

            final List<IndicatorValue> actual = specification.findIndicatorValueList(2);

            assertEquals(1, actual.size());
        }
    }

    @Nested
    @DisplayName("insert メソッド")
    class Insert {

        @DisplayName("insert : 正常時はエンティティを DAO に登録する")
        @Test
        void insertsNormally() {
            specification.insert(analysisResult(), computedResult(BigDecimal.valueOf(1500), BigDecimal.valueOf(120)), stockPrice());

            verify(dao, times(1)).insert(any(InvestmentIndicatorEntity.class));
        }

        @DisplayName("insert : 補正後株価で PER を計算する")
        @Test
        void usesAdjustedStockPriceForPer() {
            when(corporateActionSpecification.adjustToBasis(
                    any(),
                    eq("1234"),
                    eq(LocalDate.parse("2024-04-01")),
                    eq(LocalDate.parse("2024-04-01")),
                    eq(true)))
                    .thenReturn(BigDecimal.valueOf(1000.0));

            specification.insert(analysisResult(), computedResult(BigDecimal.valueOf(1500), BigDecimal.valueOf(120)), stockPrice());

            final ArgumentCaptor<InvestmentIndicatorEntity> captor =
                    ArgumentCaptor.forClass(InvestmentIndicatorEntity.class);
            verify(dao).insert(captor.capture());

            final InvestmentIndicatorEntity actual = captor.getValue();
            assertEquals(0, BigDecimal.valueOf(8.3333333333).compareTo(actual.getPer().orElseThrow()));
        }

        @DisplayName("insert : EPSが負のときは PER とグレアム指数を保存しない")
        @Test
        void doesNotSavePerOrGrahamIndexWhenEpsIsNegative() {
            specification.insert(analysisResult(BigDecimal.valueOf(1500), BigDecimal.valueOf(-120)), computedResult(BigDecimal.valueOf(1500), BigDecimal.valueOf(-120)), stockPrice());

            final ArgumentCaptor<InvestmentIndicatorEntity> captor =
                    ArgumentCaptor.forClass(InvestmentIndicatorEntity.class);
            verify(dao).insert(captor.capture());

            final InvestmentIndicatorEntity actual = captor.getValue();
            assertTrue(actual.getPer().isEmpty());
            assertTrue(actual.getGrahamIndex().isEmpty());
            assertEquals(0, BigDecimal.valueOf(0.3333333333).compareTo(actual.getPbr().orElseThrow()));
        }

        @DisplayName("insert : EPSがゼロのときは PER とグレアム指数を保存しない")
        @Test
        void doesNotSavePerOrGrahamIndexWhenEpsIsZero() {
            specification.insert(analysisResult(BigDecimal.valueOf(1500), BigDecimal.ZERO), computedResult(BigDecimal.valueOf(1500), BigDecimal.ZERO), stockPrice());

            final ArgumentCaptor<InvestmentIndicatorEntity> captor =
                    ArgumentCaptor.forClass(InvestmentIndicatorEntity.class);
            verify(dao).insert(captor.capture());

            final InvestmentIndicatorEntity actual = captor.getValue();
            assertTrue(actual.getPer().isEmpty());
            assertTrue(actual.getGrahamIndex().isEmpty());
            assertEquals(0, BigDecimal.valueOf(0.3333333333).compareTo(actual.getPbr().orElseThrow()));
        }

        @DisplayName("insert : PBRが負のときは PBR は保存しグレアム指数は保存しない")
        @Test
        void savesNegativePbrButNotGrahamIndex() {
            specification.insert(analysisResult(BigDecimal.valueOf(-1500), BigDecimal.valueOf(120)), computedResult(BigDecimal.valueOf(-1500), BigDecimal.valueOf(120)), stockPrice());

            final ArgumentCaptor<InvestmentIndicatorEntity> captor =
                    ArgumentCaptor.forClass(InvestmentIndicatorEntity.class);
            verify(dao).insert(captor.capture());

            final InvestmentIndicatorEntity actual = captor.getValue();
            assertEquals(0, BigDecimal.valueOf(4.1666666667).compareTo(actual.getPer().orElseThrow()));
            assertEquals(0, BigDecimal.valueOf(-0.3333333333).compareTo(actual.getPbr().orElseThrow()));
            assertTrue(actual.getGrahamIndex().isEmpty());
        }

        @DisplayName("insert : PER と PBR が両方負相当のときもグレアム指数を保存しない")
        @Test
        void doesNotSaveGrahamIndexWhenPerAndPbrWouldBothBeNegative() {
            specification.insert(analysisResult(BigDecimal.valueOf(-1500), BigDecimal.valueOf(-120)), computedResult(BigDecimal.valueOf(-1500), BigDecimal.valueOf(-120)), stockPrice());

            final ArgumentCaptor<InvestmentIndicatorEntity> captor =
                    ArgumentCaptor.forClass(InvestmentIndicatorEntity.class);
            verify(dao).insert(captor.capture());

            final InvestmentIndicatorEntity actual = captor.getValue();
            assertTrue(actual.getPer().isEmpty());
            assertEquals(0, BigDecimal.valueOf(-0.3333333333).compareTo(actual.getPbr().orElseThrow()));
            assertTrue(actual.getGrahamIndex().isEmpty());
        }

        @DisplayName("insert : UniqueConstraintException 発生時はログ出力で握りつぶす")
        @Test
        void swallowsUniqueConstraint() {
            doThrow(new DuplicateKeyException("dup",
                    new UniqueConstraintException(SqlLogType.FORMATTED, mock(Sql.class), null)))
                    .when(dao).insert(any(InvestmentIndicatorEntity.class));
            when(companySpecification.findCompanyByCode("1234")).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> specification.insert(analysisResult(), computedResult(BigDecimal.valueOf(1500), BigDecimal.valueOf(120)), stockPrice()));
        }

        @DisplayName("insert : SQLIntegrityConstraintViolation 発生時はログ出力で握りつぶす")
        @Test
        void swallowsIntegrityViolation() {
            doThrow(new DuplicateKeyException("integrity", new SQLIntegrityConstraintViolationException("fk")))
                    .when(dao).insert(any(InvestmentIndicatorEntity.class));
            when(companySpecification.findCompanyByCode("1234")).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> specification.insert(analysisResult(), computedResult(BigDecimal.valueOf(1500), BigDecimal.valueOf(120)), stockPrice()));
        }

        @DisplayName("insert : それ以外の NestedRuntimeException はそのまま再送出する")
        @Test
        void rethrowsOtherException() {
            final DuplicateKeyException other = new DuplicateKeyException("other");
            doThrow(other).when(dao).insert(any(InvestmentIndicatorEntity.class));

            final Throwable thrown = assertThrows(DuplicateKeyException.class,
                    () -> specification.insert(analysisResult(), computedResult(BigDecimal.valueOf(1500), BigDecimal.valueOf(120)), stockPrice()));
            assertEquals(other, thrown);
        }
    }
}
