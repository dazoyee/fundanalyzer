package github.com.ioridazo.fundanalyzer.domain.value;

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

class AnalysisResultTest {

    private final AnalysisResult analysisResult = new AnalysisResult(null, null, null, null, null);

    @Nested
    class calculateCorporateValue {

        Document document = defaultDocument();

        @DisplayName("calculateCorporateValue : 各値を取得して計算する")
        @Test
        void present() {
            var financeValue = FinanceValue.of(
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

            var expected = BigDecimal.valueOf(10005).multiply(BigDecimal.valueOf(10))
                    .add(BigDecimal.valueOf(1001))
                    .subtract(BigDecimal.valueOf(1003).multiply(BigDecimal.valueOf(1.2))).add(BigDecimal.valueOf(1002))
                    .subtract(BigDecimal.valueOf(1004))
                    .divide(BigDecimal.valueOf(1006), 10, RoundingMode.HALF_UP);
            var actual = analysisResult.calculateCorporateValue(financeValue, document);
            assertEquals(expected, actual);
        }

        @DisplayName("calculateCorporateValue : 四半期報告書の各値を取得して計算する")
        @Test
        void quarter() {
            var document = new Document(
                    null,
                    null,
                    QuarterType.QT_3,
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
            var financeValue = FinanceValue.of(
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

            var expected = BigDecimal.valueOf(10005).multiply(BigDecimal.valueOf(10))
                    .add(BigDecimal.valueOf(1001))
                    .subtract(BigDecimal.valueOf(1003).multiply(BigDecimal.valueOf(1.2))).add(BigDecimal.valueOf(1002))
                    .subtract(BigDecimal.valueOf(1004))
                    .divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(4))
                    .divide(BigDecimal.valueOf(1006), 10, RoundingMode.HALF_UP);
            var actual = analysisResult.calculateCorporateValue(financeValue, document);
            assertEquals(expected, actual);
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
                    () -> analysisResult.calculateCorporateValue(financeValue, document)
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
                    () -> analysisResult.calculateCorporateValue(financeValue, document)
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
                    () -> analysisResult.calculateCorporateValue(financeValue, document)
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
                    () -> analysisResult.calculateCorporateValue(financeValue, document)
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
                    () -> analysisResult.calculateCorporateValue(financeValue, document)
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
                    () -> analysisResult.calculateCorporateValue(financeValue, document)
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

    private Document defaultDocument() {
        return new Document(
                "documentId",
                null,
                null,
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
}