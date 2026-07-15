package github.com.ioridazo.fundanalyzer.domain.value;

import github.com.ioridazo.fundanalyzer.config.AnalysisCoefficient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Supplier;

@Getter
public class AnalysisResult {

    private final BigDecimal corporateValue;

    private final BigDecimal rimValue;

    private final BigDecimal bps;

    private final BigDecimal eps;

    private final BigDecimal roe;

    private final BigDecimal roa;

    private final LocalDate submitDate;

    private final String documentId;

    private static final int TENTH_DECIMAL_PLACE = 10;
    /** 年換算重み（1年=4四半期の不変定数。四半期分母の既定値と年換算倍率を兼ねる）。 */
    private static final BigDecimal ANNUAL_WEIGHT = BigDecimal.valueOf(4);

    public AnalysisResult(
            final BigDecimal corporateValue,
            final BigDecimal bps,
            final BigDecimal eps,
            final BigDecimal roe,
            final BigDecimal roa,
            final LocalDate submitDate,
            final String documentId) {
        this(corporateValue, null, bps, eps, roe, roa, submitDate, documentId);
    }

    public AnalysisResult(
            final BigDecimal corporateValue,
            final BigDecimal rimValue,
            final BigDecimal bps,
            final BigDecimal eps,
            final BigDecimal roe,
            final BigDecimal roa,
            final LocalDate submitDate,
            final String documentId) {
        this.corporateValue = corporateValue;
        this.rimValue = rimValue;
        this.bps = bps;
        this.eps = eps;
        this.roe = roe;
        this.roa = roa;
        this.submitDate = submitDate;
        this.documentId = documentId;
    }

    public AnalysisResult(final FinanceValue financeValue, final Document document, final AnalysisCoefficient coefficient) {
        this.corporateValue = calculateCorporateValue(financeValue, document, coefficient);
        this.bps = calculateBps(financeValue, document).orElse(null);
        this.eps = calculateEps(financeValue, document).orElse(null);
        this.roe = calculateRoe(financeValue, document).orElse(null);
        this.roa = calculateRoa(financeValue, document).orElse(null);
        this.rimValue = calculateRimValue(this.bps, this.roe, coefficient.getCostOfEquity()).orElse(null);
        this.submitDate = document.getSubmitDate();
        this.documentId = document.getDocumentId();
    }

    public static AnalysisResult of() {
        return new AnalysisResult(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * 永続化された企業価値・理論株価（係数依存＝算出時点の係数を凍結した値）と、
     * 財務諸表から都度計算した係数非依存指標（BPS/EPS/ROE/ROA）を組み合わせて再構築する。
     *
     * <p>指標の入力科目が欠損している場合は例外とせず該当指標を空にする（永続列の NULL と同じ扱い）。
     *
     * @param entity       企業価値エンティティ（corporate_value / rim_value の凍結値を保持）
     * @param financeValue 財務諸表値
     * @param document     ドキュメント
     * @return 企業価値
     */
    public static AnalysisResult of(
            final AnalysisResultEntity entity, final FinanceValue financeValue, final Document document) {
        return new AnalysisResult(
                entity.getCorporateValue(),
                entity.getRimValue().orElse(null),
                computeIndicatorQuietly(() -> calculateBps(financeValue, document)),
                computeIndicatorQuietly(() -> calculateEps(financeValue, document)),
                computeIndicatorQuietly(() -> calculateRoe(financeValue, document)),
                computeIndicatorQuietly(() -> calculateRoa(financeValue, document)),
                entity.getSubmitDate(),
                entity.getDocumentId()
        );
    }

    private static BigDecimal computeIndicatorQuietly(final Supplier<Optional<BigDecimal>> calculation) {
        try {
            return calculation.get().orElse(null);
        } catch (final FundanalyzerNotExistException | ArithmeticException e) {
            // 入力科目の欠損（NotExist）と不正値によるゼロ除算（Arithmetic）は指標なし扱いとする
            return null;
        }
    }

    public Optional<BigDecimal> getRimValue() {
        return Optional.ofNullable(rimValue);
    }

    /**
     * 残余利益モデル(無成長)の理論株価を算出する。
     *
     * <p>{@code BPS × (ROE/100) ÷ r}。BPS/ROE/r が無い・r が 0・ROE が 0 以下（赤字等）のときは算出しない。
     *
     * @param bps          1株当たり純資産
     * @param roe          自己資本利益率（百分率）
     * @param costOfEquity 資本コスト（割引率）
     * @return RIM 理論株価（算出不能時は空）
     */
    Optional<BigDecimal> calculateRimValue(final BigDecimal bps, final BigDecimal roe, final BigDecimal costOfEquity) {
        if (bps == null || roe == null || costOfEquity == null
            || costOfEquity.signum() <= 0 || roe.signum() <= 0) {
            return Optional.empty();
        }
        return Optional.of(bps
                .multiply(roe.divide(BigDecimal.valueOf(100), TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP))
                .divide(costOfEquity, TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP));
    }

    public Optional<BigDecimal> getBps() {
        return Optional.ofNullable(bps);
    }

    public Optional<BigDecimal> getEps() {
        return Optional.ofNullable(eps);
    }

    public Optional<BigDecimal> getRoe() {
        return Optional.ofNullable(roe);
    }

    public Optional<BigDecimal> getRoa() {
        return Optional.ofNullable(roa);
    }

    /**
     * 企業価値を指定係数で算出する。
     *
     * <p>年換算は営業利益にのみ適用し、BS のストック項目（流動資産・流動負債・投資その他の資産・固定負債）は
     * 四半期末時点の残高を等倍で加減算する。
     *
     * <p>書類種別 140/150（四半期報告書・訂正四半期報告書）は保存されても集計・表示対象外であり、
     * 既存保存データの再計算は行わない。
     *
     * @param financeValue 財務諸表値
     * @param document     ドキュメント
     * @param coefficient  算出係数
     * @return 企業価値
     * @throws FundanalyzerNotExistException 値が存在しないとき
     */
    BigDecimal calculateCorporateValue(
            final FinanceValue financeValue, final Document document, final AnalysisCoefficient coefficient)
            throws FundanalyzerNotExistException {
        // 流動資産合計
        final BigDecimal totalCurrentAssets = financeValue.getTotalCurrentAssets().map(BigDecimal::new)
                .orElseThrow(() -> new FundanalyzerNotExistException(
                        FinancialStatementEnum.BALANCE_SHEET,
                        BsSubject.BsEnum.TOTAL_CURRENT_ASSETS.getSubject(),
                        document
                ));
        // 投資その他の資産合計
        final BigDecimal totalInvestmentsAndOtherAssets = financeValue.getTotalInvestmentsAndOtherAssets().map(BigDecimal::new)
                .orElseThrow(() -> new FundanalyzerNotExistException(
                        FinancialStatementEnum.BALANCE_SHEET,
                        BsSubject.BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS.getSubject(),
                        document
                ));
        // 流動負債合計
        final BigDecimal totalCurrentLiabilities = financeValue.getTotalCurrentLiabilities().map(BigDecimal::new)
                .orElseThrow(() -> new FundanalyzerNotExistException(
                        FinancialStatementEnum.BALANCE_SHEET,
                        BsSubject.BsEnum.TOTAL_CURRENT_LIABILITIES.getSubject(),
                        document
                ));
        // 固定負債合計
        final BigDecimal totalFixedLiabilities = financeValue.getTotalFixedLiabilities().map(BigDecimal::new)
                .orElseThrow(() -> new FundanalyzerNotExistException(
                        FinancialStatementEnum.BALANCE_SHEET,
                        BsSubject.BsEnum.TOTAL_FIXED_LIABILITIES.getSubject(),
                        document
                ));
        // 営業利益
        final BigDecimal operatingProfit = financeValue.getOperatingProfit().map(BigDecimal::new)
                .orElseThrow(() -> new FundanalyzerNotExistException(
                        FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                        PlSubject.PlEnum.OPERATING_PROFIT.getSubject(),
                        document
                ));
        // 四半期種別の重みづけ（QuarterType 未設定時は年次想定の ANNUAL_WEIGHT をフォールバックに使う）
        final BigDecimal weightingQuarterType = Optional.of(document)
                .map(Document::getQuarterType)
                .map(QuarterType::getWeight)
                .map(BigDecimal::new)
                .orElse(ANNUAL_WEIGHT);
        // 株式総数
        final BigDecimal numberOfShares = financeValue.getNumberOfShares().map(BigDecimal::new)
                .orElseThrow(() -> new FundanalyzerNotExistException(
                        FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES,
                        "株式総数",
                        document
                ));
        final BigDecimal annualizedOperatingProfit = operatingProfit.multiply(coefficient.getOperatingProfitWeight())
                .divide(weightingQuarterType, TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP)
                .multiply(ANNUAL_WEIGHT);

        return annualizedOperatingProfit
                .add(totalCurrentAssets).subtract(totalCurrentLiabilities.multiply(coefficient.getCurrentLiabilitiesRatio())).add(totalInvestmentsAndOtherAssets)
                .subtract(totalFixedLiabilities)
                .divide(numberOfShares, TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP);
    }

    static Optional<BigDecimal> calculateBps(final FinanceValue financeValue, final Document document) {
        // 純資産
        final Optional<BigDecimal> totalNetAssets = financeValue.getNetAssets().map(BigDecimal::new);
        // 株式総数
        final Optional<BigDecimal> numberOfShares = financeValue.getNumberOfShares().map(BigDecimal::new);

        if (Optional.of(document).map(Document::getQuarterType).map(QuarterType::getWeight).isPresent()) {
            if (totalNetAssets.isPresent() && numberOfShares.isPresent()) {
                return Optional.of(totalNetAssets.get().divide(numberOfShares.get(), TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP));
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.of(totalNetAssets.orElseThrow(() -> new FundanalyzerNotExistException(
                    FinancialStatementEnum.BALANCE_SHEET,
                    BsSubject.BsEnum.TOTAL_NET_ASSETS.getSubject(),
                    document
            )).divide(numberOfShares.orElseThrow(() -> new FundanalyzerNotExistException(
                    FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES,
                    "株式総数",
                    document
            )), TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP));
        }
    }

    static Optional<BigDecimal> calculateEps(final FinanceValue financeValue, final Document document) {
        if (Optional.of(document).map(Document::getQuarterType).map(QuarterType::getWeight).isPresent()) {
            return Optional.empty();
        } else {
            // 当期純利益
            final BigDecimal netIncome = financeValue.getNetIncome().map(BigDecimal::new)
                    .orElseThrow(() -> new FundanalyzerNotExistException(
                            FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                            PlSubject.PlEnum.NET_INCOME.getSubject(),
                            document
                    ));
            // 株式総数
            final BigDecimal numberOfShares = financeValue.getNumberOfShares().map(BigDecimal::new)
                    .orElseThrow(() -> new FundanalyzerNotExistException(
                            FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES,
                            "株式総数",
                            document
                    ));

            return Optional.of(netIncome.divide(numberOfShares, TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP));
        }
    }

    static Optional<BigDecimal> calculateRoe(final FinanceValue financeValue, final Document document) {
        if (Optional.of(document).map(Document::getQuarterType).map(QuarterType::getWeight).isPresent()) {
            return Optional.empty();
        } else {
            // 当期純利益
            final BigDecimal netIncome = financeValue.getNetIncome().map(BigDecimal::new)
                    .orElseThrow(() -> new FundanalyzerNotExistException(
                            FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                            PlSubject.PlEnum.NET_INCOME.getSubject(),
                            document
                    ));
            // 純資産
            final BigDecimal totalNetAssets = financeValue.getNetAssets().map(BigDecimal::new)
                    .orElseThrow(() -> new FundanalyzerNotExistException(
                            FinancialStatementEnum.BALANCE_SHEET,
                            BsSubject.BsEnum.TOTAL_NET_ASSETS.getSubject(),
                            document
                    ));
            // 新株予約権
            final BigDecimal subscriptionWarrant = financeValue.getSubscriptionWarrant().map(BigDecimal::new).orElse(BigDecimal.ZERO);

            // TODO 被支配株主持分（連結財務諸表のみ）

            return Optional.of(netIncome
                    .divide(totalNetAssets.subtract(subscriptionWarrant), TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }
    }

    static Optional<BigDecimal> calculateRoa(final FinanceValue financeValue, final Document document) {
        if (Optional.of(document).map(Document::getQuarterType).map(QuarterType::getWeight).isPresent()) {
            return Optional.empty();
        } else {
            // 当期純利益
            final BigDecimal netIncome = financeValue.getNetIncome().map(BigDecimal::new)
                    .orElseThrow(() -> new FundanalyzerNotExistException(
                            FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                            PlSubject.PlEnum.NET_INCOME.getSubject(),
                            document
                    ));
            // 総資産
            final Optional<BigDecimal> totalAssets = financeValue.getTotalAssets().map(BigDecimal::new);

            return totalAssets
                    .map(ta -> netIncome
                            .divide(ta, TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                    );
        }
    }
}
