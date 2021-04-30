package github.com.ioridazo.fundanalyzer.domain.logic.view;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.MinkabuDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.master.CompanyEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.DocumentEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.MinkabuEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.logic.view.bean.CorporateViewBean;
import github.com.ioridazo.fundanalyzer.domain.util.Target;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CorporateViewLogic {

    private static final int SECOND_DECIMAL_PLACE = 2;
    private static final int THIRD_DECIMAL_PLACE = 3;

    private final DocumentDao documentDao;
    private final AnalysisResultDao analysisResultDao;
    private final StockPriceDao stockPriceDao;
    private final MinkabuDao minkabuDao;

    public CorporateViewLogic(
            final DocumentDao documentDao,
            final AnalysisResultDao analysisResultDao,
            final StockPriceDao stockPriceDao,
            final MinkabuDao minkabuDao) {
        this.documentDao = documentDao;
        this.analysisResultDao = analysisResultDao;
        this.stockPriceDao = stockPriceDao;
        this.minkabuDao = minkabuDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 企業価値等を画面表示するためのBean生成
     *
     * @param companyEntity     会社
     * @param targetTypes 書類種別コード
     * @return CorporateViewBean
     */
    @NewSpan("CorporateViewLogic.corporateViewOf")
    public CorporateViewBean corporateViewOf(final CompanyEntity companyEntity, final List<DocumentTypeCode> targetTypes) {
        final var submitDate = latestSubmitDate(companyEntity, targetTypes);
        final var corporateValue = corporateValue(companyEntity);
        final var stockPrice = submitDate.map(sd -> stockPrice(companyEntity, sd)).orElse(StockPriceValue.of());
        final var discountValue = discountValue(
                corporateValue.getAverageCorporateValue().orElse(null),
                stockPrice.getLatestStockPrice().orElse(null));

        return new CorporateViewBean(
                companyEntity.getCode().orElseThrow().substring(0, 4),
                companyEntity.getCompanyName(),
                submitDate.orElse(null),
                corporateValue.getLatestCorporateValue().orElse(null),
                corporateValue.getAverageCorporateValue().orElse(null),
                corporateValue.getStandardDeviation().orElse(null),
                corporateValue.getCoefficientOfVariation().orElse(null),
                stockPrice.getAverageStockPrice().orElse(null),
                stockPrice.getImportDate().orElse(null),
                stockPrice.getLatestStockPrice().orElse(null),
                discountValue.getFirst().orElse(null),
                discountValue.getSecond().orElse(null),
                corporateValue.getCountYear().orElse(null),
                forecastStock(companyEntity).orElse(null),
                nowLocalDateTime(),
                nowLocalDateTime()
        );
    }

    /**
     * 直近の財務諸表提出日を取得する
     *
     * @param companyEntity     会社情報
     * @param targetTypes 書類種別コード
     * @return 提出日
     */
    Optional<LocalDate> latestSubmitDate(final CompanyEntity companyEntity, final List<DocumentTypeCode> targetTypes) {
        final List<String> docTypeCode = targetTypes.stream().map(DocumentTypeCode::toValue).collect(Collectors.toList());
        return documentDao.selectByDocumentTypeCode(docTypeCode).stream()
                .filter(d -> companyEntity.getEdinetCode().equals(d.getEdinetCode()))
                .max(Comparator.comparing(DocumentEntity::getSubmitDate))
                .map(DocumentEntity::getSubmitDate);
    }

    /**
     * 企業価値を算出する
     *
     * @param companyEntity 会社情報
     * @return <li>平均の企業価値</li><li>標準偏差</li><li>対象年数</li>
     */
    CorporateValue corporateValue(final CompanyEntity companyEntity) {
        final List<AnalysisResultEntity> corporateValueList = Target.distinctAnalysisResults(
                analysisResultDao.selectByCompanyCode(companyEntity.getCode().orElseThrow()));

        if (!corporateValueList.isEmpty()) {
            // 最新企業価値
            final var latest = corporateValueList.stream()
                    // latest
                    .max(Comparator.comparing(AnalysisResultEntity::getDocumentPeriod))
                    // corporate value
                    .map(AnalysisResultEntity::getCorporateValue)
                    // scale
                    .map(bigDecimal -> bigDecimal.setScale(SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP))
                    .orElse(null);
            // 平均企業価値
            final var average = corporateValueList.stream()
                    .map(AnalysisResultEntity::getCorporateValue)
                    // sum
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    // average
                    .divide(BigDecimal.valueOf(corporateValueList.size()), SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP);
            // 標準偏差
            final var standardDeviation = corporateValueList.stream()
                    .map(AnalysisResultEntity::getCorporateValue)
                    // (value - average) ^2
                    .map(value -> value.subtract(average).pow(2))
                    // sum
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    // average
                    .divide(BigDecimal.valueOf(corporateValueList.size()), THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)
                    // sqrt
                    .sqrt(new MathContext(5, RoundingMode.HALF_UP));
            // 変動係数
            final var coefficientOfVariation = standardDeviation.divide(average, THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP);
            return CorporateValue.of(latest, average, standardDeviation, coefficientOfVariation, BigDecimal.valueOf(corporateValueList.size()));
        } else {
            return CorporateValue.of();
        }
    }

    /**
     * 株価を取得する
     *
     * @param companyEntity    会社情報
     * @param submitDate 提出日
     * @return <li>提出日の株価</li><li>最近株価日付</li><li>最新株価</li>
     */
    StockPriceValue stockPrice(final CompanyEntity companyEntity, final LocalDate submitDate) {
        final var stockPriceList = stockPriceDao.selectByCode(companyEntity.getCode().orElseThrow());
        // importDate
        final var importDate = stockPriceList.stream()
                .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                .map(StockPriceEntity::getTargetDate).orElse(null);
        // latestStockPrice
        final var latestStockPrice = stockPriceList.stream()
                .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                .map(StockPriceEntity::getStockPrice)
                .map(BigDecimal::valueOf).orElse(null);

        // stock price for one month
        final var monthList = stockPriceList.stream()
                .filter(sp -> submitDate.minusMonths(1).isBefore(sp.getTargetDate()) && submitDate.plusDays(1).isAfter(sp.getTargetDate()))
                .map(StockPriceEntity::getStockPrice)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (monthList.isEmpty()) {
            return StockPriceValue.of(null, importDate, latestStockPrice);
        } else {
            // averageStockPrice
            final var averageStockPrice = monthList.stream()
                    .map(BigDecimal::valueOf)
                    // sum
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    // average
                    .divide(BigDecimal.valueOf(monthList.size()), SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP);

            return StockPriceValue.of(averageStockPrice, importDate, latestStockPrice);
        }
    }

    /**
     * 割安値
     *
     * @param corporateValue   企業価値
     * @param latestStockPrice 最新株価
     * @return <li>割安値</li><li>割安度</li>
     */
    Pair<Optional<BigDecimal>, Optional<BigDecimal>> discountValue(
            final BigDecimal corporateValue, final BigDecimal latestStockPrice) {
        try {
            final var cv = Objects.requireNonNull(corporateValue);
            final var sp = Objects.requireNonNull(latestStockPrice);
            return Pair.of(
                    Optional.of(cv.subtract(sp).abs(new MathContext(6))),
                    Optional.of(cv.divide(sp, 5, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(3, RoundingMode.HALF_UP))
            );
        } catch (NullPointerException e) {
            return Pair.of(Optional.empty(), Optional.empty());
        }
    }

    /**
     * 最新のみんかぶ株価予想を取得する
     *
     * @param companyEntity 会社情報
     * @return 最新の株価予想
     */
    Optional<BigDecimal> forecastStock(final CompanyEntity companyEntity) {
        return minkabuDao.selectByCode(companyEntity.getCode().orElseThrow()).stream()
                .max(Comparator.comparing(MinkabuEntity::getTargetDate))
                .map(MinkabuEntity::getGoalsStock)
                .map(BigDecimal::new);
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class CorporateValue {
        // 最新企業価値
        private BigDecimal latestCorporateValue;
        // 平均企業価値
        private BigDecimal averageCorporateValue;
        // 標準偏差
        private BigDecimal standardDeviation;
        // 変動係数
        private BigDecimal coefficientOfVariation;
        // 対象年カウント
        private BigDecimal countYear;

        public static CorporateValue of() {
            return new CorporateValue();
        }

        public static CorporateValue of(
                final BigDecimal latestCorporateValue,
                final BigDecimal averageCorporateValue,
                final BigDecimal standardDeviation,
                final BigDecimal coefficientOfVariation,
                final BigDecimal countYear) {
            return new CorporateValue(latestCorporateValue, averageCorporateValue, standardDeviation, coefficientOfVariation, countYear);
        }

        public Optional<BigDecimal> getLatestCorporateValue() {
            return Optional.ofNullable(latestCorporateValue);
        }

        public Optional<BigDecimal> getAverageCorporateValue() {
            return Optional.ofNullable(averageCorporateValue);
        }

        public Optional<BigDecimal> getStandardDeviation() {
            return Optional.ofNullable(standardDeviation);
        }

        public Optional<BigDecimal> getCoefficientOfVariation() {
            return Optional.ofNullable(coefficientOfVariation);
        }

        public Optional<BigDecimal> getCountYear() {
            return Optional.ofNullable(countYear);
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class StockPriceValue {
        // 提出日株価平均
        private BigDecimal averageStockPrice;
        // 株価取得日
        private LocalDate importDate;
        // 最新株価
        private BigDecimal latestStockPrice;

        public static StockPriceValue of() {
            return new StockPriceValue();
        }

        public static StockPriceValue of(
                final BigDecimal stockPriceOfSubmitDate,
                final LocalDate importDate,
                BigDecimal latestStockPrice) {
            return new StockPriceValue(stockPriceOfSubmitDate, importDate, latestStockPrice);
        }

        public Optional<BigDecimal> getAverageStockPrice() {
            return Optional.ofNullable(averageStockPrice);
        }

        public Optional<LocalDate> getImportDate() {
            return Optional.ofNullable(importDate);
        }

        public Optional<BigDecimal> getLatestStockPrice() {
            return Optional.ofNullable(latestStockPrice);
        }
    }
}
