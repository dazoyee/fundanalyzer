package github.com.ioridazo.fundanalyzer.domain.logic.view.bean;

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

    // 最新企業価値
    private final BigDecimal latestCorporateValue;

    // 平均企業価値
    private final BigDecimal averageCorporateValue;

    // 標準偏差
    private final BigDecimal standardDeviation;

    // 変動係数
    private final BigDecimal coefficientOfVariation;

    // 提出日株価平均
    private final BigDecimal averageStockPrice;

    // 株価取得日
    private final LocalDate importDate;

    // 最新株価
    private final BigDecimal latestStockPrice;

    // 割安値
    private final BigDecimal discountValue;

    // 割安度
    private final BigDecimal discountRate;

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
                viewModel.getLatestCorporateValue(),
                viewModel.getAverageCorporateValue(),
                viewModel.getStandardDeviation(),
                viewModel.getCoefficientOfVariation(),
                viewModel.getAverageStockPrice(),
                viewModel.getImportDate(),
                viewModel.getLatestStockPrice(),
                viewModel.getDiscountValue(),
                viewModel.getDiscountRate(),
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

    public Optional<BigDecimal> getAverageCorporateValue() {
        return Optional.ofNullable(averageCorporateValue);
    }

    public Optional<BigDecimal> getStandardDeviation() {
        return Optional.ofNullable(standardDeviation);
    }

    public Optional<BigDecimal> getCoefficientOfVariation() {
        return Optional.ofNullable(coefficientOfVariation);
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

    public Optional<BigDecimal> getDiscountValue() {
        return Optional.ofNullable(discountValue);
    }

    public Optional<BigDecimal> getDiscountRate() {
        return Optional.ofNullable(discountRate);
    }

    public Optional<BigDecimal> getCountYear() {
        return Optional.ofNullable(countYear);
    }

    public Optional<BigDecimal> getForecastStock() {
        return Optional.ofNullable(forecastStock);
    }
}
