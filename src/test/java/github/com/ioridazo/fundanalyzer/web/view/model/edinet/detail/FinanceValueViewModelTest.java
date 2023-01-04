package github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.BsSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.PlSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FinanceValueViewModelTest {

    private final List<BsSubjectEntity> bsSubjectEntityList = List.of(
            new BsSubjectEntity("1", "1", "11", "流動資産合計"),
            new BsSubjectEntity("2", "4", "12", "投資その他の資産合計"),
            new BsSubjectEntity("3", "7", "14", "資産合計"),
            new BsSubjectEntity("4", "8", "15", "流動負債合計"),
            new BsSubjectEntity("5", "9", "16", "固定負債合計"),
            new BsSubjectEntity("6", "14", "17", "純資産合計"),
            new BsSubjectEntity("7", "16", "18", "新株予約権")
    );

    private final List<PlSubjectEntity> plSubjectEntityList = List.of(
            new PlSubjectEntity("1", "3", "11", "営業利益"),
            new PlSubjectEntity("2", "11", "12", "当期純利益")
    );

    private final FinanceValue financeValue = FinanceValue.of(
            101L,
            102L,
            103L,
            104L,
            105L,
            106L,
            107L,
            108L,
            109L,
            110L
    );

    @DisplayName("of : マッピングを確認する")
    @Test
    void of() {
        var actual = FinanceValueViewModel.of(
                bsSubjectEntityList,
                plSubjectEntityList,
                financeValue
        );

        assertAll(
                () -> assertAll("流動資産合計",
                        () -> assertEquals("1", actual.totalCurrentAssets().financialStatementId(), "流動資産合計"),
                        () -> assertEquals("1", actual.totalCurrentAssets().subjectId(), "流動資産合計"),
                        () -> assertEquals("流動資産合計", actual.totalCurrentAssets().name(), "流動資産合計"),
                        () -> assertEquals(101L, actual.totalCurrentAssets().value(), "流動資産合計")
                ),
                () -> assertAll("投資その他の資産合計",
                        () -> assertEquals("1", actual.totalInvestmentsAndOtherAssets().financialStatementId(), "投資その他の資産合計"),
                        () -> assertEquals("2", actual.totalInvestmentsAndOtherAssets().subjectId(), "投資その他の資産合計"),
                        () -> assertEquals("投資その他の資産合計", actual.totalInvestmentsAndOtherAssets().name(), "投資その他の資産合計"),
                        () -> assertEquals(102L, actual.totalInvestmentsAndOtherAssets().value(), "投資その他の資産合計")
                ),
                () -> assertAll("総資産",
                        () -> assertEquals("1", actual.totalAssets().financialStatementId(), "総資産"),
                        () -> assertEquals("3", actual.totalAssets().subjectId(), "総資産"),
                        () -> assertEquals("資産合計", actual.totalAssets().name(), "総資産"),
                        () -> assertEquals(103L, actual.totalAssets().value(), "総資産")
                ),
                () -> assertAll("流動負債合計",
                        () -> assertEquals("1", actual.totalCurrentLiabilities().financialStatementId(), "流動負債合計"),
                        () -> assertEquals("4", actual.totalCurrentLiabilities().subjectId(), "流動負債合計"),
                        () -> assertEquals("流動負債合計", actual.totalCurrentLiabilities().name(), "流動負債合計"),
                        () -> assertEquals(104L, actual.totalCurrentLiabilities().value(), "流動負債合計")
                ),
                () -> assertAll("固定負債合計",
                        () -> assertEquals("1", actual.totalFixedLiabilities().financialStatementId(), "固定負債合計"),
                        () -> assertEquals("5", actual.totalFixedLiabilities().subjectId(), "固定負債合計"),
                        () -> assertEquals("固定負債合計", actual.totalFixedLiabilities().name(), "固定負債合計"),
                        () -> assertEquals(105L, actual.totalFixedLiabilities().value(), "固定負債合計")
                ),
                () -> assertAll("新株予約権",
                        () -> assertEquals("1", actual.subscriptionWarrant().financialStatementId(), "新株予約権"),
                        () -> assertEquals("7", actual.subscriptionWarrant().subjectId(), "新株予約権"),
                        () -> assertEquals("新株予約権", actual.subscriptionWarrant().name(), "新株予約権"),
                        () -> assertEquals(106L, actual.subscriptionWarrant().value(), "新株予約権")
                ),
                () -> assertAll("純資産",
                        () -> assertEquals("1", actual.netAssets().financialStatementId(), "純資産"),
                        () -> assertEquals("6", actual.netAssets().subjectId(), "純資産"),
                        () -> assertEquals("純資産合計", actual.netAssets().name(), "純資産"),
                        () -> assertEquals(107L, actual.netAssets().value(), "純資産")
                ),
                () -> assertAll("営業利益",
                        () -> assertEquals("2", actual.operatingProfit().financialStatementId(), "営業利益"),
                        () -> assertEquals("1", actual.operatingProfit().subjectId(), "営業利益"),
                        () -> assertEquals("営業利益", actual.operatingProfit().name(), "営業利益"),
                        () -> assertEquals(108L, actual.operatingProfit().value(), "営業利益")
                ),
                () -> assertAll("当期純利益",
                        () -> assertEquals("2", actual.netIncome().financialStatementId(), "当期純利益"),
                        () -> assertEquals("2", actual.netIncome().subjectId(), "当期純利益"),
                        () -> assertEquals("当期純利益", actual.netIncome().name(), "当期純利益"),
                        () -> assertEquals(109L, actual.netIncome().value(), "当期純利益")
                ),
                () -> assertAll("株式総数",
                        () -> assertEquals("4", actual.numberOfShares().financialStatementId(), "株式総数"),
                        () -> assertEquals("0", actual.numberOfShares().subjectId(), "株式総数"),
                        () -> assertEquals("株式総数", actual.numberOfShares().name(), "株式総数"),
                        () -> assertEquals(110L, actual.numberOfShares().value(), "株式総数")
                )
        );
    }
}