package github.com.ioridazo.fundanalyzer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AnalysisCoefficient のテスト")
class AnalysisCoefficientTest {

    @Nested
    @DisplayName("defaults メソッド")
    class Defaults {

        @DisplayName("defaults : 現行ハードコード値（10 / 1.2 / 4）と一致する")
        @Test
        void returnsCurrentHardcodedValues() {
            var actual = AnalysisCoefficient.defaults();
            assertAll(
                    () -> assertEquals(BigDecimal.valueOf(10), actual.getOperatingProfitWeight()),
                    () -> assertEquals(BigDecimal.valueOf(1.2), actual.getCurrentLiabilitiesRatio()),
                    () -> assertEquals(BigDecimal.valueOf(4), actual.getAnnualWeight())
            );
        }
    }

    @Nested
    @DisplayName("プロパティバインド")
    class Binding {

        @DisplayName("バインド : app.config.analysis.* が各フィールドに反映される")
        @Test
        void bindsKeysToFields() {
            var source = new MapConfigurationPropertySource(Map.of(
                    "app.config.analysis.operating-profit-weight", "12",
                    "app.config.analysis.current-liabilities-ratio", "1.5",
                    "app.config.analysis.annual-weight", "4"
            ));

            var actual = new Binder(source).bind("app.config.analysis", AnalysisCoefficient.class).get();
            assertAll(
                    () -> assertEquals(new BigDecimal("12"), actual.getOperatingProfitWeight()),
                    () -> assertEquals(new BigDecimal("1.5"), actual.getCurrentLiabilitiesRatio()),
                    () -> assertEquals(new BigDecimal("4"), actual.getAnnualWeight())
            );
        }
    }
}
