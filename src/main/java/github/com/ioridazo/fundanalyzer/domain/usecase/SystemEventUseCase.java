package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;

import java.util.List;

public interface SystemEventUseCase {

    void record(SystemEventType type, String source, String message);

    List<SystemEventEntity> findRecent(int limit);

    long countRecentByType(SystemEventType type, int limit);
}
