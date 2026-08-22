package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;

import java.util.List;

public interface SystemEventUseCase {

    void record(SystemEventType type, String source, String message);

    /**
     * 直近の一定期間に発生したイベントを新しい順に取得する。
     *
     * @param days  現在から遡る日数
     * @param limit 期間内で取得する最大件数
     * @return イベントリスト（発生日時の降順）
     */
    List<SystemEventEntity> findRecent(int days, int limit);

    /**
     * 直近の一定期間に発生したイベントのうち、指定種別の件数を数える。
     *
     * @param type  イベント種別
     * @param days  現在から遡る日数
     * @param limit 期間内で対象とする最大件数
     * @return 件数
     */
    long countRecentByType(SystemEventType type, int days, int limit);
}
