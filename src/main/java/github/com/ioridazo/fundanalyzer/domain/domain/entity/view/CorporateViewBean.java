package github.com.ioridazo.fundanalyzer.domain.domain.entity.view;

import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "corporate_view")
public class CorporateViewBean {

    // 証券コード
    @Id
    private final String code;

    // 会社名
    private final String name;

    // 提出日
    private final LocalDate submitDate;

    // 最新書類種別コード
    @Id
    private final String latestDocumentTypeCode;

    // 最新企業価値
    private final BigDecimal latestCorporateValue;

    // 3年平均企業価値
    private final BigDecimal threeAverageCorporateValue;

    // 3年標準偏差
    private final BigDecimal threeStandardDeviation;

    // 3年変動係数
    private final BigDecimal threeCoefficientOfVariation;

    // 5年平均企業価値
    private final BigDecimal fiveAverageCorporateValue;

    // 5年標準偏差
    private final BigDecimal fiveStandardDeviation;

    // 5年変動係数
    private final BigDecimal fiveCoefficientOfVariation;

    // 10年平均企業価値
    private final BigDecimal tenAverageCorporateValue;

    // 10年標準偏差
    private final BigDecimal tenStandardDeviation;

    // 10年変動係数
    private final BigDecimal tenCoefficientOfVariation;

    // 全て平均企業価値
    private final BigDecimal allAverageCorporateValue;

    // 全て標準偏差
    private final BigDecimal allStandardDeviation;

    // 全て変動係数
    private final BigDecimal allCoefficientOfVariation;

    // 提出日株価平均
    private final BigDecimal averageStockPrice;

    // 株価取得日
    private final LocalDate importDate;

    // 最新株価
    private final BigDecimal latestStockPrice;

    // 3年割安値
    private final BigDecimal threeDiscountValue;

    // 3年割安度
    private final BigDecimal threeDiscountRate;

    // 5年割安値
    private final BigDecimal fiveDiscountValue;

    // 5年割安度
    private final BigDecimal fiveDiscountRate;

    // 10年割安値
    private final BigDecimal tenDiscountValue;

    // 10年割安度
    private final BigDecimal tenDiscountRate;

    // 全割安値
    private final BigDecimal allDiscountValue;

    // 全割安度
    private final BigDecimal allDiscountRate;

    // 対象年カウント
    private final BigDecimal countYear;

    // みんかぶ株価予想
    private final BigDecimal forecastStock;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;

    public static CorporateViewBean of(final CorporateViewModel viewModel, final LocalDateTime nowLocalDateTime) {
        return new CorporateViewBean(
                viewModel.getCode(),
                viewModel.getName(),
                viewModel.getSubmitDate(),
                viewModel.getLatestDocumentTypeCode(),
                viewModel.getLatestCorporateValue(),
                viewModel.getThreeAverageCorporateValue(),
                viewModel.getThreeStandardDeviation(),
                viewModel.getThreeCoefficientOfVariation(),
                viewModel.getFiveAverageCorporateValue(),
                viewModel.getFiveStandardDeviation(),
                viewModel.getFiveCoefficientOfVariation(),
                viewModel.getTenAverageCorporateValue(),
                viewModel.getTenStandardDeviation(),
                viewModel.getTenCoefficientOfVariation(),
                viewModel.getAllAverageCorporateValue(),
                viewModel.getAllStandardDeviation(),
                viewModel.getAllCoefficientOfVariation(),
                viewModel.getAverageStockPrice(),
                viewModel.getImportDate(),
                viewModel.getLatestStockPrice(),
                viewModel.getThreeDiscountValue(),
                viewModel.getThreeDiscountRate(),
                viewModel.getFiveDiscountValue(),
                viewModel.getFiveDiscountRate(),
                viewModel.getTenDiscountValue(),
                viewModel.getTenDiscountRate(),
                viewModel.getAllDiscountValue(),
                viewModel.getAllDiscountRate(),
                viewModel.getCountYear(),
                viewModel.getForecastStock(),
                nowLocalDateTime,
                nowLocalDateTime
        );
    }

    public Optional<LocalDate> getSubmitDate() {
        return Optional.ofNullable(submitDate);
    }

    public Optional<BigDecimal> getLatestCorporateValue() {
        return Optional.ofNullable(latestCorporateValue);
    }

    public Optional<BigDecimal> getThreeAverageCorporateValue() {
        return Optional.ofNullable(threeAverageCorporateValue);
    }

    public Optional<BigDecimal> getThreeStandardDeviation() {
        return Optional.ofNullable(threeStandardDeviation);
    }

    public Optional<BigDecimal> getThreeCoefficientOfVariation() {
        return Optional.ofNullable(threeCoefficientOfVariation);
    }

    public Optional<BigDecimal> getFiveAverageCorporateValue() {
        return Optional.ofNullable(fiveAverageCorporateValue);
    }

    public Optional<BigDecimal> getFiveStandardDeviation() {
        return Optional.ofNullable(fiveStandardDeviation);
    }

    public Optional<BigDecimal> getFiveCoefficientOfVariation() {
        return Optional.ofNullable(fiveCoefficientOfVariation);
    }

    public Optional<BigDecimal> getTenAverageCorporateValue() {
        return Optional.ofNullable(tenAverageCorporateValue);
    }

    public Optional<BigDecimal> getTenStandardDeviation() {
        return Optional.ofNullable(tenStandardDeviation);
    }

    public Optional<BigDecimal> getTenCoefficientOfVariation() {
        return Optional.ofNullable(tenCoefficientOfVariation);
    }

    public Optional<BigDecimal> getAllAverageCorporateValue() {
        return Optional.ofNullable(allAverageCorporateValue);
    }

    public Optional<BigDecimal> getAllStandardDeviation() {
        return Optional.ofNullable(allStandardDeviation);
    }

    public Optional<BigDecimal> getAllCoefficientOfVariation() {
        return Optional.ofNullable(allCoefficientOfVariation);
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

    public Optional<BigDecimal> getThreeDiscountValue() {
        return Optional.ofNullable(threeDiscountValue);
    }

    public Optional<BigDecimal> getThreeDiscountRate() {
        return Optional.ofNullable(threeDiscountRate);
    }

    public Optional<BigDecimal> getFiveDiscountValue() {
        return Optional.ofNullable(fiveDiscountValue);
    }

    public Optional<BigDecimal> getFiveDiscountRate() {
        return Optional.ofNullable(fiveDiscountRate);
    }

    public Optional<BigDecimal> getTenDiscountValue() {
        return Optional.ofNullable(tenDiscountValue);
    }

    public Optional<BigDecimal> getTenDiscountRate() {
        return Optional.ofNullable(tenDiscountRate);
    }

    public Optional<BigDecimal> getAllDiscountValue() {
        return Optional.ofNullable(allDiscountValue);
    }

    public Optional<BigDecimal> getAllDiscountRate() {
        return Optional.ofNullable(allDiscountRate);
    }

    public Optional<BigDecimal> getCountYear() {
        return Optional.ofNullable(countYear);
    }

    public Optional<BigDecimal> getForecastStock() {
        return Optional.ofNullable(forecastStock);
    }
}
