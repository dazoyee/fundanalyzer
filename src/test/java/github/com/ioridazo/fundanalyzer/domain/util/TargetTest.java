package github.com.ioridazo.fundanalyzer.domain.util;

import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}