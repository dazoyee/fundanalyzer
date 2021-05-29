package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.MinkabuEntity;
import lombok.Value;

import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class MinkabuViewModel {
    private final LocalDate targetDate;
    private final Double goalsStock;
    private final Double theoreticalStock;

    public static MinkabuViewModel of(final MinkabuEntity entity) {
        return new MinkabuViewModel(
                entity.getTargetDate(),
                entity.getGoalsStock(),
                entity.getTheoreticalStock()
        );
    }
}
