package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDateTime;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@AllArgsConstructor
@Entity(immutable = true)
@Table(name = "system_event")
public class SystemEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    private final String eventType;

    private final String source;

    private final String message;

    private final LocalDateTime occurredAt;

    public static SystemEventEntity of(
            final SystemEventType eventType,
            final String source,
            final String message,
            final LocalDateTime occurredAt) {
        return new SystemEventEntity(null, eventType.toValue(), source, message, occurredAt);
    }

    public SystemEventType getEventTypeEnum() {
        return SystemEventType.fromValue(eventType);
    }
}
