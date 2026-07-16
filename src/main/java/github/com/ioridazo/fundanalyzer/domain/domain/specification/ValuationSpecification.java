package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.ValuationDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.service.InvestmentIndicatorReconciliationService;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class ValuationSpecification {

    private static final int SECOND_DECIMAL_PLACE = 2;

    private static final Logger log = LogManager.getLogger(ValuationSpecification.class);

    private final ValuationDao valuationDao;
    private final CompanySpecification companySpecification;
    private final StockSpecification stockSpecification;
    private final InvestmentIndicatorReconciliationService investmentIndicatorReconciliationService;
    private final CorporateActionSpecification corporateActionSpecification;

    public ValuationSpecification(
            final ValuationDao valuationDao,
            final CompanySpecification companySpecification,
            final StockSpecification stockSpecification,
            final InvestmentIndicatorReconciliationService investmentIndicatorReconciliationService,
            final CorporateActionSpecification corporateActionSpecification) {
        this.valuationDao = valuationDao;
        this.companySpecification = companySpecification;
        this.stockSpecification = stockSpecification;
        this.investmentIndicatorReconciliationService = investmentIndicatorReconciliationService;
        this.corporateActionSpecification = corporateActionSpecification;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 最新の評価結果を取得する
     *
     * @param companyCode 企業コード
     * @param submitDate  提出日
     * @return 最新の評価結果
     */
    public Optional<ValuationEntity> findLatestValuation(final String companyCode, final LocalDate submitDate) {
        return valuationDao.selectByCodeAndSubmitDate(companyCode, submitDate).stream()
                .max(Comparator.comparing(ValuationEntity::getTargetDate));
    }

    /**
     * 提出日の評価結果を取得する
     *
     * @param companyCode 企業コード
     * @param submitDate  提出日
     * @return 最新の評価結果
     */
    public Optional<ValuationEntity> findValuationOfSubmitDate(final String companyCode, final LocalDate submitDate) {
        return valuationDao.selectByCodeAndSubmitDate(companyCode, submitDate).stream()
                .min(Comparator.comparing(ValuationEntity::getDaySinceSubmitDate));
    }

    /**
     * 最新の評価結果を取得する
     *
     * @param companyCode 企業コード
     * @return 最新の評価結果
     */
    public Optional<ValuationEntity> findLatestValuation(final String companyCode) {
        final List<ValuationEntity> valuationList = valuationDao.selectByCode(companyCode);
        // 最新の提出日を取得する
        final LocalDate latestSubmitDate = valuationList.stream()
                .max(Comparator.comparing(ValuationEntity::getSubmitDate))
                .map(ValuationEntity::getSubmitDate)
                .orElse(LocalDate.EPOCH);
        // 最新の提出日かつ最新の対象日を取得する
        return valuationList.stream()
                .filter(entity -> latestSubmitDate.equals(entity.getSubmitDate()))
                .max(Comparator.comparing(ValuationEntity::getTargetDate));
    }

    /**
     * 企業コードで全評価エンティティを取得する（重複排除なし）。
     *
     * @param companyCode 企業コード
     * @return 評価エンティティリスト（重複あり）
     */
    public List<ValuationEntity> findAllValuationEntities(final String companyCode) {
        return valuationDao.selectByCode(companyCode);
    }

    /**
     * 評価結果を取得する
     *
     * @param companyCode 企業コード
     * @return 評価結果リスト
     */
    public List<ValuationEntity> findValuation(final String companyCode) {
        final List<ValuationEntity> entityList = valuationDao.selectByCode(companyCode);
        return entityList.stream()
                .map(ValuationEntity::getTargetDate)
                .distinct()
                // 最新の提出日を取得する
                .map(targetDate -> entityList.stream()
                        .filter(e -> targetDate.equals(e.getTargetDate()))
                        .max(Comparator.comparing(ValuationEntity::getSubmitDate))
                        .orElseThrow()
                )
                .toList();
    }

    /**
     * 全件数を取得する（再計算バッチの事前確認用）。
     *
     * @return 全件数
     */
    public int countAll() {
        return valuationDao.countAll();
    }

    /**
     * analysis_result の企業価値（再計算後の現行係数値）を基に、割引値・割引率を一括更新する。
     *
     * @return 更新件数
     */
    public int updateDerivedValuesFromAnalysisResult() {
        return valuationDao.updateDerivedValuesFromAnalysisResult();
    }

    /**
     * 評価結果を登録する
     *
     * @param stock          株価
     * @param analysisResult 分析結果
     */
    public void insert(final StockPriceEntity stock, final AnalysisResultEntity analysisResult) {
        try {
            valuationDao.insert(evaluate(stock, analysisResult));
        } catch (final NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "一意制約違反のため、データベースへの登録をスキップします。" +
                                "\tテーブル名:{0}\t企業コード:{1}\t対象日付:{2}\t株価:{3}",
                                "valuation",
                                stock.getCompanyCode(),
                                stock.getTargetDate(),
                                stock.getStockPrice()
                        ),
                        companySpecification.findCompanyByCode(stock.getCompanyCode()).map(Company::edinetCode).orElse("null"),
                        Category.STOCK,
                        Process.EVALUATE
                ), e);
            } else if (e.contains(SQLIntegrityConstraintViolationException.class)) {
                log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "整合性制約 (外部キー、主キー、または一意キー) 違反のため、データベースへの登録をスキップします。" +
                                "\tテーブル名:{0}\t企業コード:{0}\t対象日付:{1}\t株価:{2}",
                                "valuation",
                                stock.getCompanyCode(),
                                stock.getTargetDate(),
                                stock.getStockPrice()
                        ),
                        companySpecification.findCompanyByCode(stock.getCompanyCode()).map(Company::edinetCode).orElse("null"),
                        Category.STOCK,
                        Process.EVALUATE
                ), e);
            } else {
                throw e;
            }
        }
    }

    /**
     * 株価を評価する
     *
     * @param stock          株価
     * @param analysisResult 分析結果
     * @return 評価結果
     */
    ValuationEntity evaluate(final StockPriceEntity stock, final AnalysisResultEntity analysisResult) {
        final String code = stock.getCompanyCode();
        final LocalDate targetDate = stock.getTargetDate();
        final BigDecimal adjustedStockPrice = corporateActionSpecification.adjustToBasis(
                BigDecimal.valueOf(stock.getStockPrice()),
                stock.getCompanyCode(),
                stock.getTargetDate(),
                analysisResult.getSubmitDate(),
                true
        );
        // グレアム指数は都度計算値（調整後株価 × 分析結果の書類から都度計算した bps/eps）から算出する。
        // investment_indicator への書き込みは停止しているため、紐づく ID は存在せず null を保存する
        final Optional<BigDecimal> grahamIndex = investmentIndicatorReconciliationService
                .reconcile(code, List.of(stock), List.of(analysisResult))
                .stream()
                .findFirst()
                .flatMap(IndicatorValue::getGrahamIndex);
        final LocalDate submitDate = analysisResult.getSubmitDate();
        final BigDecimal stockPriceOfSubmitDate = getStockPriceOfSubmitDate(code, submitDate);

        return ValuationEntity.of(
                code,
                submitDate,
                targetDate,
                stock.getId(),
                adjustedStockPrice,
                null,
                grahamIndex.orElse(null),
                ChronoUnit.DAYS.between(submitDate, targetDate),
                adjustedStockPrice.subtract(stockPriceOfSubmitDate),
                adjustedStockPrice.divide(stockPriceOfSubmitDate, SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP),
                analysisResult.getCorporateValue().subtract(adjustedStockPrice),
                analysisResult.getCorporateValue().divide(adjustedStockPrice, SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP),
                analysisResult.getId(),
                nowLocalDateTime()
        );
    }

    private BigDecimal getStockPriceOfSubmitDate(final String companyCode, final LocalDate submitDate) {
        // 過去のvaluationレコードから取得する
        final Optional<ValuationEntity> valuation = valuationDao.selectByCodeAndSubmitDate(companyCode, submitDate).stream()
                .min(Comparator.comparing(ValuationEntity::getDaySinceSubmitDate));
        if (valuation.isPresent()) {
            return valuation.get().getStockPrice();
        }

        // 上記になければ提出日の株価を取得する
        final Optional<Double> stock = stockSpecification.findStock(companyCode, submitDate).map(StockPriceEntity::getStockPrice);
        if (stock.isPresent()) {
            return BigDecimal.valueOf(stock.get());
        }

        // 上記になければ提出日より以前の平均株価を取得する
        final Optional<BigDecimal> averageStockPrice = stockSpecification.getAverageStockPrice(companyCode, submitDate);
        if (averageStockPrice.isPresent()) {
            return averageStockPrice.get();
        }

        throw new FundanalyzerNotExistException("提出日株価終値");
    }
}
