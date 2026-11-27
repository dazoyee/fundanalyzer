package github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Unit")
class UnitTest {

    @Nested
    @DisplayName("matches")
    class Matches {

        @Test
        @DisplayName("百万円の表記揺れに一致する")
        void matchesMillionsOfYenVariants() {
            assertTrue(Unit.MILLIONS_OF_YEN.matches("金額(百万円)"));
            assertTrue(Unit.MILLIONS_OF_YEN.matches("(単位: 百万円)"));
            assertTrue(Unit.MILLIONS_OF_YEN.matches("単位　百万円"));
        }

        @Test
        @DisplayName("千円の表記揺れに一致する")
        void matchesThousandsOfYenVariants() {
            assertTrue(Unit.THOUSANDS_OF_YEN.matches("（単位 : 千円）"));
            assertTrue(Unit.THOUSANDS_OF_YEN.matches("金額(千円)"));
            assertTrue(Unit.THOUSANDS_OF_YEN.matches("単位：千円"));
        }

        @Test
        @DisplayName("円だけの表記には一致しない")
        void doesNotMatchYenOnly() {
            assertFalse(Unit.THOUSANDS_OF_YEN.matches("(円)"));
            assertFalse(Unit.MILLIONS_OF_YEN.matches("（円）"));
        }
    }
}
