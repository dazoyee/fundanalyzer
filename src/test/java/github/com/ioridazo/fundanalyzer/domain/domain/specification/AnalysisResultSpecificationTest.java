package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.seasar.doma.jdbc.Sql;
import org.seasar.doma.jdbc.SqlLogType;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisResultSpecificationTest {

    private static final List<String> annualStatTypeCodes = List.of("120", "130");
    private static final List<String> latestTypeCodes = List.of("120", "130", "140", "150", "160", "170");

    private AnalysisResultDao analysisResultDao;
    private CompanySpecification companySpecification;

    private AnalysisResultSpecification analysisResultSpecification;

    @BeforeEach
    void setUp() {
        analysisResultDao = Mockito.mock(AnalysisResultDao.class);
        companySpecification = Mockito.mock(CompanySpecification.class);

        analysisResultSpecification = Mockito.spy(new AnalysisResultSpecification(
                analysisResultDao,
                companySpecification
        ));
        analysisResultSpecification.annualStatTypeCodes = annualStatTypeCodes;
        analysisResultSpecification.latestTypeCodes = latestTypeCodes;
    }

    @Nested
    class yearAverageCorporateValue {

        private final Integer THREE = 3;

        @DisplayName("yearAverageCorporateValue : 平均の企業価値を取得する")
        @Test
        void present() {
            var analysisResult1 = new AnalysisResultEntity(1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(500), null, "120", "4", null, null, null);
            var analysisResult2 = new AnalysisResultEntity(2, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(1100), null, "120", "4", null, null, null);
            var analysisResult3 = new AnalysisResultEntity(3, "code", LocalDate.parse("2018-06-30"), BigDecimal.valueOf(1400), null, "120", "4", null, null, null);
            var analysisResult4 = new AnalysisResultEntity(4, "code", LocalDate.parse("2017-06-30"), BigDecimal.valueOf(10000), null, "120", "4", null, null, null);
            doReturn(List.of(analysisResult1, analysisResult2, analysisResult3, analysisResult4))
                    .when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.yearAverageCorporateValue(company, THREE);
            assertEquals(BigDecimal.valueOf(100000, 2), actual.orElseThrow());
        }

        @DisplayName("yearAverageCorporateValue : 企業価値がないときは空で返却する")
        @Test
        void empty() {
            doReturn(List.of()).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.yearAverageCorporateValue(company, THREE);
            assertNull(actual.orElse(null));
        }

        @DisplayName("yearAverageCorporateValue : 企業価値がないときは空で返却する")
        @Test
        void not_enough() {
            var analysisResult1 = new AnalysisResultEntity(1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(500), null, "120", "4", null, null, null);
            var analysisResult2 = new AnalysisResultEntity(2, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(1100), null, "120", "4", null, null, null);
            doReturn(List.of(analysisResult1, analysisResult2))
                    .when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.yearAverageCorporateValue(company, THREE);
            assertNull(actual.orElse(null));
        }

        @DisplayName("yearAverageCorporateValue : 小数点以下表示を確認する")
        @Test
        void scale() {
            var analysisResult1 = new AnalysisResultEntity(
                    1,
                    "code",
                    LocalDate.parse("2020-06-30"),
                    BigDecimal.valueOf(500.250515),
                    null,
                    DocumentTypeCode.DTC_120.toValue(),
                    QuarterType.QT_4.toValue(),
                    LocalDate.parse("2020-09-30"),
                    null,
                    null
            );
            doReturn(List.of(analysisResult1)).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.yearAverageCorporateValue(company, 1);
            assertEquals(BigDecimal.valueOf(500.25), actual.orElseThrow());
        }
    }

    @Nested
    class allYearAverageCorporateValue {

        @DisplayName("allYearAverageCorporateValue : 平均の企業価値を取得する")
        @Test
        void present() {
            var analysisResult1 = new AnalysisResultEntity(1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(900), null, "120", "4", null, null, null);
            var analysisResult2 = new AnalysisResultEntity(2, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(1100), null, "120", "4", null, null, null);
            doReturn(List.of(analysisResult1, analysisResult2)).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.allYearAverageCorporateValue(company);
            assertEquals(BigDecimal.valueOf(100000, 2), actual.orElseThrow());
        }

        @DisplayName("allYearAverageCorporateValue : 企業価値がないときは空で返却する")
        @Test
        void empty() {
            doReturn(List.of()).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.allYearAverageCorporateValue(company);
            assertNull(actual.orElse(null));
        }

        @DisplayName("allYearAverageCorporateValue : 小数点以下表示を確認する")
        @Test
        void scale() {
            var analysisResult1 = new AnalysisResultEntity(
                    1,
                    "code",
                    LocalDate.parse("2020-06-30"),
                    BigDecimal.valueOf(500.250515),
                    null,
                    DocumentTypeCode.DTC_120.toValue(),
                    QuarterType.QT_4.toValue(),
                    LocalDate.parse("2020-09-30"),
                    null,
                    null
            );
            doReturn(List.of(analysisResult1)).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.allYearAverageCorporateValue(company);
            assertEquals(BigDecimal.valueOf(500.25), actual.orElseThrow());
        }
    }

    @Nested
    class standardDeviation {

        @DisplayName("standardDeviation : 企業価値の標準偏差を取得する")
        @Test
        void present() {
            var analysisResult1 = new AnalysisResultEntity(1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(1100), null, "120", "4", null, null, null);
            var analysisResult2 = new AnalysisResultEntity(2, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(900), null, "120", "4", null, null, null);
            doReturn(List.of(analysisResult1, analysisResult2)).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.standardDeviation(company, BigDecimal.valueOf(100000, 2));
            assertEquals(BigDecimal.valueOf(100.0), actual.orElseThrow());
        }

        @DisplayName("standardDeviation : 企業価値がないときは空で返却する")
        @Test
        void empty() {
            doReturn(List.of()).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.standardDeviation(company, BigDecimal.valueOf(100000, 2));
            assertNull(actual.orElse(null));
        }

        @DisplayName("standardDeviation : 平均の企業価値がないときは空で返却する")
        @Test
        void averageCorporateValue_isNull() {
            doReturn(List.of()).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.standardDeviation(company, null);
            assertNull(actual.orElse(null));
        }

        @DisplayName("standardDeviation : 小数点以下表示を確認する")
        @Test
        void scale() {
            var analysisResult1 = new AnalysisResultEntity(
                    1,
                    "code",
                    LocalDate.parse("2020-06-30"),
                    BigDecimal.valueOf(500.250515),
                    null,
                    DocumentTypeCode.DTC_120.toValue(),
                    QuarterType.QT_4.toValue(),
                    LocalDate.parse("2020-09-30"),
                    null,
                    null
            );
            doReturn(List.of(analysisResult1)).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.standardDeviation(company, BigDecimal.valueOf(500.25));
            assertEquals(BigDecimal.valueOf(0, 1), actual.orElseThrow());
        }
    }

    @Nested
    class coefficientOfVariation {

        @DisplayName("coefficientOfVariation : 変動係数を取得する")
        @Test
        void present() {
            var actual = analysisResultSpecification.coefficientOfVariation(BigDecimal.valueOf(100.0), BigDecimal.valueOf(100000, 2));
            assertEquals(BigDecimal.valueOf(100, 3), actual.orElseThrow());
        }

        @DisplayName("coefficientOfVariation : 標準偏差がないときは空で返却する")
        @Test
        void coefficientOfVariation_isNull() {
            doReturn(List.of()).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.coefficientOfVariation(null, BigDecimal.valueOf(100000, 2));
            assertNull(actual.orElse(null));
        }

        @DisplayName("coefficientOfVariation : 平均の企業価値がないときは空で返却する")
        @Test
        void averageCorporateValue_isNull() {
            doReturn(List.of()).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.coefficientOfVariation(BigDecimal.valueOf(100.0), null);
            assertNull(actual.orElse(null));
        }

        @DisplayName("coefficientOfVariation : 小数点以下表示を確認する")
        @Test
        void scale() {
            var actual = analysisResultSpecification.coefficientOfVariation(BigDecimal.valueOf(0, 1), BigDecimal.valueOf(500.25));
            assertEquals(BigDecimal.valueOf(0, 3), actual.orElseThrow());
        }
    }

    @Nested
    class countYear {

        @DisplayName("countYear : 分析年数を取得する")
        @Test
        void present() {
            var analysisResult1 = new AnalysisResultEntity(1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(1100), null, "120", "4", null, null, null);
            var analysisResult2 = new AnalysisResultEntity(2, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(900), null, "120", "4", null, null, null);
            doReturn(List.of(analysisResult1, analysisResult2)).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.countYear(company);
            assertEquals(BigDecimal.valueOf(2), actual);
        }

        @DisplayName("countYear : 企業価値がないときは0で返却する")
        @Test
        void empty() {
            doReturn(List.of()).when(analysisResultSpecification).analysisTargetList("code", annualStatTypeCodes);

            var actual = analysisResultSpecification.countYear(company);
            assertEquals(BigDecimal.valueOf(0), actual);
        }
    }

    @Nested
    class isAnalyzed {

        Company company = new Company(
                "code",
                null,
                null,
                null,
                "edinetCode",
                null,
                null,
                null,
                null,
                false,
                false,
                false
        );

        @DisplayName("isAnalyzed : true")
        @Test
        void boolean_true() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            when(analysisResultDao.selectByUniqueKey(any(), any(), any(), any()))
                    .thenReturn(Optional.of(new AnalysisResultEntity(null, null, null, null, null, null, null, null, null, null)));

            assertTrue(analysisResultSpecification.isAnalyzed(new Document(
                    null,
                    DocumentTypeCode.DTC_120,
                    QuarterType.QT_4,
                    "edinetCode",
                    LocalDate.parse("2021-01-01"),
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
            )));
        }

        @DisplayName("isAnalyzed : false")
        @Test
        void boolean_false_1() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));

            assertFalse(analysisResultSpecification.isAnalyzed(new Document(
                    null,
                    DocumentTypeCode.DTC_120,
                    QuarterType.QT_4,
                    "edinetCode",
                    LocalDate.parse("2021-01-01"),
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
            )));
        }

        @DisplayName("isAnalyzed : false")
        @Test
        void boolean_false_2() {
            assertFalse(analysisResultSpecification.isAnalyzed(new Document(
                    null,
                    DocumentTypeCode.DTC_120,
                    QuarterType.QT_4,
                    "edinetCode",
                    LocalDate.parse("2021-01-01"),
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
            )));
        }

        @DisplayName("isAnalyzed : false")
        @Test
        void boolean_false_3() {
            assertFalse(analysisResultSpecification.isAnalyzed(new Document(
                    null,
                    DocumentTypeCode.DTC_120,
                    QuarterType.QT_4,
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
            )));
        }
    }

    private final Company company = new Company(
            "code",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            true
    );

    @Nested
    @DisplayName("findAnalysisResult のテスト")
    class FindAnalysisResult {

        @DisplayName("findAnalysisResult(Integer) : ID で分析結果が取得できる")
        @Test
        void byId_present() {
            final AnalysisResultEntity entity = new AnalysisResultEntity(
                    10, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(500),
                    null, "120", "4", null, null, null);
            when(analysisResultDao.selectById(10)).thenReturn(Optional.of(entity));

            final Optional<AnalysisResultEntity> actual = analysisResultSpecification.findAnalysisResult(10);

            assertTrue(actual.isPresent());
            assertSame(entity, actual.get());
        }

        @DisplayName("findAnalysisResult(Integer) : 該当なしのときは空で返却する")
        @Test
        void byId_empty() {
            when(analysisResultDao.selectById(99)).thenReturn(Optional.empty());

            final Optional<AnalysisResultEntity> actual = analysisResultSpecification.findAnalysisResult(99);

            assertTrue(actual.isEmpty());
        }

        @DisplayName("findAnalysisResult(String) : ドキュメントIDで分析結果が取得できる")
        @Test
        void byDocumentId_present() {
            final AnalysisResultEntity entity = new AnalysisResultEntity(
                    1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(500),
                    null, "120", "4", null, "doc-1", null);
            when(analysisResultDao.selectByDocumentId("doc-1")).thenReturn(Optional.of(entity));

            final Optional<AnalysisResultEntity> actual = analysisResultSpecification.findAnalysisResult("doc-1");

            assertTrue(actual.isPresent());
            assertEquals("doc-1", actual.get().getDocumentId());
        }

        @DisplayName("findAnalysisResult(String) : 該当なしのときは空で返却する")
        @Test
        void byDocumentId_empty() {
            when(analysisResultDao.selectByDocumentId("missing")).thenReturn(Optional.empty());

            final Optional<AnalysisResultEntity> actual = analysisResultSpecification.findAnalysisResult("missing");

            assertTrue(actual.isEmpty());
        }
    }

    @Nested
    @DisplayName("findLatestAnalysisResult のテスト")
    class FindLatestAnalysisResult {

        @DisplayName("findLatestAnalysisResult : documentPeriod が最新のものを返却する")
        @Test
        void latestByDocumentPeriod() {
            final AnalysisResultEntity older = new AnalysisResultEntity(
                    1, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(900),
                    null, "120", "4", LocalDate.parse("2019-09-30"), null, null);
            final AnalysisResultEntity newer = new AnalysisResultEntity(
                    2, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(1100),
                    null, "120", "4", LocalDate.parse("2020-09-30"), null, null);
            doReturn(List.of(older, newer))
                    .when(analysisResultSpecification).analysisTargetList("code", latestTypeCodes);

            final Optional<AnalysisResultEntity> actual = analysisResultSpecification.findLatestAnalysisResult("code");

            assertTrue(actual.isPresent());
            assertEquals(2, actual.get().getId());
        }

        @DisplayName("findLatestAnalysisResult : documentPeriod 同一なら submitDate が新しいものを返却する")
        @Test
        void tieBreakBySubmitDate() {
            final AnalysisResultEntity earlySubmit = new AnalysisResultEntity(
                    1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(900),
                    null, "120", "4", LocalDate.parse("2020-09-30"), null, null);
            final AnalysisResultEntity lateSubmit = new AnalysisResultEntity(
                    2, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(1100),
                    null, "120", "4", LocalDate.parse("2020-12-31"), null, null);
            doReturn(List.of(earlySubmit, lateSubmit))
                    .when(analysisResultSpecification).analysisTargetList("code", latestTypeCodes);

            final Optional<AnalysisResultEntity> actual = analysisResultSpecification.findLatestAnalysisResult("code");

            assertTrue(actual.isPresent());
            assertEquals(2, actual.get().getId());
        }

        @DisplayName("findLatestAnalysisResult : 半期報告書は latest 母集団に含まれる")
        @Test
        void includesSemiannualInLatestPopulation() {
            final AnalysisResultEntity annual = new AnalysisResultEntity(
                    1, "code", LocalDate.parse("2025-03-31"), BigDecimal.valueOf(900),
                    null, "120", "4", LocalDate.parse("2025-06-30"), null, null);
            final AnalysisResultEntity semiannual = new AnalysisResultEntity(
                    2, "code", LocalDate.parse("2025-09-30"), BigDecimal.valueOf(1100),
                    null, "160", "H", LocalDate.parse("2025-11-15"), null, null);
            doReturn(List.of(annual, semiannual))
                    .when(analysisResultSpecification).analysisTargetList("code", latestTypeCodes);

            final Optional<AnalysisResultEntity> actual = analysisResultSpecification.findLatestAnalysisResult("code");

            assertTrue(actual.isPresent());
            assertEquals(2, actual.get().getId());
        }

        @DisplayName("findLatestAnalysisResult : 対象がないときは空で返却する")
        @Test
        void empty() {
            doReturn(List.of()).when(analysisResultSpecification).analysisTargetList("code", latestTypeCodes);

            final Optional<AnalysisResultEntity> actual = analysisResultSpecification.findLatestAnalysisResult("code");

            assertTrue(actual.isEmpty());
        }
    }

    @Nested
    @DisplayName("findUpdatedList のテスト")
    class FindUpdatedList {

        @DisplayName("findUpdatedList : DAO の結果をそのまま返却する")
        @Test
        void delegatesToDao() {
            final LocalDate submitDate = LocalDate.parse("2021-04-01");
            final AnalysisResultEntity entity = new AnalysisResultEntity(
                    1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(500),
                    null, "120", "4", submitDate, null, null);
            when(analysisResultDao.selectBySubmitDateAndCreatedAt(submitDate, LocalDate.now()))
                    .thenReturn(List.of(entity));

            final List<AnalysisResultEntity> actual = analysisResultSpecification.findUpdatedList(submitDate);

            assertEquals(1, actual.size());
            assertSame(entity, actual.get(0));
        }

        @DisplayName("findUpdatedList : 結果が空でも例外なく空リストを返却する")
        @Test
        void emptyResult() {
            final LocalDate submitDate = LocalDate.parse("2021-04-01");
            when(analysisResultDao.selectBySubmitDateAndCreatedAt(submitDate, LocalDate.now()))
                    .thenReturn(List.of());

            final List<AnalysisResultEntity> actual = analysisResultSpecification.findUpdatedList(submitDate);

            assertNotNull(actual);
            assertTrue(actual.isEmpty());
        }
    }

    @Nested
    @DisplayName("係数一括再計算バッチ向けメソッドのテスト")
    class Recalculation {

        @DisplayName("findAll : DAO の結果をそのまま返却する")
        @Test
        void findAll() {
            final AnalysisResultEntity entity = new AnalysisResultEntity(
                    1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(500),
                    null, "120", "4", LocalDate.now(), null, null);
            when(analysisResultDao.selectAll()).thenReturn(List.of(entity));

            final List<AnalysisResultEntity> actual = analysisResultSpecification.findAll();

            assertEquals(1, actual.size());
            assertSame(entity, actual.get(0));
        }

        @DisplayName("findAnalysisResults : DAO の結果をそのまま返却する")
        @Test
        void findAnalysisResults() {
            final AnalysisResultEntity entity = new AnalysisResultEntity(
                    1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(500),
                    null, "120", "4", LocalDate.now(), null, null);
            when(analysisResultDao.selectByIds(List.of(1, 2))).thenReturn(List.of(entity));

            final List<AnalysisResultEntity> actual = analysisResultSpecification.findAnalysisResults(List.of(1, 2));

            assertEquals(1, actual.size());
            assertSame(entity, actual.get(0));
        }

        @DisplayName("findAnalysisResults : 空リストなら DAO を呼ばず空を返す")
        @Test
        void findAnalysisResultsEmpty() {
            final List<AnalysisResultEntity> actual = analysisResultSpecification.findAnalysisResults(List.of());

            assertTrue(actual.isEmpty());
            verify(analysisResultDao, never()).selectByIds(any());
        }

        @DisplayName("countAll : DAO の件数をそのまま返却する")
        @Test
        void countAll() {
            when(analysisResultDao.countAll()).thenReturn(42);

            assertEquals(42, analysisResultSpecification.countAll());
        }

        @DisplayName("updateCorporateValueAndRimValue : DAO に委譲する")
        @Test
        void updateCorporateValueAndRimValue() {
            analysisResultSpecification.updateCorporateValueAndRimValue(1, BigDecimal.TEN, BigDecimal.ONE);

            verify(analysisResultDao, times(1))
                    .updateCorporateValueAndRimValue(1, BigDecimal.TEN, BigDecimal.ONE);
        }
    }

    @Nested
    @DisplayName("displayTargetList のテスト")
    class DisplayTargetList {

        @DisplayName("displayTargetList : 企業コードと書類種別で DAO に委譲する")
        @Test
        void delegatesToDao() {
            final List<String> documentTypeCode = List.of("120");
            final AnalysisResultEntity entity = new AnalysisResultEntity(
                    1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(500),
                    null, "120", "4", null, null, null);
            when(analysisResultDao.selectByCompanyCodeAndType("code", documentTypeCode))
                    .thenReturn(List.of(entity));

            final List<AnalysisResultEntity> actual = analysisResultSpecification.displayTargetList(company, documentTypeCode);

            assertEquals(1, actual.size());
            assertSame(entity, actual.get(0));
        }
    }

    @Nested
    @DisplayName("analysisTargetList のテスト")
    class AnalysisTargetList {

        @DisplayName("analysisTargetList : EPOCH の documentPeriod は除外される")
        @Test
        void filtersOutEpoch() {
            final AnalysisResultEntity epoch = new AnalysisResultEntity(
                    1, "code", LocalDate.EPOCH, BigDecimal.valueOf(500),
                    null, "120", "4", LocalDate.parse("2020-09-30"), null, null);
            final AnalysisResultEntity normal = new AnalysisResultEntity(
                    2, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(900),
                    null, "120", "4", LocalDate.parse("2020-09-30"), null, null);
            when(analysisResultDao.selectByCompanyCodeAndType("code", annualStatTypeCodes))
                    .thenReturn(List.of(epoch, normal));

            final List<AnalysisResultEntity> actual = analysisResultSpecification.analysisTargetList("code", annualStatTypeCodes);

            assertEquals(1, actual.size());
            assertEquals(2, actual.get(0).getId());
        }

        @DisplayName("analysisTargetList : 同一 documentPeriod では submitDate が最大のものを採用する")
        @Test
        void picksLatestSubmitDatePerPeriod() {
            final AnalysisResultEntity oldSubmit = new AnalysisResultEntity(
                    1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(900),
                    null, "120", "4", LocalDate.parse("2020-09-30"), null, null);
            final AnalysisResultEntity newSubmit = new AnalysisResultEntity(
                    2, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(1100),
                    null, "120", "4", LocalDate.parse("2020-12-31"), null, null);
            final AnalysisResultEntity otherPeriod = new AnalysisResultEntity(
                    3, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(700),
                    null, "120", "4", LocalDate.parse("2019-09-30"), null, null);
            when(analysisResultDao.selectByCompanyCodeAndType("code", annualStatTypeCodes))
                    .thenReturn(List.of(oldSubmit, newSubmit, otherPeriod));

            final List<AnalysisResultEntity> actual = analysisResultSpecification.analysisTargetList("code", annualStatTypeCodes);

            assertEquals(2, actual.size());
            assertTrue(actual.stream().anyMatch(e -> e.getId() == 2));
            assertTrue(actual.stream().anyMatch(e -> e.getId() == 3));
            assertFalse(actual.stream().anyMatch(e -> e.getId() == 1));
        }

        @DisplayName("analysisTargetList : DAO が空のときは空リストを返す")
        @Test
        void empty() {
            when(analysisResultDao.selectByCompanyCodeAndType("code", annualStatTypeCodes))
                    .thenReturn(List.of());

            final List<AnalysisResultEntity> actual = analysisResultSpecification.analysisTargetList("code", annualStatTypeCodes);

            assertNotNull(actual);
            assertTrue(actual.isEmpty());
        }

        @DisplayName("analysisTargetList : latest 母集団と annual 母集団を別々に扱える")
        @Test
        void supportsSeparatedPopulations() {
            final AnalysisResultEntity annual = new AnalysisResultEntity(
                    1, "code", LocalDate.parse("2025-03-31"), BigDecimal.valueOf(900),
                    null, "120", "4", LocalDate.parse("2025-06-30"), null, null);
            final AnalysisResultEntity semiannual = new AnalysisResultEntity(
                    2, "code", LocalDate.parse("2025-09-30"), BigDecimal.valueOf(1100),
                    null, "160", "H", LocalDate.parse("2025-11-15"), null, null);
            when(analysisResultDao.selectByCompanyCodeAndType("code", annualStatTypeCodes))
                    .thenReturn(List.of(annual));
            when(analysisResultDao.selectByCompanyCodeAndType("code", latestTypeCodes))
                    .thenReturn(List.of(annual, semiannual));

            final List<AnalysisResultEntity> annualActual =
                    analysisResultSpecification.analysisTargetList("code", annualStatTypeCodes);
            final List<AnalysisResultEntity> latestActual =
                    analysisResultSpecification.analysisTargetList("code", latestTypeCodes);

            assertEquals(List.of(1), annualActual.stream().map(AnalysisResultEntity::getId).toList());
            assertEquals(List.of(1, 2), latestActual.stream().map(AnalysisResultEntity::getId).toList());
        }
    }

    @Nested
    @DisplayName("insert のテスト")
    class Insert {

        private static final LocalDateTime FIXED_NOW = LocalDateTime.parse("2021-04-01T12:00:00");

        private Document buildDocument(final LocalDate documentPeriod) {
            return new Document(
                    "doc-1",
                    DocumentTypeCode.DTC_120,
                    QuarterType.QT_4,
                    "edinetCode",
                    documentPeriod,
                    LocalDate.parse("2021-04-01"),
                    null,
                    null,
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
        }

        private AnalysisResult buildAnalysisResult() {
            return new AnalysisResult(
                    BigDecimal.valueOf(500),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(50),
                    BigDecimal.valueOf(10),
                    BigDecimal.valueOf(5),
                    LocalDate.parse("2021-04-01"),
                    "doc-1"
            );
        }

        @DisplayName("insert : 正常系では DAO に登録される")
        @Test
        void success() {
            final Company target = new Company(
                    "code",
                    null,
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    true
            );
            when(companySpecification.findCompanyByEdinetCode("edinetCode"))
                    .thenReturn(Optional.of(target));
            doReturn(FIXED_NOW).when(analysisResultSpecification).nowLocalDateTime();

            analysisResultSpecification.insert(buildDocument(LocalDate.parse("2020-06-30")), buildAnalysisResult());

            verify(analysisResultDao, times(1)).insert(any(AnalysisResultEntity.class));
        }

        @DisplayName("insert : corporate_value と rim_value を書き込む（BPS/EPS/ROE/ROA は永続列を持たない）")
        @Test
        void doesNotPersistIndicatorColumns() {
            final Company target = new Company(
                    "code",
                    null,
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    true
            );
            when(companySpecification.findCompanyByEdinetCode("edinetCode"))
                    .thenReturn(Optional.of(target));
            doReturn(FIXED_NOW).when(analysisResultSpecification).nowLocalDateTime();
            final AnalysisResult analysisResult = new AnalysisResult(
                    BigDecimal.valueOf(500),
                    BigDecimal.valueOf(77),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(50),
                    BigDecimal.valueOf(10),
                    BigDecimal.valueOf(5),
                    LocalDate.parse("2021-04-01"),
                    "doc-1"
            );

            analysisResultSpecification.insert(buildDocument(LocalDate.parse("2020-06-30")), analysisResult);

            final ArgumentCaptor<AnalysisResultEntity> captor = ArgumentCaptor.forClass(AnalysisResultEntity.class);
            verify(analysisResultDao).insert(captor.capture());
            final AnalysisResultEntity actual = captor.getValue();
            assertEquals(BigDecimal.valueOf(500), actual.getCorporateValue());
            assertEquals(BigDecimal.valueOf(77), actual.getRimValue().orElseThrow());
        }

        @DisplayName("insert : 企業が見つからないときは FundanalyzerNotExistException")
        @Test
        void companyNotFound() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode"))
                    .thenReturn(Optional.empty());

            assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResultSpecification.insert(
                            buildDocument(LocalDate.parse("2020-06-30")), buildAnalysisResult())
            );
            verify(analysisResultDao, never()).insert(any(AnalysisResultEntity.class));
        }

        @DisplayName("insert : documentPeriod が null のときは FundanalyzerNotExistException")
        @Test
        void documentPeriodMissing() {
            final Company target = new Company(
                    "code",
                    null,
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    true
            );
            when(companySpecification.findCompanyByEdinetCode("edinetCode"))
                    .thenReturn(Optional.of(target));

            assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResultSpecification.insert(buildDocument(null), buildAnalysisResult())
            );
            verify(analysisResultDao, never()).insert(any(AnalysisResultEntity.class));
        }

        @DisplayName("insert : 一意制約違反のときは例外を握りつぶしてログ出力する")
        @Test
        void uniqueConstraintIsSwallowed() {
            final Company target = new Company(
                    "code",
                    null,
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    true
            );
            when(companySpecification.findCompanyByEdinetCode("edinetCode"))
                    .thenReturn(Optional.of(target));
            doReturn(FIXED_NOW).when(analysisResultSpecification).nowLocalDateTime();
            final DataIntegrityViolationException wrapper =
                    new DataIntegrityViolationException(
                            "duplicate",
                            new UniqueConstraintException(SqlLogType.FORMATTED, Mockito.mock(Sql.class), null));
            doThrow(wrapper).when(analysisResultDao).insert(any(AnalysisResultEntity.class));

            analysisResultSpecification.insert(
                    buildDocument(LocalDate.parse("2020-06-30")), buildAnalysisResult());

            verify(analysisResultDao, times(1)).insert(any(AnalysisResultEntity.class));
        }

        @DisplayName("insert : 一意制約以外の NestedRuntimeException は FundanalyzerRuntimeException でラップされる")
        @Test
        void otherNestedRuntimeException() {
            final Company target = new Company(
                    "code",
                    null,
                    null,
                    null,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    true
            );
            when(companySpecification.findCompanyByEdinetCode("edinetCode"))
                    .thenReturn(Optional.of(target));
            doReturn(FIXED_NOW).when(analysisResultSpecification).nowLocalDateTime();
            final DataIntegrityViolationException wrapper =
                    new DataIntegrityViolationException(
                            "boom",
                            new IllegalStateException("other failure"));
            doThrow(wrapper).when(analysisResultDao).insert(any(AnalysisResultEntity.class));

            assertThrows(
                    FundanalyzerRuntimeException.class,
                    () -> analysisResultSpecification.insert(
                            buildDocument(LocalDate.parse("2020-06-30")), buildAnalysisResult())
            );
        }
    }
}
