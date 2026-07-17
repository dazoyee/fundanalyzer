package github.com.ioridazo.fundanalyzer.domain.domain.entity.master;

import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(immutable = true)
@Table(name = "view_filter_setting")
public record ViewFilterSettingEntity(

        @Id
        Integer id,

        BigDecimal discountRate,

        BigDecimal outlierOfStandardDeviation,

        BigDecimal coefficientOfVariation,

        BigDecimal diffForecastStock,

        Integer corporateSize,

        LocalDateTime updatedAt
) {
}
