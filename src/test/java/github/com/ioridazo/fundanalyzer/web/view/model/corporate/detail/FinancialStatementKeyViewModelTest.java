package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.CreatedType;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("FinancialStatementKeyViewModelのテスト")
class FinancialStatementKeyViewModelTest {

    @DisplayName("periodLabel : 有価証券報告書は期間をそのまま表示する")
    @Test
    void periodLabel_annualReport() {
        final FinancialStatementKeyViewModel actual = FinancialStatementKeyViewModel.of(
                entity(DocumentTypeCode.DTC_120));

        assertEquals("2025-01-01 〜 2025-12-31", actual.periodLabel());
    }

    @DisplayName("periodLabel : 半期報告書は事業年度の中間期であることを明示する")
    @Test
    void periodLabel_semiannualReport() {
        final FinancialStatementKeyViewModel actual = FinancialStatementKeyViewModel.of(
                entity(DocumentTypeCode.DTC_160));

        assertEquals("事業年度 2025-01-01 〜 2025-12-31 の中間期", actual.periodLabel());
    }

    @DisplayName("periodLabel : 訂正半期報告書も事業年度の中間期であることを明示する")
    @Test
    void periodLabel_correctedSemiannualReport() {
        final FinancialStatementKeyViewModel actual = FinancialStatementKeyViewModel.of(
                entity(DocumentTypeCode.DTC_170));

        assertEquals("事業年度 2025-01-01 〜 2025-12-31 の中間期", actual.periodLabel());
    }

    @DisplayName("periodLabel : 四半期報告書は期間をそのまま表示する")
    @Test
    void periodLabel_quarterlyReport() {
        final FinancialStatementKeyViewModel actual = FinancialStatementKeyViewModel.of(
                entity(DocumentTypeCode.DTC_140));

        assertEquals("2025-01-01 〜 2025-12-31", actual.periodLabel());
    }

    private FinancialStatementEntity entity(final DocumentTypeCode documentTypeCode) {
        return new FinancialStatementEntity(
                1,
                "1234",
                "E12345",
                FinancialStatementEnum.BALANCE_SHEET.getId(),
                "1",
                LocalDate.parse("2025-01-01"),
                LocalDate.parse("2025-12-31"),
                100L,
                documentTypeCode.toValue(),
                null,
                LocalDate.parse("2026-03-27"),
                "DOC001",
                CreatedType.AUTO.toValue(),
                LocalDateTime.parse("2026-03-27T00:00:00")
        );
    }
}
