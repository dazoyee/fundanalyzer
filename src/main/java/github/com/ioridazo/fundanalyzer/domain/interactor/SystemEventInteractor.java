package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.SystemEventDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;
import github.com.ioridazo.fundanalyzer.domain.usecase.SystemEventUseCase;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SystemEventInteractor implements SystemEventUseCase {

    static final int MAX_MESSAGE_LENGTH = 1000;

    private final SystemEventDao systemEventDao;

    public SystemEventInteractor(final SystemEventDao systemEventDao) {
        this.systemEventDao = systemEventDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    @Override
    public void record(final SystemEventType type, final String source, final String message) {
        systemEventDao.insert(SystemEventEntity.of(
                type,
                source,
                buildMessage(type, message),
                nowLocalDateTime()
        ));
    }

    @Override
    public List<SystemEventEntity> findRecent(final int days, final int limit) {
        if (days <= 0 || limit <= 0) {
            return List.of();
        }
        return systemEventDao.selectRecent(occurredAtSince(days), limit);
    }

    @Override
    public long countRecentByType(final SystemEventType type, final int days, final int limit) {
        if (days <= 0 || limit <= 0) {
            return 0;
        }
        return systemEventDao.countRecentByType(type.toValue(), occurredAtSince(days), limit);
    }

    /**
     * 取得対象とする発生日時の下限を返す。
     * 件数ではなく期間で区切ることで、新しいイベントが積まれるのを待たずに古いイベントが表示から外れる。
     *
     * @param days 現在から遡る日数
     * @return 発生日時の下限
     */
    private LocalDateTime occurredAtSince(final int days) {
        return nowLocalDateTime().minusDays(days);
    }

    private String buildMessage(final SystemEventType type, final String message) {
        final String safeMessage = StringUtils.hasText(message) ? message.trim() : "詳細メッセージなし";
        final String builtMessage = switch (type) {
            case ERROR -> "想定外のエラーが発生しました。 " + safeMessage;
            case WARNING -> safeMessage;
        };
        return builtMessage.length() <= MAX_MESSAGE_LENGTH
                ? builtMessage
                : builtMessage.substring(0, MAX_MESSAGE_LENGTH);
    }
}
