package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.MinkabuEntity;

import java.time.LocalDate;

public record MinkabuViewModel(
        LocalDate targetDate,
        Double goalsStock,
        Double theoreticalStock) {

    public static MinkabuViewModel of(final MinkabuEntity entity) {
        return new MinkabuViewModel(
                entity.getTargetDate(),
                entity.getGoalsStock().orElse(null),
                entity.getTheoreticalStock().orElse(null)
        );
    }
}
