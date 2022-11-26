package github.com.ioridazo.fundanalyzer.domain.value;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IndicatorValueTest {

    private final IndicatorValue indicatorValue = new IndicatorValue(null, null, null, null, null);

    @Nested
    class calculatePriceCorporateValueRatio {

        @DisplayName("calculatePriceCorporateValueRatio : 各値を取得して計算する")
        @Test
        void present() {
            var stockPrice = BigDecimal.valueOf(1523);
            var analysisResultEntity = new AnalysisResultEntity(
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(2123.14),
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

            var expected = BigDecimal.valueOf(1523).divide(BigDecimal.valueOf(2123.14), 10, RoundingMode.HALF_UP);
            var actual = indicatorValue.calculatePriceCorporateValueRatio(stockPrice, analysisResultEntity);
            assertEquals(expected, actual);
        }
    }

    @Nested
    class calculatePer {

        @DisplayName("calculatePer : 各値を取得して計算する")
        @Test
        void present() {
            var stockPrice = BigDecimal.valueOf(1523);
            var analysisResultEntity = new AnalysisResultEntity(
                    null,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(2123.14),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            var expected = BigDecimal.valueOf(1523).divide(BigDecimal.valueOf(2123.14), 10, RoundingMode.HALF_UP);
            var actual = indicatorValue.calculatePer(stockPrice, analysisResultEntity).orElseThrow();
            assertEquals(expected, actual);
        }

        @DisplayName("calculatePer : EPSが存在しないとき")
        @Test
        void eps_isEmpty() {
            var stockPrice = BigDecimal.valueOf(1523);
            var analysisResultEntity = new AnalysisResultEntity(
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
            );

            assertNull(indicatorValue.calculatePer(stockPrice, analysisResultEntity).orElse(null));
        }
    }

    @Nested
    class calculatePbr {

        @DisplayName("calculatePbr : 各値を取得して計算する")
        @Test
        void present() {
            var stockPrice = BigDecimal.valueOf(1523);
            var analysisResultEntity = new AnalysisResultEntity(
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(2123.14),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            var expected = BigDecimal.valueOf(1523).divide(BigDecimal.valueOf(2123.14), 10, RoundingMode.HALF_UP);
            var actual = indicatorValue.calculatePbr(stockPrice, analysisResultEntity).orElseThrow();
            assertEquals(expected, actual);
        }

        @DisplayName("calculatePbr : BPSが存在しないとき")
        @Test
        void bps_isEmpty() {
            var stockPrice = BigDecimal.valueOf(1523);
            var analysisResultEntity = new AnalysisResultEntity(
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
            );

            assertNull(indicatorValue.calculatePbr(stockPrice, analysisResultEntity).orElse(null));
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
    }
}