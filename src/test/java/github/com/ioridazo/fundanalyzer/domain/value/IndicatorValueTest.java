package github.com.ioridazo.fundanalyzer.domain.value;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IndicatorValueTest {

    private final IndicatorValue indicatorValue = new IndicatorValue(null, null, null, null, null);

    private AnalysisResult analysisResult(
            final BigDecimal corporateValue, final BigDecimal bps, final BigDecimal eps) {
        return new AnalysisResult(corporateValue, null, bps, eps, null, null, null, null);
    }

    @Nested
    class calculatePriceCorporateValueRatio {

        @DisplayName("calculatePriceCorporateValueRatio : 各値を取得して計算する")
        @Test
        void present() {
            var stockPrice = BigDecimal.valueOf(1523);
            var analysisResult = analysisResult(BigDecimal.valueOf(2123.14), null, null);

            var expected = BigDecimal.valueOf(1523).divide(BigDecimal.valueOf(2123.14), 10, RoundingMode.HALF_UP);
            var actual = indicatorValue.calculatePriceCorporateValueRatio(stockPrice, analysisResult);
            assertEquals(expected, actual);
        }
    }

    @Nested
    class calculatePer {

        @DisplayName("calculatePer : 各値を取得して計算する")
        @Test
        void present() {
            var stockPrice = BigDecimal.valueOf(1523);
            var analysisResult = analysisResult(null, null, BigDecimal.valueOf(2123.14));

            var expected = BigDecimal.valueOf(1523).divide(BigDecimal.valueOf(2123.14), 10, RoundingMode.HALF_UP);
            var actual = indicatorValue.calculatePer(stockPrice, analysisResult).orElseThrow();
            assertEquals(expected, actual);
        }

        @DisplayName("calculatePer : EPSが存在しないとき")
        @Test
        void eps_isEmpty() {
            var stockPrice = BigDecimal.valueOf(1523);
            var analysisResult = analysisResult(null, null, null);

            assertNull(indicatorValue.calculatePer(stockPrice, analysisResult).orElse(null));
        }

        @DisplayName("calculatePer : EPSが負のときは算出しない")
        @Test
        void eps_isNegative() {
            var stockPrice = BigDecimal.valueOf(1523);
            var analysisResult = analysisResult(null, null, BigDecimal.valueOf(-2123.14));

            assertEquals(Optional.empty(), indicatorValue.calculatePer(stockPrice, analysisResult));
        }

        @DisplayName("calculatePer : EPSがゼロのときは算出しない")
        @Test
        void eps_isZero() {
            var stockPrice = BigDecimal.valueOf(1523);
            var analysisResult = analysisResult(null, null, BigDecimal.ZERO);

            assertEquals(Optional.empty(), indicatorValue.calculatePer(stockPrice, analysisResult));
        }
    }

    @Nested
    class calculatePbr {

        @DisplayName("calculatePbr : 各値を取得して計算する")
        @Test
        void present() {
            var stockPrice = BigDecimal.valueOf(1523);
            var analysisResult = analysisResult(null, BigDecimal.valueOf(2123.14), null);

            var expected = BigDecimal.valueOf(1523).divide(BigDecimal.valueOf(2123.14), 10, RoundingMode.HALF_UP);
            var actual = indicatorValue.calculatePbr(stockPrice, analysisResult).orElseThrow();
            assertEquals(expected, actual);
        }

        @DisplayName("calculatePbr : BPSが存在しないとき")
        @Test
        void bps_isEmpty() {
            var stockPrice = BigDecimal.valueOf(1523);
            var analysisResult = analysisResult(null, null, null);

            assertNull(indicatorValue.calculatePbr(stockPrice, analysisResult).orElse(null));
        }
    }

    @Nested
    class grahamIndex {
        @DisplayName("grahamIndex : 各値を取得して計算する")
        @Test
        void present() {
            var per = BigDecimal.valueOf(6.33);
            var pbr = BigDecimal.valueOf(0.14);

            var expected = per.multiply(pbr);
            var actual = indicatorValue.calculateGrahamIndex(per, pbr).orElseThrow();
            assertEquals(expected, actual);
        }

        @DisplayName("grahamIndex : PERが存在しないとき")
        @Test
        void per_isEmpty() {
            var per = BigDecimal.valueOf(6.33);
            assertNull(indicatorValue.calculateGrahamIndex(per, null).orElse(null));
        }

        @DisplayName("grahamIndex : PBRが存在しないとき")
        @Test
        void pbr_isEmpty() {
            var pbr = BigDecimal.valueOf(6.33);
            assertNull(indicatorValue.calculateGrahamIndex(null, pbr).orElse(null));
        }

        @DisplayName("grahamIndex : PERが負のときは算出しない")
        @Test
        void per_isNegative() {
            var per = BigDecimal.valueOf(-6.33);
            var pbr = BigDecimal.valueOf(0.14);

            assertEquals(Optional.empty(), indicatorValue.calculateGrahamIndex(per, pbr));
        }

        @DisplayName("grahamIndex : PBRが負のときは算出しない")
        @Test
        void pbr_isNegative() {
            var per = BigDecimal.valueOf(6.33);
            var pbr = BigDecimal.valueOf(-0.14);

            assertEquals(Optional.empty(), indicatorValue.calculateGrahamIndex(per, pbr));
        }

        @DisplayName("grahamIndex : PERとPBRが両方負のときは算出しない")
        @Test
        void per_and_pbr_areNegative() {
            var per = BigDecimal.valueOf(-6.33);
            var pbr = BigDecimal.valueOf(-0.14);

            assertEquals(Optional.empty(), indicatorValue.calculateGrahamIndex(per, pbr));
        }

        @DisplayName("grahamIndex : PERがゼロのときは算出しない")
        @Test
        void per_isZero() {
            var per = BigDecimal.ZERO;
            var pbr = BigDecimal.valueOf(0.14);

            assertEquals(Optional.empty(), indicatorValue.calculateGrahamIndex(per, pbr));
        }

        @DisplayName("grahamIndex : PBRがゼロのときは算出しない")
        @Test
        void pbr_isZero() {
            var per = BigDecimal.valueOf(6.33);
            var pbr = BigDecimal.ZERO;

            assertEquals(Optional.empty(), indicatorValue.calculateGrahamIndex(per, pbr));
        }
    }
}
