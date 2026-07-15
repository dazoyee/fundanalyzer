package github.com.ioridazo.fundanalyzer.domain.value;

import github.com.ioridazo.fundanalyzer.config.AnalysisCoefficient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisResultTest {

    private final AnalysisResult analysisResult = new AnalysisResult(null, null, null, null, null, null, null);
    private final AnalysisCoefficient defaultCoefficient = new AnalysisCoefficient(BigDecimal.valueOf(10), BigDecimal.valueOf(1.2));

    @Nested
    class calculateCorporateValue {

        Document document = defaultDocument();

        @DisplayName("calculateCorporateValue : 各値を取得して計算する")
        @Test
        void present() {
            var financeValue = defaultCorporateValueFinanceValue();

            var expected = expectedCorporateValue(defaultCoefficient, null);
            var actual = analysisResult.calculateCorporateValue(financeValue, document, defaultCoefficient);
            assertEquals(expected, actual);
            assertEquals(expectedCorporateValueLegacy(defaultCoefficient, null), actual);
        }

        @DisplayName("calculateCorporateValue : 四半期報告書の各値を取得して計算する")
        @Test
        void quarter() {
            var document = documentOf(QuarterType.QT_3);
            var financeValue = defaultCorporateValueFinanceValue();

            var expected = expectedCorporateValue(defaultCoefficient, QuarterType.QT_3);
            var actual = analysisResult.calculateCorporateValue(financeValue, document, defaultCoefficient);
            assertEquals(expected, actual);
        }

        @DisplayName("calculateCorporateValue : QuarterType の全パターンで営業利益のみ年換算する")
        @Test
        void allQuarterTypes() {
            var financeValue = defaultCorporateValueFinanceValue();

            assertEquals(
                    expectedCorporateValue(defaultCoefficient, QuarterType.QT_1),
                    analysisResult.calculateCorporateValue(financeValue, documentOf(QuarterType.QT_1), defaultCoefficient)
            );
            assertEquals(
                    expectedCorporateValue(defaultCoefficient, QuarterType.QT_2),
                    analysisResult.calculateCorporateValue(financeValue, documentOf(QuarterType.QT_2), defaultCoefficient)
            );
            assertEquals(
                    expectedCorporateValue(defaultCoefficient, QuarterType.QT_3),
                    analysisResult.calculateCorporateValue(financeValue, documentOf(QuarterType.QT_3), defaultCoefficient)
            );
            assertEquals(
                    expectedCorporateValue(defaultCoefficient, QuarterType.QT_4),
                    analysisResult.calculateCorporateValue(financeValue, documentOf(QuarterType.QT_4), defaultCoefficient)
            );
            assertEquals(
                    expectedCorporateValue(defaultCoefficient, QuarterType.QT_OTHER),
                    analysisResult.calculateCorporateValue(financeValue, documentOf(QuarterType.QT_OTHER), defaultCoefficient)
            );
        }

        @DisplayName("calculateCorporateValue : 有報相当は修正前後で算出結果が不変")
        @Test
        void annualFormulaIsUnchanged() {
            var financeValue = defaultCorporateValueFinanceValue();
            var expectedNewFormula = expectedCorporateValue(defaultCoefficient, null);
            var expectedLegacyFormula = expectedCorporateValueLegacy(defaultCoefficient, null);

            assertEquals(expectedLegacyFormula, expectedNewFormula);
            assertEquals(
                    expectedNewFormula,
                    analysisResult.calculateCorporateValue(financeValue, defaultDocument(), defaultCoefficient)
            );
        }

        @DisplayName("calculateCorporateValue : 流動資産合計が存在しないとき")
        @Test
        void totalCurrentAssets_isEmpty() {
            var financeValue = FinanceValue.of(
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

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateCorporateValue(financeValue, document, defaultCoefficient)
            );
            assertEquals(BsSubject.BsEnum.TOTAL_CURRENT_ASSETS.getSubject(), exception.getSubjectName().orElseThrow());
        }

        @DisplayName("calculateCorporateValue : 投資その他の資産合計が存在しないとき")
        @Test
        void totalInvestmentsAndOtherAssets_isEmpty() {
            var financeValue = FinanceValue.of(
                    1000L,
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

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateCorporateValue(financeValue, document, defaultCoefficient)
            );
            assertEquals(BsSubject.BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS.getSubject(), exception.getSubjectName().orElseThrow());
        }


        @DisplayName("calculateCorporateValue : 流動負債合計が存在しないとき")
        @Test
        void totalCurrentLiabilities_isEmpty() {
            var financeValue = FinanceValue.of(
                    1000L,
                    1000L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateCorporateValue(financeValue, document, defaultCoefficient)
            );
            assertEquals(BsSubject.BsEnum.TOTAL_CURRENT_LIABILITIES.getSubject(), exception.getSubjectName().orElseThrow());
        }

        @DisplayName("calculateCorporateValue : 固定負債合計が存在しないとき")
        @Test
        void totalFixedLiabilities_isEmpty() {
            var financeValue = FinanceValue.of(
                    1000L,
                    1000L,
                    null,
                    1000L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateCorporateValue(financeValue, document, defaultCoefficient)
            );
            assertEquals(BsSubject.BsEnum.TOTAL_FIXED_LIABILITIES.getSubject(), exception.getSubjectName().orElseThrow());
        }

        @DisplayName("calculateCorporateValue : 営業利益が存在しないとき")
        @Test
        void operatingProfit_isEmpty() {
            var financeValue = FinanceValue.of(
                    1000L,
                    1000L,
                    null,
                    1000L,
                    1000L,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateCorporateValue(financeValue, document, defaultCoefficient)
            );
            assertEquals(PlSubject.PlEnum.OPERATING_PROFIT.getSubject(), exception.getSubjectName().orElseThrow());
        }

        @DisplayName("calculateCorporateValue : 株式総数が存在しないとき")
        @Test
        void numberOfShares_isEmpty() {
            var financeValue = FinanceValue.of(
                    1000L,
                    1000L,
                    null,
                    1000L,
                    1000L,
                    null,
                    null,
                    1000L,
                    null,
                    null
            );

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateCorporateValue(financeValue, document, defaultCoefficient)
            );
            assertEquals("株式総数", exception.getSubjectName().orElseThrow());
        }
    }

    @Nested
    class calculateBps {

        Document document = defaultDocument();

        @DisplayName("calculateBps : 各値を取得して計算する")
        @Test
        void present() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1001L,
                    null,
                    null,
                    999L
            );

            var expected = BigDecimal.valueOf(1001).divide(BigDecimal.valueOf(999), 10, RoundingMode.HALF_UP);
            var actual = analysisResult.calculateBps(financeValue, document).orElseThrow();
            assertEquals(expected, actual);
        }

        @DisplayName("calculateBps : 純資産が存在しないとき")
        @Test
        void totalNetAssets_isEmpty() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    990L
            );

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateBps(financeValue, document)
            );
            assertEquals(BsSubject.BsEnum.TOTAL_NET_ASSETS.getSubject(), exception.getSubjectName().orElseThrow());
        }

        @DisplayName("calculateBps : 株式総数が存在しないとき")
        @Test
        void numberOfShares_isEmpty() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1000L,
                    null,
                    null,
                    null
            );

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateBps(financeValue, document)
            );
            assertEquals("株式総数", exception.getSubjectName().orElseThrow());
        }
    }

    @Nested
    class calculateEps {

        Document document = defaultDocument();

        @DisplayName("calculateEps : 各値を取得して計算する")
        @Test
        void present() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    2001L,
                    1991L
            );

            var expected = BigDecimal.valueOf(2001).divide(BigDecimal.valueOf(1991), 10, RoundingMode.HALF_UP);
            var actual = analysisResult.calculateEps(financeValue, document).orElseThrow();
            assertEquals(expected, actual);
        }

        @DisplayName("calculateEps : 当期純利益が存在しないとき")
        @Test
        void netIncome_isEmpty() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    990L
            );

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateEps(financeValue, document)
            );
            assertEquals(PlSubject.PlEnum.NET_INCOME.getSubject(), exception.getSubjectName().orElseThrow());
        }

        @DisplayName("calculateEps : 株式総数が存在しないとき")
        @Test
        void numberOfShares_isEmpty() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1000L,
                    null
            );

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateEps(financeValue, document)
            );
            assertEquals("株式総数", exception.getSubjectName().orElseThrow());
        }
    }

    @Nested
    class calculateRoe {

        Document document = defaultDocument();

        @DisplayName("calculateRoe : 各値を取得して計算する")
        @Test
        void present() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    123L,
                    1000L,
                    null,
                    880L,
                    null
            );

            var expected = BigDecimal.valueOf(880)
                    .divide(BigDecimal.valueOf(1000 - 123), 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            var actual = analysisResult.calculateRoe(financeValue, document).orElseThrow();
            assertEquals(expected, actual);
        }

        @DisplayName("calculateRoe : 新株予約権が存在しないとき")
        @Test
        void subscriptionWarrant_isEmpty() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1001L,
                    null,
                    888L,
                    null
            );
            var expected = BigDecimal.valueOf(888)
                    .divide(BigDecimal.valueOf(1001), 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            var actual = analysisResult.calculateRoe(financeValue, document).orElseThrow();
            assertEquals(expected, actual);
        }

        @DisplayName("calculateRoe : 当期純利益が存在しないとき")
        @Test
        void netIncome_isEmpty() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1000L,
                    null,
                    null,
                    null
            );

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateRoe(financeValue, document)
            );
            assertEquals(PlSubject.PlEnum.NET_INCOME.getSubject(), exception.getSubjectName().orElseThrow());
        }

        @DisplayName("calculateRoe : 純資産が存在しないとき")
        @Test
        void totalNetAssets_isEmpty() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    880L,
                    null
            );

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateRoe(financeValue, document)
            );
            assertEquals(BsSubject.BsEnum.TOTAL_NET_ASSETS.getSubject(), exception.getSubjectName().orElseThrow());
        }
    }

    @Nested
    class calculateRoa {

        Document document = defaultDocument();

        @DisplayName("calculateRoa : 各値を取得して計算する")
        @Test
        void present() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    1009L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1010L,
                    null
            );

            var expected = BigDecimal.valueOf(1010)
                    .divide(BigDecimal.valueOf(1009), 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            var actual = analysisResult.calculateRoa(financeValue, document).orElseThrow();
            assertEquals(expected, actual);
        }

        @DisplayName("calculateRoa : 当期純利益が存在しないとき")
        @Test
        void netIncome_isEmpty() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    1000L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            var exception = assertThrows(
                    FundanalyzerNotExistException.class,
                    () -> analysisResult.calculateRoa(financeValue, document)
            );
            assertEquals(PlSubject.PlEnum.NET_INCOME.getSubject(), exception.getSubjectName().orElseThrow());
        }

        @DisplayName("calculateRoe : 総資産が存在しないとき")
        @Test
        void totalAssets_isEmpty() {
            var financeValue = FinanceValue.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    880L,
                    null
            );

            assertNull(analysisResult.calculateRoa(financeValue, document).orElse(null));
        }
    }

    @Nested
    @DisplayName("calculateCorporateValue 係数オーバーロードのテスト")
    class calculateCorporateValueWithCoefficient {

        Document document = defaultDocument();

        FinanceValue financeValue = FinanceValue.of(
                1001L,
                1002L,
                null,
                1003L,
                1004L,
                null,
                null,
                10005L,
                null,
                1006L
        );

        @DisplayName("calculateCorporateValue : 指定した営業利益重みが式に反映される")
        @Test
        void operatingProfitWeight_reflected() {
            var coefficient = new AnalysisCoefficient(BigDecimal.valueOf(20), BigDecimal.valueOf(1.2));

            var expected = expectedCorporateValue(coefficient, null);
            var actual = analysisResult.calculateCorporateValue(financeValue, document, coefficient);
            assertEquals(expected, actual);
            assertEquals(expectedCorporateValueLegacy(coefficient, null), actual);
        }

        @DisplayName("calculateCorporateValue : 流動負債調整係数のみ変更すると流動負債の項のみ変わる")
        @Test
        void currentLiabilitiesRatio_reflected() {
            var coefficient = new AnalysisCoefficient(BigDecimal.valueOf(10), BigDecimal.valueOf(2.0));

            var expected = expectedCorporateValue(coefficient, null);
            var actual = analysisResult.calculateCorporateValue(financeValue, document, coefficient);
            assertEquals(expected, actual);
            assertEquals(expectedCorporateValueLegacy(coefficient, null), actual);
        }
    }

    @Nested
    @DisplayName("calculateRimValue のテスト")
    class CalculateRimValue {

        @DisplayName("calculateRimValue : BPS×(ROE/100)÷r で算出する")
        @Test
        void present() {
            // BPS=1000, ROE=12%, r=0.08 → 1000×0.12÷0.08 = 1500
            final BigDecimal actual = analysisResult.calculateRimValue(
                    BigDecimal.valueOf(1000), BigDecimal.valueOf(12), BigDecimal.valueOf(0.08)).orElse(null);
            assertEquals(0, BigDecimal.valueOf(1500).compareTo(actual));
        }

        @DisplayName("calculateRimValue : 資本コスト r が異なると比例して変わる")
        @Test
        void differentR() {
            final BigDecimal actual = analysisResult.calculateRimValue(
                    BigDecimal.valueOf(1000), BigDecimal.valueOf(12), BigDecimal.valueOf(0.10)).orElse(null);
            assertEquals(0, BigDecimal.valueOf(1200).compareTo(actual));
        }

        @DisplayName("calculateRimValue : ROE が 0 以下（赤字等）のときは算出しない")
        @Test
        void roeNonPositive() {
            assertTrue(analysisResult.calculateRimValue(
                    BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.valueOf(0.08)).isEmpty());
            assertTrue(analysisResult.calculateRimValue(
                    BigDecimal.valueOf(1000), BigDecimal.valueOf(-5), BigDecimal.valueOf(0.08)).isEmpty());
        }

        @DisplayName("calculateRimValue : BPS/ROE/r が null・r=0 のときは算出しない")
        @Test
        void nullOrZero() {
            assertTrue(analysisResult.calculateRimValue(
                    null, BigDecimal.valueOf(12), BigDecimal.valueOf(0.08)).isEmpty());
            assertTrue(analysisResult.calculateRimValue(
                    BigDecimal.valueOf(1000), null, BigDecimal.valueOf(0.08)).isEmpty());
            assertTrue(analysisResult.calculateRimValue(
                    BigDecimal.valueOf(1000), BigDecimal.valueOf(12), null).isEmpty());
            assertTrue(analysisResult.calculateRimValue(
                    BigDecimal.valueOf(1000), BigDecimal.valueOf(12), BigDecimal.ZERO).isEmpty());
        }
    }

    @Nested
    @DisplayName("of(entity, financeValue, document) : 都度計算ファクトリ")
    class OfWithComputedIndicators {

        private AnalysisResultEntity entity() {
            return new AnalysisResultEntity(
                    1,
                    "code",
                    LocalDate.parse("2024-03-31"),
                    BigDecimal.valueOf(999),
                    BigDecimal.valueOf(555),
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    "120",
                    null,
                    LocalDate.parse("2024-06-30"),
                    "documentId",
                    null
            );
        }

        @DisplayName("係数依存値は永続値を凍結し、指標は財務諸表値から都度計算する")
        @Test
        void computesIndicatorsFromFinanceValue() {
            var financeValue = FinanceValue.of(
                    null, null, 1200L, null, null, 0L, 800L, null, 150L, 10L);

            var actual = AnalysisResult.of(entity(), financeValue, defaultDocument());

            assertEquals(BigDecimal.valueOf(999), actual.getCorporateValue());
            assertEquals(BigDecimal.valueOf(555), actual.getRimValue().orElseThrow());
            assertEquals(0, BigDecimal.valueOf(80).compareTo(actual.getBps().orElseThrow()));
            assertEquals(0, BigDecimal.valueOf(15).compareTo(actual.getEps().orElseThrow()));
            assertEquals(0, BigDecimal.valueOf(18.75).compareTo(actual.getRoe().orElseThrow()));
            assertEquals(0, BigDecimal.valueOf(12.5).compareTo(actual.getRoa().orElseThrow()));
            assertEquals(LocalDate.parse("2024-06-30"), actual.getSubmitDate());
            assertEquals("documentId", actual.getDocumentId());
        }

        @DisplayName("入力科目が欠損しているときは例外とせず指標を空にする")
        @Test
        void missingInputsYieldEmptyIndicatorsWithoutThrowing() {
            var financeValue = FinanceValue.of(
                    null, null, null, null, null, null, null, null, null, null);

            var actual = AnalysisResult.of(entity(), financeValue, defaultDocument());

            assertEquals(BigDecimal.valueOf(999), actual.getCorporateValue());
            assertEquals(BigDecimal.valueOf(555), actual.getRimValue().orElseThrow());
            assertTrue(actual.getBps().isEmpty());
            assertTrue(actual.getEps().isEmpty());
            assertTrue(actual.getRoe().isEmpty());
            assertTrue(actual.getRoa().isEmpty());
        }
    }

    private Document defaultDocument() {
        return documentOf(null);
    }

    private Document documentOf(final QuarterType quarterType) {
        return new Document(
                "documentId",
                null,
                quarterType,
                "edinetCode",
                null,
                LocalDate.now(),
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
        );
    }

    private FinanceValue defaultCorporateValueFinanceValue() {
        return FinanceValue.of(
                1001L,
                1002L,
                null,
                1003L,
                1004L,
                null,
                null,
                10005L,
                null,
                1006L
        );
    }

    private BigDecimal expectedCorporateValue(final AnalysisCoefficient coefficient, final QuarterType quarterType) {
        var weightingQuarterType = quarterType == null || quarterType.getWeight() == null ? BigDecimal.valueOf(4) : BigDecimal.valueOf(quarterType.getWeight());

        return BigDecimal.valueOf(10005).multiply(coefficient.getOperatingProfitWeight())
                .divide(weightingQuarterType, 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(4))
                .add(BigDecimal.valueOf(1001))
                .subtract(BigDecimal.valueOf(1003).multiply(coefficient.getCurrentLiabilitiesRatio())).add(BigDecimal.valueOf(1002))
                .subtract(BigDecimal.valueOf(1004))
                .divide(BigDecimal.valueOf(1006), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal expectedCorporateValueLegacy(final AnalysisCoefficient coefficient, final QuarterType quarterType) {
        var weightingQuarterType = quarterType == null || quarterType.getWeight() == null ? BigDecimal.valueOf(4) : BigDecimal.valueOf(quarterType.getWeight());

        return BigDecimal.valueOf(10005).multiply(coefficient.getOperatingProfitWeight())
                .add(BigDecimal.valueOf(1001))
                .subtract(BigDecimal.valueOf(1003).multiply(coefficient.getCurrentLiabilitiesRatio())).add(BigDecimal.valueOf(1002))
                .subtract(BigDecimal.valueOf(1004))
                .divide(weightingQuarterType, 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(4))
                .divide(BigDecimal.valueOf(1006), 10, RoundingMode.HALF_UP);
    }
}
