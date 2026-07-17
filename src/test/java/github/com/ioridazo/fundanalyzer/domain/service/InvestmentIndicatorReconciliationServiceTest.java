package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CorporateActionSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("InvestmentIndicatorReconciliationService のテスト")
class InvestmentIndicatorReconciliationServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.parse("2024-01-01T00:00:00");

    private DocumentSpecification documentSpecification;
    private FinancialStatementSpecification financialStatementSpecification;
    private CorporateActionSpecification corporateActionSpecification;
    private InvestmentIndicatorReconciliationService service;

    @BeforeEach
    void setUp() {
        documentSpecification = mock(DocumentSpecification.class);
        financialStatementSpecification = mock(FinancialStatementSpecification.class);
        corporateActionSpecification = mock(CorporateActionSpecification.class);
        service = new InvestmentIndicatorReconciliationService(
                documentSpecification, financialStatementSpecification, corporateActionSpecification);

        lenient().when(corporateActionSpecification.findActions(any())).thenReturn(List.of());
        // 既定では補正なし（引数の株価をそのまま返す）
        lenient().when(corporateActionSpecification.adjustToBasisWithActions(any(), any(), any(), any(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("reconcile メソッド")
    class Reconcile {

        @DisplayName("対象日に対しsubmitDate<=対象日で最新のAnalysisResultEntityが選ばれる（複数書類の切替境界を含む）")
        @Test
        void selectsLatestAnalysisResultAtOrBeforeTargetDate() {
            final AnalysisResultEntity entityA = analysisResultEntity(
                    "docA", LocalDate.parse("2024-01-10"), BigDecimal.valueOf(1000));
            final AnalysisResultEntity entityB = analysisResultEntity(
                    "docB", LocalDate.parse("2024-04-10"), BigDecimal.valueOf(2000));
            stubDocument("docA", LocalDate.parse("2024-01-10"), 1000L, 10L, 100L);
            stubDocument("docB", LocalDate.parse("2024-04-10"), 1000L, 10L, 100L);

            final List<StockPriceEntity> stockPriceList = List.of(
                    stockPrice(LocalDate.parse("2024-01-10"), 100.0d), // docA の提出日ちょうど
                    stockPrice(LocalDate.parse("2024-04-09"), 100.0d), // docB 提出前日 → まだ docA
                    stockPrice(LocalDate.parse("2024-04-10"), 100.0d)  // docB の提出日ちょうど → 切替
            );

            final List<IndicatorValue> actual = service.reconcile("code", stockPriceList, List.of(entityA, entityB));

            assertEquals(3, actual.size());
            assertEquals(0, BigDecimal.valueOf(100).divide(BigDecimal.valueOf(1000)).compareTo(actual.get(0).getPriceCorporateValueRatio()));
            assertEquals(0, BigDecimal.valueOf(100).divide(BigDecimal.valueOf(1000)).compareTo(actual.get(1).getPriceCorporateValueRatio()));
            assertEquals(0, BigDecimal.valueOf(100).divide(BigDecimal.valueOf(2000)).compareTo(actual.get(2).getPriceCorporateValueRatio()));
        }

        @DisplayName("提出日前の株価（対応する分析結果なし）は除外される")
        @Test
        void excludesStockPriceBeforeAnySubmitDate() {
            final AnalysisResultEntity entity = analysisResultEntity(
                    "docA", LocalDate.parse("2024-01-10"), BigDecimal.valueOf(1000));
            stubDocument("docA", LocalDate.parse("2024-01-10"), 1000L, 10L, 100L);

            final List<StockPriceEntity> stockPriceList = List.of(
                    stockPrice(LocalDate.parse("2024-01-05"), 100.0d), // 提出日より前 → 対応する分析結果なし
                    stockPrice(LocalDate.parse("2024-01-10"), 100.0d)
            );

            final List<IndicatorValue> actual = service.reconcile("code", stockPriceList, List.of(entity));

            assertEquals(1, actual.size());
            assertEquals(LocalDate.parse("2024-01-10"), actual.get(0).getTargetDate());
        }

        @DisplayName("書類が存在しない分析結果は当該対象日のみ除外され、他の対象日の処理は継続される")
        @Test
        void excludesOnlyTargetDateWhenDocumentMissing() {
            final AnalysisResultEntity missingDocEntity = analysisResultEntity(
                    "docMissing", LocalDate.parse("2024-01-10"), BigDecimal.valueOf(1000));
            final AnalysisResultEntity okEntity = analysisResultEntity(
                    "docOk", LocalDate.parse("2024-04-10"), BigDecimal.valueOf(2000));
            when(documentSpecification.findDocument("docMissing"))
                    .thenThrow(new FundanalyzerNotExistException("書類"));
            stubDocument("docOk", LocalDate.parse("2024-04-10"), 1000L, 10L, 100L);

            final List<StockPriceEntity> stockPriceList = List.of(
                    stockPrice(LocalDate.parse("2024-02-01"), 100.0d), // 実効書類は docMissing → 除外
                    stockPrice(LocalDate.parse("2024-05-01"), 100.0d)  // 実効書類は docOk → 継続して構築
            );

            final List<IndicatorValue> actual = assertDoesNotThrow(
                    () -> service.reconcile("code", stockPriceList, List.of(missingDocEntity, okEntity)));

            assertEquals(1, actual.size());
            assertEquals(LocalDate.parse("2024-05-01"), actual.get(0).getTargetDate());
        }

        @DisplayName("財務諸表値（bps/eps算出科目）が欠損していても除外されず、指標のみnullとして伝播する")
        @Test
        void nullBpsEpsPropagateWithoutExcludingTheEntry() {
            final AnalysisResultEntity entity = analysisResultEntity(
                    "docA", LocalDate.parse("2024-01-10"), BigDecimal.valueOf(500));
            final Document document = document("docA", LocalDate.parse("2024-01-10"), null);
            when(documentSpecification.findDocument("docA")).thenReturn(document);
            // 純資産・株式総数・当期純利益のいずれも欠損 → bps/eps は算出できない
            when(financialStatementSpecification.getFinanceValue(document))
                    .thenReturn(FinanceValue.of(null, null, null, null, null, null, null, null, null, null, null));

            final List<StockPriceEntity> stockPriceList = List.of(stockPrice(LocalDate.parse("2024-01-10"), 100.0d));

            final List<IndicatorValue> actual = service.reconcile("code", stockPriceList, List.of(entity));

            assertEquals(1, actual.size());
            assertTrue(actual.get(0).getPer().isEmpty(), "eps欠損によりPERはnull（空）になる");
            assertTrue(actual.get(0).getPbr().isEmpty(), "bps欠損によりPBRはnull（空）になる");
            assertTrue(actual.get(0).getGrahamIndex().isEmpty(), "PER/PBRがnullのためグレアム指数もnull（空）になる");
        }

        @DisplayName("corporateActionSpecificationの補正（adjustToBasisWithActions）が適用される")
        @Test
        void appliesCorporateActionAdjustment() {
            final AnalysisResultEntity entity = analysisResultEntity(
                    "docA", LocalDate.parse("2024-01-10"), BigDecimal.valueOf(1000));
            stubDocument("docA", LocalDate.parse("2024-01-10"), 1000L, 10L, 100L);
            final List<CorporateActionSpecification.CorporateAction> actions = List.of(
                    new CorporateActionSpecification.CorporateAction(LocalDate.parse("2024-01-05"), BigDecimal.valueOf(2), true));
            when(corporateActionSpecification.findActions("code")).thenReturn(actions);
            when(corporateActionSpecification.adjustToBasisWithActions(
                    eq(BigDecimal.valueOf(100.0d)), eq(actions), eq(LocalDate.parse("2024-01-10")), eq(LocalDate.parse("2024-01-10")), eq(true)))
                    .thenReturn(BigDecimal.valueOf(200));

            final List<IndicatorValue> actual = service.reconcile(
                    "code", List.of(stockPrice(LocalDate.parse("2024-01-10"), 100.0d)), List.of(entity));

            assertEquals(1, actual.size());
            // 補正後株価(200) / 企業価値(1000) が使われること（生の株価100を使った場合の0.1ではなく0.2になる）
            assertEquals(0, BigDecimal.valueOf(0.2).compareTo(actual.get(0).getPriceCorporateValueRatio()));
            verify(corporateActionSpecification, times(1))
                    .adjustToBasisWithActions(eq(BigDecimal.valueOf(100.0d)), eq(actions), any(), any(), eq(true));
        }

        @DisplayName("stockPriceListが空の場合は空リストを返し、他の依存には問い合わせない")
        @Test
        void emptyStockPriceListReturnsEmptyList() {
            final AnalysisResultEntity entity = analysisResultEntity(
                    "docA", LocalDate.parse("2024-01-10"), BigDecimal.valueOf(1000));

            final List<IndicatorValue> actual = service.reconcile("code", List.of(), List.of(entity));

            assertEquals(List.of(), actual);
            verifyNoInteractions(documentSpecification);
            verifyNoInteractions(financialStatementSpecification);
        }

        @DisplayName("analysisResultListが空の場合は空リストを返し、他の依存には問い合わせない")
        @Test
        void emptyAnalysisResultListReturnsEmptyList() {
            final List<IndicatorValue> actual = service.reconcile(
                    "code", List.of(stockPrice(LocalDate.parse("2024-01-10"), 100.0d)), List.of());

            assertEquals(List.of(), actual);
            verifyNoInteractions(documentSpecification);
            verifyNoInteractions(financialStatementSpecification);
        }

        private void stubDocument(
                final String documentId, final LocalDate submitDate,
                final Long netAssets, final Long numberOfShares, final Long netIncome) {
            final Document document = document(documentId, submitDate, null);
            when(documentSpecification.findDocument(documentId)).thenReturn(document);
            when(financialStatementSpecification.getFinanceValue(document)).thenReturn(
                    FinanceValue.of(null, null, null, null, null, null, netAssets, null, null, netIncome, numberOfShares));
        }
    }

    @Nested
    @DisplayName("reconcilePrecomputed メソッド")
    class ReconcilePrecomputed {

        @DisplayName("提出日が対象日以前のとき、事前計算済みのAnalysisResultで投資指標を構築し、書類・財務諸表値の再解決は行わない")
        @Test
        void buildsIndicatorFromPrecomputedResultWithoutReResolvingDocument() {
            final AnalysisResultEntity entity = analysisResultEntity(
                    "docA", LocalDate.parse("2024-01-10"), BigDecimal.valueOf(1000));
            final AnalysisResult precomputed = new AnalysisResult(
                    BigDecimal.valueOf(1000), BigDecimal.valueOf(100), BigDecimal.valueOf(20),
                    null, null, LocalDate.parse("2024-01-10"), "docA");
            final StockPriceEntity stockPrice = stockPrice(LocalDate.parse("2024-01-10"), 100.0d);

            final Optional<IndicatorValue> actual = service.reconcilePrecomputed("code", stockPrice, entity, precomputed);

            assertTrue(actual.isPresent());
            assertEquals(0, BigDecimal.valueOf(0.1).compareTo(actual.get().getPriceCorporateValueRatio()));
            assertEquals(0, BigDecimal.valueOf(5).compareTo(actual.get().getPer().orElseThrow())); // 100 / 20
            assertEquals(0, BigDecimal.valueOf(1).compareTo(actual.get().getPbr().orElseThrow())); // 100 / 100
            verify(documentSpecification, never()).findDocument(any(String.class));
            verify(financialStatementSpecification, never()).getFinanceValue(any());
        }

        @DisplayName("提出日が対象日より後のときは空を返し、コーポレートアクション補正も行わない")
        @Test
        void returnsEmptyWhenSubmitDateIsAfterTargetDate() {
            final AnalysisResultEntity entity = analysisResultEntity(
                    "docA", LocalDate.parse("2024-04-10"), BigDecimal.valueOf(1000));
            final AnalysisResult precomputed = new AnalysisResult(
                    BigDecimal.valueOf(1000), BigDecimal.valueOf(100), BigDecimal.valueOf(20),
                    null, null, LocalDate.parse("2024-04-10"), "docA");
            final StockPriceEntity stockPrice = stockPrice(LocalDate.parse("2024-01-10"), 100.0d);

            final Optional<IndicatorValue> actual = service.reconcilePrecomputed("code", stockPrice, entity, precomputed);

            assertEquals(Optional.empty(), actual);
            verifyNoInteractions(corporateActionSpecification);
            verifyNoInteractions(documentSpecification);
            verifyNoInteractions(financialStatementSpecification);
        }
    }

    private static AnalysisResultEntity analysisResultEntity(
            final String documentId, final LocalDate submitDate, final BigDecimal corporateValue) {
        return new AnalysisResultEntity(
                null,
                "code",
                LocalDate.parse("2024-03-31"),
                corporateValue,
                null,
                "120",
                null,
                submitDate,
                documentId,
                CREATED_AT
        );
    }

    private static Document document(final String documentId, final LocalDate submitDate, final QuarterType quarterType) {
        return new Document(
                documentId,
                null,
                quarterType,
                "edinetCode",
                null,
                submitDate,
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

    private static StockPriceEntity stockPrice(final LocalDate targetDate, final double price) {
        return new StockPriceEntity(
                null,
                "code",
                targetDate,
                price,
                null, null, null, null, null, null, null, null, null, null, null, null,
                CREATED_AT,
                CREATED_AT
        );
    }
}
