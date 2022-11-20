package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(immutable = true)
@Table(name = "investment_indicator")
public record InvestmentIndicatorEntity(

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Integer id,

        Integer stockId,

        Integer analysisResultId,

        String companyCode,

        LocalDate targetDate,

        BigDecimal priceCorporateValueRatio,

        BigDecimal per,

        BigDecimal pbr,

        String documentId,

        @Column(updatable = false)
        LocalDateTime createdAt
) {

    public static InvestmentIndicatorEntity of(
            final Integer stockId,
            final Integer analysisResultId,
            final String companyCode,
            final LocalDate targetDate,
            final BigDecimal priceCorporateValueRatio,
            final BigDecimal per,
            final BigDecimal pbr,
            final String documentId,
            final LocalDateTime createdAt
    ) {
        return new InvestmentIndicatorEntity(
                null,
                stockId,
                analysisResultId,
                companyCode,
                targetDate,
                priceCorporateValueRatio,
                per,
                pbr,
                documentId,
                createdAt
        );
    }
}
