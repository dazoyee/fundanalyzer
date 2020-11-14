package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "stock_price")
public class StockPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Integer id;

    private final String companyCode;

    private final LocalDate targetDate;

    private final Double stockPrice;

    private final Double openingPrice;

    private final Double highPrice;

    private final Double lowPrice;

    private final Integer volume;

    private final String per;

    private final String pbr;

    private final String roe;

    private final String numberOfShares;

    private final String marketCapitalization;

    private final String dividendYield;

    private final String shareholderBenefit;

    private final String sourceOf;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    public Optional<String> getPer() {
        return Optional.ofNullable(per);
    }

    public Optional<String> getPbr() {
        return Optional.ofNullable(pbr);
    }

    public Optional<String> getRoe() {
        return Optional.ofNullable(roe);
    }

    public Optional<String> getNumberOfShares() {
        return Optional.ofNullable(numberOfShares);
    }

    public Optional<String> getMarketCapitalization() {
        return Optional.ofNullable(marketCapitalization);
    }

    public Optional<String> getDividendYield() {
        return Optional.ofNullable(dividendYield);
    }

    public Optional<String> getShareholderBenefit() {
        return Optional.ofNullable(shareholderBenefit);
    }
}
