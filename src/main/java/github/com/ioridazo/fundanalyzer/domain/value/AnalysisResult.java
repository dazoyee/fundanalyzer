package github.com.ioridazo.fundanalyzer.domain.value;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.QuarterType;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Getter
public class AnalysisResult {

    private final BigDecimal corporateValue;

    private final BigDecimal bps;

    private final BigDecimal eps;

    private final BigDecimal roe;

    private final BigDecimal roa;

    private final LocalDate submitDate;

    private final String documentId;

    private static final BigDecimal WEIGHTING_BUSINESS_VALUE = BigDecimal.TEN;
    private static final BigDecimal AVERAGE_CURRENT_RATIO = BigDecimal.valueOf(1.2);
    private static final BigDecimal WEIGHTING_QUARTER_VALUE = BigDecimal.valueOf(4);
    private static final int TENTH_DECIMAL_PLACE = 10;

    public AnalysisResult(
            final BigDecimal corporateValue,
            final BigDecimal bps,
            final BigDecimal eps,
            final BigDecimal roe,
            final BigDecimal roa,
            final LocalDate submitDate,
            final String documentId) {
        this.corporateValue = corporateValue;
        this.bps = bps;
        this.eps = eps;
        this.roe = roe;
        this.roa = roa;
        this.submitDate = submitDate;
        this.documentId = documentId;
    }

    public AnalysisResult(final FinanceValue financeValue, final Document document) {
        this.corporateValue = calculateCorporateValue(financeValue, document);
        this.bps = calculateBps(financeValue, document).orElse(null);
        this.eps = calculateEps(financeValue, document).orElse(null);
        this.roe = calculateRoe(financeValue, document).orElse(null);
        this.roa = calculateRoa(financeValue, document).orElse(null);
        this.submitDate = document.getSubmitDate();
        this.documentId = document.getDocumentId();
    }

    public static AnalysisResult of(final AnalysisResultEntity entity) {
        return new AnalysisResult(
                entity.getCorporateValue(),
                entity.getBps().orElse(null),
                entity.getEps().orElse(null),
                entity.getRoe().orElse(null),
                entity.getRoa().orElse(null),
                entity.getSubmitDate(),
                entity.getDocumentId()
        );
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
     * 企業価値の算出する
     *
     * @param financeValue 財務諸表値
     * @param document     ドキュメント
     * @return 企業価値
     * @throws FundanalyzerNotExistException 値が存在しないとき
     */
    BigDecimal calculateCorporateValue(
            final FinanceValue financeValue, final Document document) throws FundanalyzerNotExistException {
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
        // 四半期種別の重みづけ
        final BigDecimal weightingQuarterType = Optional.of(document)
                .map(Document::getQuarterType)
                .map(QuarterType::getWeight)
                .map(BigDecimal::new)
                .orElse(WEIGHTING_QUARTER_VALUE);
        // 株式総数
        final BigDecimal numberOfShares = financeValue.getNumberOfShares().map(BigDecimal::new)
                .orElseThrow(() -> new FundanalyzerNotExistException(
                        FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES,
                        "株式総数",
                        document
                ));

        return operatingProfit.multiply(WEIGHTING_BUSINESS_VALUE)
                .add(totalCurrentAssets).subtract(totalCurrentLiabilities.multiply(AVERAGE_CURRENT_RATIO)).add(totalInvestmentsAndOtherAssets)
                .subtract(totalFixedLiabilities)
                .divide(weightingQuarterType, TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP)
                .multiply(WEIGHTING_QUARTER_VALUE)
                .divide(numberOfShares, TENTH_DECIMAL_PLACE, RoundingMode.HALF_UP);
    }

    Optional<BigDecimal> calculateBps(final FinanceValue financeValue, final Document document) {
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

    Optional<BigDecimal> calculateEps(final FinanceValue financeValue, final Document document) {
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

    Optional<BigDecimal> calculateRoe(final FinanceValue financeValue, final Document document) {
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

    Optional<BigDecimal> calculateRoa(final FinanceValue financeValue, final Document document) {
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
