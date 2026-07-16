package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CorporateActionSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * investment_indicator への書き込みを行わず、株価と分析結果から投資指標（PER/PBR/グレアム指数）を
 * 読み取りの都度突合するドメインサービス。
 *
 * <p>対象日に対して提出日が対象日以前で最新の {@link AnalysisResultEntity} を選び、その書類の財務諸表値から
 * 都度計算した BPS/EPS と、コーポレートアクション補正後株価から {@link IndicatorValue} を構築する。
 * 詳細画面チャート・corporate_view 生成・valuation 評価の3経路で共用する。
 */
@Component
public class InvestmentIndicatorReconciliationService {

    private static final Logger log = LogManager.getLogger(InvestmentIndicatorReconciliationService.class);

    private final DocumentSpecification documentSpecification;
    private final FinancialStatementSpecification financialStatementSpecification;
    private final CorporateActionSpecification corporateActionSpecification;

    public InvestmentIndicatorReconciliationService(
            final DocumentSpecification documentSpecification,
            final FinancialStatementSpecification financialStatementSpecification,
            final CorporateActionSpecification corporateActionSpecification) {
        this.documentSpecification = documentSpecification;
        this.financialStatementSpecification = financialStatementSpecification;
        this.corporateActionSpecification = corporateActionSpecification;
    }

    /**
     * 株価リストの各対象日について、提出日が対象日以前で最新の分析結果と突合し投資指標を構築する。
     *
     * <p>書類・財務諸表値の解決は対象書類数分の小ループで一度だけ行い（N+1 回避）、
     * 株価日次ループの内側では追加のDB問い合わせを行わない。
     *
     * @param companyCode        企業コード
     * @param stockPriceList     株価エンティティリスト（対象日ごとに1件）
     * @param analysisResultList 分析結果エンティティリスト（候補集合。提出日順である必要はない）
     * @return 対応する分析結果が存在した対象日についてのみ構築された投資指標リスト（順序は stockPriceList に従う）
     */
    public List<IndicatorValue> reconcile(
            final String companyCode,
            final List<StockPriceEntity> stockPriceList,
            final List<AnalysisResultEntity> analysisResultList) {
        if (stockPriceList.isEmpty() || analysisResultList.isEmpty()) {
            return List.of();
        }

        final List<AnalysisResultEntity> sortedBySubmitDate = analysisResultList.stream()
                .sorted(Comparator.comparing(AnalysisResultEntity::getSubmitDate))
                .toList();

        final Map<String, AnalysisResult> computedByDocumentId = resolveAnalysisResults(sortedBySubmitDate);
        final List<CorporateActionSpecification.CorporateAction> actions = corporateActionSpecification.findActions(companyCode);

        return stockPriceList.stream()
                .map(stockPrice -> reconcileOne(
                        stockPrice.getTargetDate(),
                        BigDecimal.valueOf(stockPrice.getStockPrice()),
                        sortedBySubmitDate,
                        computedByDocumentId,
                        actions))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * 事前に計算済みの分析結果（都度計算値）を用いて、株価1件分の投資指標を構築する。
     *
     * <p>呼び出し元が既に対象書類の {@link Document} / 財務諸表値を解決済みで {@link AnalysisResult} を
     * 構築している場合に使う。{@link DocumentSpecification#findDocument} や
     * {@link FinancialStatementSpecification#getFinanceValue} を再実行しないため、
     * 同一書類に対する二重計算（企業ごとの重い財務諸表参照の重複実行）を避けられる。
     *
     * <p>{@code reconcile} と異なり、提出日が対象日以前かの判定のみをここで行い、
     * 複数書類からの最新選択（切替境界の判定）は呼び出し元が既に済ませていることを前提とする。
     *
     * @param companyCode               企業コード
     * @param stockPrice                株価エンティティ（対象日1件）
     * @param analysisResultEntity      事前計算に使った分析結果エンティティ（提出日の判定に使用）
     * @param precomputedAnalysisResult 事前計算済みの分析結果（都度計算値）
     * @return 提出日が対象日以前であれば構築された投資指標。提出日が対象日より後（対応する分析結果なし）であれば空
     */
    public Optional<IndicatorValue> reconcilePrecomputed(
            final String companyCode,
            final StockPriceEntity stockPrice,
            final AnalysisResultEntity analysisResultEntity,
            final AnalysisResult precomputedAnalysisResult) {
        if (analysisResultEntity.getSubmitDate().isAfter(
                stockPrice.getTargetDate())) {
            return Optional.empty();
        }
        final List<CorporateActionSpecification.CorporateAction> actions =
                corporateActionSpecification.findActions(companyCode);
        return Optional.of(buildIndicatorValue(
                stockPrice.getTargetDate(),
                BigDecimal.valueOf(stockPrice.getStockPrice()),
                analysisResultEntity.getSubmitDate(),
                precomputedAnalysisResult,
                actions));
    }

    /**
     * 書類ごとの分析結果（BPS/EPS 都度計算値）を対象書類数分の小ループで解決する。
     *
     * <p>書類が解決できない行（データ不整合）は warn ログを出して対象から除外し、処理は継続する。
     *
     * @param sortedBySubmitDate 提出日昇順の分析結果エンティティリスト
     * @return 書類IDをキーとした分析結果マップ（解決できた書類のみ）
     */
    private Map<String, AnalysisResult> resolveAnalysisResults(final List<AnalysisResultEntity> sortedBySubmitDate) {
        final Map<String, AnalysisResult> computedByDocumentId = new HashMap<>();
        for (final AnalysisResultEntity entity : sortedBySubmitDate) {
            if (computedByDocumentId.containsKey(entity.getDocumentId())) {
                continue;
            }
            try {
                final Document document = documentSpecification.findDocument(entity.getDocumentId());
                computedByDocumentId.put(
                        entity.getDocumentId(),
                        AnalysisResult.of(entity, financialStatementSpecification.getFinanceValue(document), document));
            } catch (final FundanalyzerNotExistException e) {
                log.warn(FundanalyzerLogClient.toSpecificationLogObject(
                        "書類が存在しないため、投資指標の突合対象から除外しました。\t書類ID:" + entity.getDocumentId(),
                        Category.ANALYSIS,
                        Process.INDICATE
                ), e);
            }
        }
        return computedByDocumentId;
    }

    private Optional<IndicatorValue> reconcileOne(
            final LocalDate targetDate,
            final BigDecimal stockPrice,
            final List<AnalysisResultEntity> sortedBySubmitDate,
            final Map<String, AnalysisResult> computedByDocumentId,
            final List<CorporateActionSpecification.CorporateAction> actions) {

        final Optional<AnalysisResultEntity> effective = sortedBySubmitDate.stream()
                .filter(entity -> !entity.getSubmitDate().isAfter(targetDate))
                .max(Comparator.comparing(AnalysisResultEntity::getSubmitDate));
        if (effective.isEmpty()) {
            return Optional.empty();
        }

        final AnalysisResultEntity entity = effective.get();
        final AnalysisResult computed = computedByDocumentId.get(entity.getDocumentId());
        if (computed == null) {
            return Optional.empty();
        }

        return Optional.of(buildIndicatorValue(targetDate, stockPrice, entity.getSubmitDate(), computed, actions));
    }

    private IndicatorValue buildIndicatorValue(
            final LocalDate targetDate,
            final BigDecimal stockPrice,
            final LocalDate submitDate,
            final AnalysisResult computed,
            final List<CorporateActionSpecification.CorporateAction> actions) {
        final BigDecimal adjustedStockPrice = corporateActionSpecification.adjustToBasisWithActions(
                stockPrice, actions, targetDate, submitDate, true);
        return IndicatorValue.of(adjustedStockPrice, computed, targetDate);
    }
}
