package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 係数一括再計算バッチの新設 SQL（相関サブクエリ UPDATE・ROUND の丸め挙動）を、
 * 実際の H2（dev プロファイル・MySQL 互換モード）に対して検証する統合テスト。
 *
 * <p>{@code @Transactional} によりテスト終了時に自動ロールバックするため、
 * 他テストが共有する in-memory H2 インスタンスを汚染しない。
 */
@SpringBootTest
@Transactional
class RecalculationSqlIntegrationTest {

    @Autowired
    private DocumentDao documentDao;
    @Autowired
    private AnalysisResultDao analysisResultDao;
    @Autowired
    private ValuationDao valuationDao;

    @DisplayName("updateCorporateValueAndRimValue → updateDerivedValuesFromAnalysisResult : "
                 + "企業価値の再計算結果が valuation の割引値・割引率に正しく反映される")
    @Test
    void recalculationSqlAppliesToValuation() {
        final String documentId = "rc000001";
        final String companyCode = "99999";

        // 1. document（FK 先。edinet_code は null 許容のため会社マスタ不要）
        documentDao.insert(DocumentEntity.builder()
                .documentId(documentId)
                .submitDate(LocalDate.now())
                .build());

        // 2. analysis_result（再計算前の凍結値。corporate_value=1000, rim_value=null）
        final AnalysisResultEntity inserted = analysisResultDao.insert(AnalysisResultEntity.of(
                companyCode,
                LocalDate.now(),
                BigDecimal.valueOf(1000),
                null,
                null,
                null,
                null,
                null,
                DocumentTypeCode.DTC_120,
                QuarterType.QT_OTHER,
                LocalDate.now(),
                documentId,
                LocalDateTime.now()
        )).getEntity();
        final Integer analysisResultId = inserted.getId();

        // 3. valuation（stock_price=200 に対し、再計算前の古い割引値・割引率を仮に保持させておく）
        valuationDao.insert(ValuationEntity.of(
                companyCode,
                LocalDate.now(),
                LocalDate.now(),
                null,
                BigDecimal.valueOf(200),
                null,
                null,
                10L,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.valueOf(9999),
                BigDecimal.valueOf(9999),
                analysisResultId,
                LocalDateTime.now()
        ));

        // 4. ストリームAの流れを再現: 企業価値を新係数の値(1300)に更新してから valuation を一括更新する
        final BigDecimal newCorporateValue = BigDecimal.valueOf(1300);
        final int updatedRows = analysisResultDao.updateCorporateValueAndRimValue(
                analysisResultId, newCorporateValue, null);
        assertEquals(1, updatedRows);

        final int valuationUpdatedRows = valuationDao.updateDerivedValuesFromAnalysisResult();
        assertTrue(valuationUpdatedRows >= 1);

        // 5. 検証: discount_value = 1300 - 200 = 1100, discount_rate = round(1300/200, 2) = 6.50
        final List<ValuationEntity> actualList = valuationDao.selectByCode(companyCode);
        final Optional<ValuationEntity> actual = actualList.stream()
                .filter(v -> analysisResultId.equals(v.getAnalysisResultId()))
                .findFirst();
        assertTrue(actual.isPresent());

        final BigDecimal expectedDiscountValue = newCorporateValue.subtract(BigDecimal.valueOf(200));
        final BigDecimal expectedDiscountRate =
                newCorporateValue.divide(BigDecimal.valueOf(200), 2, RoundingMode.HALF_UP);

        // FLOAT 列に保存されるため、BigDecimal#compareTo の完全一致ではなく実用上十分な許容誤差で比較する
        assertEquals(expectedDiscountValue.doubleValue(), actual.get().getDiscountValue().doubleValue(), 0.01);
        assertEquals(expectedDiscountRate.doubleValue(), actual.get().getDiscountRate().doubleValue(), 0.01);
    }
}
