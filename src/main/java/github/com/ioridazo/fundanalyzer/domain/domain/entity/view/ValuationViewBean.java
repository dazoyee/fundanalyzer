package github.com.ioridazo.fundanalyzer.domain.domain.entity.view;

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
@Table(name = "valuation_view")
public class ValuationViewBean {

    // 証券コード
    @Id
    private final String code;

    // 会社名
    private final String name;

    // 対象日付
    private final LocalDate targetDate;

    // 株価終値
    private final BigDecimal stockPrice;

    // グレアム指数
    private final BigDecimal grahamIndex;

    // 割安値
    private final BigDecimal discountValue;

    // 割安度
    private final BigDecimal discountRate;

    // 提出日
    private final LocalDate submitDate;

    // 提出日の株価終値
    private final BigDecimal stockPriceOfSubmitDate;

    // 提出日からの日数
    private final Long daySinceSubmitDate;

    // 提出日との株価の差
    private final BigDecimal differenceFromSubmitDate;

    // 提出日との株価比率
    private final BigDecimal submitDateRatio;

    // 提出日のグレアム指数
    private final BigDecimal grahamIndexOfSubmitDate;

    // 企業価値
    private final BigDecimal corporateValue;

    // 予想配当利回り
    private final BigDecimal dividendYield;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;

    public Optional<BigDecimal> getGrahamIndex() {
        return Optional.ofNullable(grahamIndex);
    }

    public Optional<BigDecimal> getGrahamIndexOfSubmitDate() {
        return Optional.ofNullable(grahamIndexOfSubmitDate);
    }

    public Optional<BigDecimal> getDividendYield() {
        return Optional.ofNullable(dividendYield);
    }
}
