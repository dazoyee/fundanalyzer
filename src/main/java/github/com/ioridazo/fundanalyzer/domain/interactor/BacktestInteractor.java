package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CorporateActionSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ValuationSpecification;
import github.com.ioridazo.fundanalyzer.domain.service.BacktestCalculator;
import github.com.ioridazo.fundanalyzer.domain.usecase.BacktestUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Horizon;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.BacktestResult;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.EpisodeOutcome;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * バックテスト集計結果を構築する。
 */
@Component
public class BacktestInteractor implements BacktestUseCase {

    private final CompanySpecification companySpecification;
    private final ValuationSpecification valuationSpecification;
    private final StockSpecification stockSpecification;
    private final AnalysisResultSpecification analysisResultSpecification;
    private final CorporateActionSpecification corporateActionSpecification;

    @Value("${app.config.analysis.bucket-boundaries}")
    List<BigDecimal> bucketBoundaries;
    @Value("${app.config.analysis.match-tolerance-days}")
    int matchToleranceDays;

    public BacktestInteractor(
            final CompanySpecification companySpecification,
            final ValuationSpecification valuationSpecification,
            final StockSpecification stockSpecification,
            final AnalysisResultSpecification analysisResultSpecification,
            final CorporateActionSpecification corporateActionSpecification) {
        this.companySpecification = companySpecification;
        this.valuationSpecification = valuationSpecification;
        this.stockSpecification = stockSpecification;
        this.analysisResultSpecification = analysisResultSpecification;
        this.corporateActionSpecification = corporateActionSpecification;
    }

    /**
     * 全対象企業のバックテスト集計結果。
     *
     * @return バックテスト集計結果
     */
    @Override
    @Cacheable("backtest")
    public BacktestResult backtest() {
        final List<EpisodeOutcome> outcomes = new ArrayList<>();
        final EnumMap<Horizon, long[]> exclusions = initializeExclusions();

        for (final Company company : companySpecification.inquiryAllTargetCompanies()) {
            final List<ValuationEntity> valuations = valuationSpecification.findAllValuationEntities(company.code());
            if (valuations.isEmpty()) {
                continue;
            }

            final List<StockPriceEntity> stocks = stockSpecification.findEntityList(company.code());
            final List<CorporateActionSpecification.CorporateAction> actions =
                    corporateActionSpecification.findActions(company.code());
            final List<ValuationEntity> representativeValuations = selectRepresentativeValuations(valuations);
            final Map<Integer, Optional<BigDecimal>> analysisResultCache = new HashMap<>();

            for (final ValuationEntity valuation : representativeValuations) {
                final BigDecimal adjustedJudgePrice = valuation.getStockPrice();
                final BigDecimal judgeDiscountRate = valuation.getDiscountRate();
                if (adjustedJudgePrice == null || judgeDiscountRate == null || adjustedJudgePrice.signum() == 0) {
                    continue;
                }

                final Optional<BigDecimal> corporateValue = resolveCorporateValue(valuation, analysisResultCache);
                final LocalDate judgeDate = valuation.getTargetDate();
                final LocalDate submitBasis = valuation.getSubmitDate();

                for (final Horizon horizon : Horizon.values()) {
                    final LocalDate targetDate = judgeDate.plusDays(horizon.getDays());
                    final Optional<StockPriceEntity> stockPrice = findMatchedStockPrice(stocks, targetDate);
                    if (stockPrice.isEmpty() || stockPrice.orElseThrow().getStockPrice() == null) {
                        incrementExclusion(exclusions, horizon, company.lived());
                        continue;
                    }

                    final StockPriceEntity matchedStock = stockPrice.orElseThrow();
                    final BigDecimal adjustedTargetPrice = corporateActionSpecification.adjustToBasisWithActions(
                            BigDecimal.valueOf(matchedStock.getStockPrice()),
                            actions,
                            matchedStock.getTargetDate(),
                            submitBasis,
                            true
                    );
                    if (adjustedTargetPrice.signum() <= 0) {
                        continue;
                    }

                    final double returnRate = adjustedTargetPrice.divide(adjustedJudgePrice, MathContext.DECIMAL64)
                            .subtract(BigDecimal.ONE)
                            .doubleValue();
                    final Double convergence = calculateConvergence(
                            corporateValue.orElse(null),
                            judgeDiscountRate,
                            adjustedTargetPrice
                    );

                    outcomes.add(new EpisodeOutcome(
                            horizon,
                            judgeDiscountRate,
                            company.industryName(),
                            returnRate,
                            convergence
                    ));
                }
            }
        }

        return BacktestCalculator.aggregate(outcomes, exclusions, bucketBoundaries);
    }

    private EnumMap<Horizon, long[]> initializeExclusions() {
        final EnumMap<Horizon, long[]> exclusions = new EnumMap<>(Horizon.class);
        for (final Horizon horizon : Horizon.values()) {
            exclusions.put(horizon, new long[]{0L, 0L});
        }
        return exclusions;
    }

    private List<ValuationEntity> selectRepresentativeValuations(final List<ValuationEntity> valuations) {
        final Map<LocalDate, List<ValuationEntity>> groupedBySubmitDate = new HashMap<>();
        for (final ValuationEntity valuation : valuations) {
            groupedBySubmitDate.computeIfAbsent(valuation.getSubmitDate(), ignored -> new ArrayList<>()).add(valuation);
        }
        return groupedBySubmitDate.values().stream()
                .map(group -> group.stream()
                        .min(Comparator.comparingLong(ValuationEntity::getDaySinceSubmitDate))
                        .orElseThrow())
                .toList();
    }

    private Optional<BigDecimal> resolveCorporateValue(
            final ValuationEntity valuation, final Map<Integer, Optional<BigDecimal>> analysisResultCache) {
        final Integer analysisResultId = valuation.getAnalysisResultId();
        if (!analysisResultCache.containsKey(analysisResultId)) {
            final Optional<BigDecimal> corporateValue = analysisResultId == null
                    ? Optional.empty()
                    : analysisResultSpecification.findAnalysisResult(analysisResultId)
                            .map(AnalysisResultEntity::getCorporateValue);
            analysisResultCache.put(analysisResultId, corporateValue);
        }
        return analysisResultCache.getOrDefault(analysisResultId, Optional.empty());
    }

    private Optional<StockPriceEntity> findMatchedStockPrice(
            final List<StockPriceEntity> stocks, final LocalDate targetDate) {
        final LocalDate lastAllowedDate = targetDate.plusDays(matchToleranceDays);
        return stocks.stream()
                .filter(stock -> !stock.getTargetDate().isBefore(targetDate))
                .filter(stock -> !stock.getTargetDate().isAfter(lastAllowedDate))
                .min(Comparator.comparing(StockPriceEntity::getTargetDate));
    }

    private void incrementExclusion(
            final EnumMap<Horizon, long[]> exclusions, final Horizon horizon, final boolean lived) {
        final long[] counts = exclusions.get(horizon);
        if (lived) {
            counts[1]++;
            return;
        }
        counts[0]++;
    }

    private Double calculateConvergence(
            final BigDecimal corporateValue,
            final BigDecimal judgeDiscountRate,
            final BigDecimal adjustedTargetPrice) {
        if (corporateValue == null || judgeDiscountRate.subtract(BigDecimal.ONE).signum() == 0) {
            return null;
        }
        final double targetDiscountRate = corporateValue.doubleValue() / adjustedTargetPrice.doubleValue();
        return (judgeDiscountRate.doubleValue() - targetDiscountRate)
                / (judgeDiscountRate.doubleValue() - 1.0);
    }
}
