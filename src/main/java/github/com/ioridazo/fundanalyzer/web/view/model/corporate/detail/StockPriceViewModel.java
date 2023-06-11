package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;

import java.time.LocalDate;

public record StockPriceViewModel(
        LocalDate targetDate,
        Double stockPrice,
        Double openingPrice,
        Double highPrice,
        Double lowPrice
) {
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
