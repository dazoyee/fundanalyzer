package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import lombok.Value;
import org.seasar.doma.Entity;

import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
public class StockPriceLatestTargetDateEntity {

    private final String companyCode;

    private final LocalDate targetDate;
}
