package github.com.ioridazo.fundanalyzer.domain.util;

import github.com.ioridazo.fundanalyzer.domain.entity.DocTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetTest {

    @Nested
    class allCompanies {

        @DisplayName("allCompanies : 処理対象となる会社を抽出する")
        @Test
        void allCompanies_ok() {
            var companyList = List.of(
                    new Company(
                            "code",
                            null,
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            "code",
                            null,
                            2,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            "code",
                            null,
                            3,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    )
            );
            var excludedIndustryList = List.of(
                    new Industry(
                            1,
                            null,
                            null
                    ),
                    new Industry(
                            2,
                            null,
                            null
                    )
            );

            var actual = Target.allCompanies(companyList, excludedIndustryList);

            assertEquals(1, actual.size());
            assertEquals(3, actual.get(0).getIndustryId());
        }
    }

    @Nested
    class containsEdinetCode {

        @DisplayName("containsEdinetCode : 処理対象となる会社に特定のEDINETコードが含まれているときはTRUE")
        @Test
        void containsEdinetCode_true() {
            var companyList = List.of(
                    new Company(
                            "code",
                            null,
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            "code",
                            null,
                            2,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            "code",
                            null,
                            3,
                            "edinetCode",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            "code",
                            null,
                            4,
                            "target",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    )

            );
            var excludedIndustryList = List.of(
                    new Industry(
                            1,
                            null,
                            null
                    ),
                    new Industry(
                            2,
                            null,
                            null
                    )
            );

            var actual = Target.containsEdinetCode("target", companyList, excludedIndustryList);

            assertEquals(2, Target.allCompanies(companyList, excludedIndustryList).size());
            assertTrue(actual);
        }

        @DisplayName("companyAllContainsEdinetCode : 処理対象となる会社に特定のEDINETコードが含まれていないときはFALSE")
        @Test
        void containsEdinetCode_false() {
            var companyList = List.of(
                    new Company(
                            "code",
                            null,
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            "code",
                            null,
                            2,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            "code",
                            null,
                            3,
                            "edinetCode",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new Company(
                            "code",
                            null,
                            4,
                            "target",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    )

            );
            var excludedIndustryList = List.of(
                    new Industry(
                            1,
                            null,
                            null
                    ),
                    new Industry(
                            2,
                            null,
                            null
                    )
            );

            var actual = Target.containsEdinetCode("no target", companyList, excludedIndustryList);

            assertEquals(2, Target.allCompanies(companyList, excludedIndustryList).size());
            assertFalse(actual);
        }
    }

    @Nested
    class distinctAnalysisResults {

        @DisplayName("distinctAnalysisResults : 処理対象の分析結果を抽出する（期間の重複なし）")
        @Test
        void distinctAnalysisResults_single() {
            var analysisResult1 = new AnalysisResult(
                    null,
                    null,
                    LocalDate.of(2020, 1, 1),
                    BigDecimal.valueOf(2020),
                    DocTypeCode.ANNUAL_SECURITIES_REPORT.toValue(),
                    LocalDate.of(2020, 2, 1),
                    null,
                    null
            );
            var analysisResult2 = new AnalysisResult(
                    null,
                    null,
                    LocalDate.of(2021, 1, 1),
                    BigDecimal.valueOf(2021),
                    DocTypeCode.ANNUAL_SECURITIES_REPORT.toValue(),
                    LocalDate.of(2021, 2, 1),
                    null,
                    null
            );

            var actual = Target.distinctAnalysisResults(List.of(analysisResult1, analysisResult2));

            assertAll(
                    () -> assertEquals(2, actual.size()),
                    () -> assertEquals(BigDecimal.valueOf(2020), actual.get(0).getCorporateValue(), "2020"),
                    () -> assertEquals(BigDecimal.valueOf(2021), actual.get(1).getCorporateValue(), "2021")
            );
        }

        @DisplayName("distinctAnalysisResults : 処理対象の分析結果を抽出する（期間の重複あり）")
        @Test
        void distinctAnalysisResults_multiple() {
            var analysisResult1 = new AnalysisResult(
                    null,
                    null,
                    LocalDate.of(2021, 1, 1),
                    BigDecimal.valueOf(201),
                    DocTypeCode.ANNUAL_SECURITIES_REPORT.toValue(),
                    LocalDate.of(2021, 2, 1),
                    null,
                    null
            );
            var analysisResult2 = new AnalysisResult(
                    null,
                    null,
                    LocalDate.of(2021, 1, 1),
                    BigDecimal.valueOf(301),
                    DocTypeCode.AMENDED_SECURITIES_REPORT.toValue(),
                    LocalDate.of(2021, 3, 1),
                    null,
                    null
            );
            var analysisResult3 = new AnalysisResult(
                    null,
                    null,
                    LocalDate.of(2021, 1, 1),
                    BigDecimal.valueOf(401),
                    DocTypeCode.AMENDED_SECURITIES_REPORT.toValue(),
                    LocalDate.of(2021, 4, 1),
                    null,
                    null
            );

            var actual = Target.distinctAnalysisResults(List.of(analysisResult1, analysisResult2, analysisResult3));

            assertAll(
                    () -> assertEquals(1, actual.size()),
                    () -> assertEquals(BigDecimal.valueOf(401), actual.get(0).getCorporateValue(), "2021")
            );
        }
    }
}