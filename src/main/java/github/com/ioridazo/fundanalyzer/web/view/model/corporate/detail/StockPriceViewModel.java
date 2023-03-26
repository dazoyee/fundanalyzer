package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import lombok.Value;

import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class StockPriceViewModel {
    private final LocalDate targetDate;
    private final Double stockPrice;
    private final Double openingPrice;
    private final Double highPrice;
    private final Double lowPrice;

    public static StockPriceViewModel of(final StockPriceEntity entity) {
        return new StockPriceViewModel(
                entity.getTargetDate(),
                entity.getStockPrice(),
                entity.getOpeningPrice().orElse(null),
                entity.getHighPrice().orElse(null),
                entity.getLowPrice().orElse(null)
        );
    }
}
