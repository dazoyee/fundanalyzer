package github.com.ioridazo.fundanalyzer.domain.bean;

import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;
}
