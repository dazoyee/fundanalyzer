package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisResultEntityTest {

    @Test
    @DisplayName("AnalysisResultEntity.of : DTC_160 で quarterType が null のとき例外")
    void semiannual_requires_quarter_type() {
        assertThrows(FundanalyzerNotExistException.class, () -> AnalysisResultEntity.of(
                "12345",
                LocalDate.parse("2026-09-30"),
                BigDecimal.ONE,
                null,
                DocumentTypeCode.DTC_160,
                QuarterType.QT_OTHER,
                LocalDate.parse("2026-11-15"),
                "S0000001",
                LocalDateTime.parse("2026-07-16T00:00:00")
        ));
    }

    @Test
    @DisplayName("AnalysisResultEntity.of : DTC_160 で quarterType=H のとき通る")
    void semiannual_accepts_h() {
        assertDoesNotThrow(() -> AnalysisResultEntity.of(
                "12345",
                LocalDate.parse("2026-09-30"),
                BigDecimal.ONE,
                null,
                DocumentTypeCode.DTC_160,
                QuarterType.QT_SEMI,
                LocalDate.parse("2026-11-15"),
                "S0000001",
                LocalDateTime.parse("2026-07-16T00:00:00")
        ));
    }
}
