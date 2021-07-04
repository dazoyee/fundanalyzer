package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class AnalysisResultSpecificationTest {

    private static final List<String> targetTypeCodes = List.of("120", "130");

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
        analysisResultSpecification.targetTypeCodes = List.of("120", "130");
    }

    @Nested
    class latestCorporateValue {

        Company company = new Company(
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

        @DisplayName("latestCorporateValue : 最新の企業価値を取得する")
        @Test
        void present_period() {
            var analysisResult1 = new AnalysisResultEntity(1, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(900), "120", "4", null, null, null);
            var analysisResult2 = new AnalysisResultEntity(2, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(1100), "120", "4", null, null, null);
            doReturn(List.of(analysisResult1, analysisResult2)).when(analysisResultSpecification).targetList(company, targetTypeCodes);

            var actual = analysisResultSpecification.latestCorporateValue(company);
            assertEquals(BigDecimal.valueOf(110000, 2), actual.orElseThrow());
        }

        @DisplayName("latestCorporateValue : 最新の企業価値を取得する")
        @Test
        void present_submitDate() {
            var analysisResult1 = new AnalysisResultEntity(1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(900), "120", "4", LocalDate.parse("2020-09-01"), null, null);
            var analysisResult2 = new AnalysisResultEntity(2, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(1100), "120", "4", LocalDate.parse("2020-10-01"), null, null);
            doReturn(List.of(analysisResult1, analysisResult2)).when(analysisResultSpecification).targetList(company, targetTypeCodes);

            var actual = analysisResultSpecification.latestCorporateValue(company);
            assertEquals(BigDecimal.valueOf(110000, 2), actual.orElseThrow());
        }

        @DisplayName("latestCorporateValue : 企業価値がないときは空で返却する")
        @Test
        void empty() {
            doReturn(List.of()).when(analysisResultSpecification).targetList(company, targetTypeCodes);

            var actual = analysisResultSpecification.latestCorporateValue(company);
            assertNull(actual.orElse(null));
        }

        @DisplayName("latestCorporateValue : 小数点以下表示を確認する")
        @Test
        void scale() {
            var analysisResult1 = new AnalysisResultEntity(
                    1,
                    "code",
                    LocalDate.parse("2020-06-30"),
                    BigDecimal.valueOf(500.250515),
                    DocumentTypeCode.DTC_120.toValue(),
                    QuarterType.QT_4.toValue(),
                    LocalDate.parse("2020-09-30"),
                    null,
                    null
            );
            doReturn(List.of(analysisResult1)).when(analysisResultSpecification).targetList(company, targetTypeCodes);

            var actual = analysisResultSpecification.latestCorporateValue(company);
            assertEquals(BigDecimal.valueOf(500.25), actual.orElseThrow());
        }
    }

    @Nested
    class averageCorporateValue {

        Company company = new Company(
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

        @DisplayName("averageCorporateValue : 平均の企業価値を取得する")
        @Test
        void present() {
            var analysisResult1 = new AnalysisResultEntity(1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(900), "120", "4", null, null, null);
            var analysisResult2 = new AnalysisResultEntity(2, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(1100), "120", "4", null, null, null);
            doReturn(List.of(analysisResult1, analysisResult2)).when(analysisResultSpecification).targetList(company, targetTypeCodes);

            var actual = analysisResultSpecification.averageCorporateValue(company);
            assertEquals(BigDecimal.valueOf(100000, 2), actual.orElseThrow());
        }

        @DisplayName("averageCorporateValue : 企業価値がないときは空で返却する")
        @Test
        void empty() {
            doReturn(List.of()).when(analysisResultSpecification).targetList(company, targetTypeCodes);

            var actual = analysisResultSpecification.averageCorporateValue(company);
            assertNull(actual.orElse(null));
        }

        @DisplayName("averageCorporateValue : 小数点以下表示を確認する")
        @Test
        void scale() {
            var analysisResult1 = new AnalysisResultEntity(
                    1,
                    "code",
                    LocalDate.parse("2020-06-30"),
                    BigDecimal.valueOf(500.250515),
                    DocumentTypeCode.DTC_120.toValue(),
                    QuarterType.QT_4.toValue(),
                    LocalDate.parse("2020-09-30"),
                    null,
                    null
            );
            doReturn(List.of(analysisResult1)).when(analysisResultSpecification).targetList(company, targetTypeCodes);

            var actual = analysisResultSpecification.averageCorporateValue(company);
            assertEquals(BigDecimal.valueOf(500.25), actual.orElseThrow());
        }
    }

    @Nested
    class standardDeviation {

        Company company = new Company(
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

        @DisplayName("standardDeviation : 企業価値の標準偏差を取得する")
        @Test
        void present() {
            var analysisResult1 = new AnalysisResultEntity(1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(1100), "120", "4", null, null, null);
            var analysisResult2 = new AnalysisResultEntity(2, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(900), "120", "4", null, null, null);
            doReturn(List.of(analysisResult1, analysisResult2)).when(analysisResultSpecification).targetList(company, targetTypeCodes);

            var actual = analysisResultSpecification.standardDeviation(company, BigDecimal.valueOf(100000, 2));
            assertEquals(BigDecimal.valueOf(100.0), actual.orElseThrow());
        }

        @DisplayName("standardDeviation : 企業価値がないときは空で返却する")
        @Test
        void empty() {
            doReturn(List.of()).when(analysisResultSpecification).targetList(company, targetTypeCodes);

            var actual = analysisResultSpecification.standardDeviation(company, BigDecimal.valueOf(100000, 2));
            assertNull(actual.orElse(null));
        }

        @DisplayName("standardDeviation : 平均の企業価値がないときは空で返却する")
        @Test
        void averageCorporateValue_isNull() {
            doReturn(List.of()).when(analysisResultSpecification).targetList(company, targetTypeCodes);

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
                    DocumentTypeCode.DTC_120.toValue(),
                    QuarterType.QT_4.toValue(),
                    LocalDate.parse("2020-09-30"),
                    null,
                    null
            );
            doReturn(List.of(analysisResult1)).when(analysisResultSpecification).targetList(company, targetTypeCodes);

            var actual = analysisResultSpecification.standardDeviation(company, BigDecimal.valueOf(500.25));
            assertEquals(BigDecimal.valueOf(0, 1), actual.orElseThrow());
        }
    }

    @Nested
    class coefficientOfVariation {

        Company company = new Company(
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

        @DisplayName("coefficientOfVariation : 変動係数を取得する")
        @Test
        void present() {
            var actual = analysisResultSpecification.coefficientOfVariation(BigDecimal.valueOf(100.0), BigDecimal.valueOf(100000, 2));
            assertEquals(BigDecimal.valueOf(100, 3), actual.orElseThrow());
        }

        @DisplayName("coefficientOfVariation : 標準偏差がないときは空で返却する")
        @Test
        void coefficientOfVariation_isNull() {
            doReturn(List.of()).when(analysisResultSpecification).targetList(company, targetTypeCodes);

            var actual = analysisResultSpecification.coefficientOfVariation(null, BigDecimal.valueOf(100000, 2));
            assertNull(actual.orElse(null));
        }

        @DisplayName("coefficientOfVariation : 平均の企業価値がないときは空で返却する")
        @Test
        void averageCorporateValue_isNull() {
            doReturn(List.of()).when(analysisResultSpecification).targetList(company, targetTypeCodes);

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

        Company company = new Company(
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

        @DisplayName("countYear : 分析年数を取得する")
        @Test
        void present() {
            var analysisResult1 = new AnalysisResultEntity(1, "code", LocalDate.parse("2020-06-30"), BigDecimal.valueOf(1100), "120", "4", null, null, null);
            var analysisResult2 = new AnalysisResultEntity(2, "code", LocalDate.parse("2019-06-30"), BigDecimal.valueOf(900), "120", "4", null, null, null);
            doReturn(List.of(analysisResult1, analysisResult2)).when(analysisResultSpecification).targetList(company, targetTypeCodes);

            var actual = analysisResultSpecification.countYear(company);
            assertEquals(BigDecimal.valueOf(2), actual);
        }

        @DisplayName("countYear : 企業価値がないときは0で返却する")
        @Test
        void empty() {
            doReturn(List.of()).when(analysisResultSpecification).targetList(company, targetTypeCodes);

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
                null
        );

        @DisplayName("isAnalyzed : true")
        @Test
        void boolean_true() {
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            when(analysisResultDao.selectByUniqueKey(any(), any(), any(), any()))
                    .thenReturn(Optional.of(new AnalysisResultEntity(null, null, null, null, null, null, null, null, null)));

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
}