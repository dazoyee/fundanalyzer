package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record IndicatorViewModel(
        LocalDate targetDate,
        BigDecimal priceCorporateValueRatio,
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal grahamIndex
) {

    private static final int THIRD_DECIMAL_PLACE = 3;

    public static IndicatorViewModel of(final IndicatorValue indicatorValue) {
        return new IndicatorViewModel(
                indicatorValue.getTargetDate(),
                indicatorValue.getPriceCorporateValueRatio().setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP),
                indicatorValue.getPer().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                indicatorValue.getPbr().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                indicatorValue.getGrahamIndex().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null)
        );
    }
}
