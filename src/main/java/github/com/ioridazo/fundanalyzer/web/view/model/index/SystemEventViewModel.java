package github.com.ioridazo.fundanalyzer.web.view.model.index;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;

import java.time.format.DateTimeFormatter;

public record SystemEventViewModel(
        String eventType,
        String eventTypeLabel,
        String badgeClass,
        String source,
        String message,
        String occurredAt
) {

    private static final DateTimeFormatter OCCURRED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static SystemEventViewModel of(final SystemEventEntity entity) {
        final SystemEventType type = entity.getEventTypeEnum();
        return new SystemEventViewModel(
                type.toValue(),
                type == SystemEventType.ERROR ? "ERROR" : "WARNING",
                type == SystemEventType.ERROR
                        ? "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
                        : "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300",
                entity.getSource(),
                entity.getMessage(),
                entity.getOccurredAt().format(OCCURRED_AT_FORMATTER)
        );
    }
}
