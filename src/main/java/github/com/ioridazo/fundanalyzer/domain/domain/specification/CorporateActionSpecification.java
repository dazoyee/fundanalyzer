package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 株式分割・併合の調整係数を導出する。
 */
@Component
public class CorporateActionSpecification {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;

    private final CompanySpecification companySpecification;
    private final FinancialStatementSpecification financialStatementSpecification;
    private final StockPriceDao stockPriceDao;
    private final BigDecimal cliffThresholdRatio;
    private final BigDecimal ratioTolerance;

    /**
     * 必要な依存と設定値を初期化する。
     *
     * @param companySpecification            企業Specification
     * @param financialStatementSpecification 財務諸表Specification
     * @param stockPriceDao                   株価DAO
     * @param cliffThresholdRatio             分割クリフ判定閾値
     * @param ratioTolerance                  比率判定許容誤差
     */
    public CorporateActionSpecification(
            final CompanySpecification companySpecification,
            final FinancialStatementSpecification financialStatementSpecification,
            final StockPriceDao stockPriceDao,
            @Value("${app.config.stock.corporate-action.cliff-threshold-ratio}") final double cliffThresholdRatio,
            @Value("${app.config.stock.corporate-action.ratio-tolerance}") final double ratioTolerance) {
        this.companySpecification = companySpecification;
        this.financialStatementSpecification = financialStatementSpecification;
        this.stockPriceDao = stockPriceDao;
        this.cliffThresholdRatio = BigDecimal.valueOf(cliffThresholdRatio);
        this.ratioTolerance = BigDecimal.valueOf(ratioTolerance);
    }

    /**
     * 施行日時点までの累積株式数倍率を返す。
     *
     * @param companyCode 会社コード
     * @param date        基準日
     * @return 累積株式数倍率
     */
    public BigDecimal sharesFactorAt(final String companyCode, final LocalDate date) {
        return sharesFactorAt(companyCode, date, false);
    }

    /**
     * 施行日時点までの累積株式数倍率を返す。
     *
     * @param companyCode   会社コード
     * @param date          基準日
     * @param confirmedOnly 確定アクションのみを含めるか
     * @return 累積株式数倍率
     */
    public BigDecimal sharesFactorAt(final String companyCode, final LocalDate date, final boolean confirmedOnly) {
        BigDecimal factor = BigDecimal.ONE;
        for (final CorporateAction action : findActions(companyCode)) {
            if (confirmedOnly && !action.confirmed()) {
                continue;
            }
            if (!action.effectiveDate().isAfter(date)) {
                factor = factor.multiply(action.ratio(), MATH_CONTEXT);
            }
        }
        return factor.stripTrailingZeros();
    }

    /**
     * 生株価を基準日の株式数ベースへ補正する。
     *
     * @param priceRaw    生株価
     * @param companyCode 会社コード
     * @param priceDate   株価日
     * @param basisDate   基準日
     * @return 補正後株価
     */
    /**
     * 事前取得した actions を使って株価を調整する。
     * findActions() を外部で1回だけ呼び、DB呼び出しの N+1 を防ぐ。
     *
     * @param priceRaw      元の株価
     * @param actions       事前取得したコーポレートアクションリスト
     * @param priceDate     株価の日付
     * @param basisDate     基準日
     * @param confirmedOnly 確定済みアクションのみを使うか
     * @return 調整後株価
     */
    public BigDecimal adjustToBasisWithActions(
            final BigDecimal priceRaw,
            final List<CorporateAction> actions,
            final LocalDate priceDate,
            final LocalDate basisDate,
            final boolean confirmedOnly) {
        final BigDecimal numerator = priceRaw.multiply(
                computeSharesFactor(actions, priceDate, confirmedOnly), MATH_CONTEXT);
        return numerator.divide(
                computeSharesFactor(actions, basisDate, confirmedOnly), MATH_CONTEXT).stripTrailingZeros();
    }

    private BigDecimal computeSharesFactor(
            final List<CorporateAction> actions,
            final LocalDate date,
            final boolean confirmedOnly) {
        BigDecimal factor = BigDecimal.ONE;
        for (final CorporateAction action : actions) {
            if (confirmedOnly && !action.confirmed()) {
                continue;
            }
            if (!action.effectiveDate().isAfter(date)) {
                factor = factor.multiply(action.ratio(), MATH_CONTEXT);
            }
        }
        return factor.stripTrailingZeros();
    }

    public BigDecimal adjustToBasis(
            final BigDecimal priceRaw,
            final String companyCode,
            final LocalDate priceDate,
            final LocalDate basisDate) {
        return adjustToBasis(priceRaw, companyCode, priceDate, basisDate, false);
    }

    /**
     * 生株価を基準日の株式数ベースへ補正する。
     *
     * @param priceRaw      生株価
     * @param companyCode   会社コード
     * @param priceDate     株価日
     * @param basisDate     基準日
     * @param confirmedOnly 確定アクションのみを含めるか
     * @return 補正後株価
     */
    public BigDecimal adjustToBasis(
            final BigDecimal priceRaw,
            final String companyCode,
            final LocalDate priceDate,
            final LocalDate basisDate,
            final boolean confirmedOnly) {
        final BigDecimal numerator = priceRaw.multiply(sharesFactorAt(companyCode, priceDate, confirmedOnly), MATH_CONTEXT);
        return numerator.divide(sharesFactorAt(companyCode, basisDate, confirmedOnly), MATH_CONTEXT).stripTrailingZeros();
    }

    /**
     * 会社コードに紐づく株式アクション一覧を返す。
     *
     * @param companyCode 会社コード
     * @return 施行日順の株式アクション一覧
     */
    public List<CorporateAction> findActions(final String companyCode) {
        return findActionsInternal(
                companySpecification.findCompanyByCode(companyCode),
                stockPriceDao.selectByCode(companyCode)
        );
    }

    /**
     * 企業情報に紐づく株式アクション一覧を返す。
     *
     * @param company 企業情報
     * @param stockPrices 取得済み株価一覧
     * @return 施行日順の株式アクション一覧
     */
    public List<CorporateAction> findActions(final Company company, final List<StockPriceEntity> stockPrices) {
        return findActionsInternal(Optional.of(company), stockPrices);
    }

    private List<CorporateAction> findActionsInternal(
            final Optional<Company> company,
            final List<StockPriceEntity> stockPrices) {
        final List<ActionCandidate> shareCandidates = extractShareCandidates(company);
        final List<ActionCandidate> cliffCandidates = extractCliffCandidates(stockPrices);
        final Map<LocalDate, CorporateAction> actions = new LinkedHashMap<>();

        for (final ActionCandidate cliffCandidate : cliffCandidates) {
            actions.put(cliffCandidate.effectiveDate(), cliffCandidate.toCorporateAction(false));
        }
        for (final ActionCandidate shareCandidate : shareCandidates) {
            final Optional<ActionCandidate> matched = findMatchingCliff(shareCandidate, cliffCandidates);
            matched.ifPresent(candidate -> actions.put(candidate.effectiveDate(), candidate.toCorporateAction(true)));
        }
        return actions.values().stream()
                .sorted(Comparator.comparing(CorporateAction::effectiveDate))
                .toList();
    }

    private Optional<ActionCandidate> findMatchingCliff(
            final ActionCandidate shareCandidate,
            final List<ActionCandidate> cliffCandidates) {
        return cliffCandidates.stream()
                .filter(cliffCandidate -> isWithinTolerance(cliffCandidate.ratio(), shareCandidate.ratio()))
                .min(Comparator.comparing(cliffCandidate ->
                        Math.abs(cliffCandidate.effectiveDate().toEpochDay() - shareCandidate.referenceDate().toEpochDay())));
    }

    private List<ActionCandidate> extractShareCandidates(final Optional<Company> company) {
        final List<FinancialStatementEntity> orderedStatements = latestShareStatements(company);
        final List<ActionCandidate> candidates = new ArrayList<>();

        for (int index = 1; index < orderedStatements.size(); index++) {
            final FinancialStatementEntity previous = orderedStatements.get(index - 1);
            final FinancialStatementEntity current = orderedStatements.get(index);
            final Optional<BigDecimal> ratio = inferShareRatio(previous.getValue().orElse(null), current.getValue().orElse(null));
            ratio.ifPresent(value -> candidates.add(new ActionCandidate(current.getSubmitDate(), current.getPeriodEnd(), value)));
        }
        return candidates;
    }

    private List<FinancialStatementEntity> latestShareStatements(final Optional<Company> company) {
        if (company.isEmpty()) {
            return List.of();
        }

        final Map<LocalDate, FinancialStatementEntity> latestByPeriod = new LinkedHashMap<>();
        final List<FinancialStatementEntity> statements = financialStatementSpecification.findByCompany(company.get());

        for (final FinancialStatementEntity statement : statements) {
            if (!FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.getId().equals(statement.getFinancialStatementId())) {
                continue;
            }
            if (statement.getValue().isEmpty() || statement.getValue().get() <= 0L) {
                continue;
            }
            final FinancialStatementEntity current = latestByPeriod.get(statement.getPeriodEnd());
            if (current == null || current.getSubmitDate().isBefore(statement.getSubmitDate())) {
                latestByPeriod.put(statement.getPeriodEnd(), statement);
            }
        }
        return latestByPeriod.values().stream()
                .sorted(Comparator.comparing(FinancialStatementEntity::getPeriodEnd))
                .toList();
    }

    private List<ActionCandidate> extractCliffCandidates(final List<StockPriceEntity> stockPrices) {
        final List<StockPriceEntity> prices = stockPrices.stream()
                .filter(price -> Objects.nonNull(price.getStockPrice()) && price.getStockPrice() > 0.0d)
                .sorted(Comparator.comparing(StockPriceEntity::getTargetDate))
                .toList();
        final List<ActionCandidate> candidates = new ArrayList<>();

        for (int index = 1; index < prices.size(); index++) {
            final StockPriceEntity previous = prices.get(index - 1);
            final StockPriceEntity current = prices.get(index);
            final Optional<BigDecimal> ratio = inferCliffRatio(previous.getStockPrice(), current.getStockPrice());
            ratio.ifPresent(value -> candidates.add(new ActionCandidate(current.getTargetDate(), current.getTargetDate(), value)));
        }
        return candidates;
    }

    private Optional<BigDecimal> inferShareRatio(final Long previousValue, final Long currentValue) {
        if (previousValue == null || currentValue == null || previousValue <= 0L || currentValue <= 0L) {
            return Optional.empty();
        }
        final BigDecimal rawRatio = BigDecimal.valueOf(currentValue)
                .divide(BigDecimal.valueOf(previousValue), MATH_CONTEXT);
        return normalizeFactor(rawRatio);
    }

    private Optional<BigDecimal> inferCliffRatio(final Double previousPrice, final Double currentPrice) {
        if (previousPrice == null || currentPrice == null || previousPrice <= 0.0d || currentPrice <= 0.0d) {
            return Optional.empty();
        }
        final BigDecimal priceRatio = BigDecimal.valueOf(currentPrice)
                .divide(BigDecimal.valueOf(previousPrice), MATH_CONTEXT);
        if (priceRatio.compareTo(cliffThresholdRatio) <= 0) {
            return normalizeFactor(BigDecimal.ONE.divide(priceRatio, MATH_CONTEXT));
        }
        if (priceRatio.compareTo(BigDecimal.ONE) > 0) {
            final BigDecimal integerMultiple = toIntegerFactor(priceRatio);
            if (integerMultiple.compareTo(BigDecimal.ONE) > 0 && isWithinTolerance(priceRatio, integerMultiple)) {
                return Optional.of(BigDecimal.ONE.divide(integerMultiple, MATH_CONTEXT).stripTrailingZeros());
            }
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> normalizeFactor(final BigDecimal rawRatio) {
        if (rawRatio.compareTo(BigDecimal.ONE) >= 0) {
            final BigDecimal integerMultiple = toIntegerFactor(rawRatio);
            if (integerMultiple.compareTo(BigDecimal.ONE) > 0 && isWithinTolerance(rawRatio, integerMultiple)) {
                return Optional.of(integerMultiple.stripTrailingZeros());
            }
            return Optional.empty();
        }
        final BigDecimal inverse = BigDecimal.ONE.divide(rawRatio, MATH_CONTEXT);
        final BigDecimal integerMultiple = toIntegerFactor(inverse);
        if (integerMultiple.compareTo(BigDecimal.ONE) > 0 && isWithinTolerance(inverse, integerMultiple)) {
            return Optional.of(BigDecimal.ONE.divide(integerMultiple, MATH_CONTEXT).stripTrailingZeros());
        }
        return Optional.empty();
    }

    private BigDecimal toIntegerFactor(final BigDecimal value) {
        return BigDecimal.valueOf(Math.round(value.doubleValue()));
    }

    private boolean isWithinTolerance(final BigDecimal value, final BigDecimal expected) {
        return value.subtract(expected).abs().compareTo(ratioTolerance) <= 0;
    }

    /**
     * 株式分割・併合イベントを表す。
     *
     * @param effectiveDate 施行日
     * @param ratio         株式数倍率
     * @param confirmed     確定フラグ
     */
    public record CorporateAction(LocalDate effectiveDate, BigDecimal ratio, boolean confirmed) {
    }

    private record ActionCandidate(LocalDate effectiveDate, LocalDate referenceDate, BigDecimal ratio) {

        private CorporateAction toCorporateAction(final boolean confirmed) {
            return new CorporateAction(effectiveDate, ratio, confirmed);
        }
    }
}
