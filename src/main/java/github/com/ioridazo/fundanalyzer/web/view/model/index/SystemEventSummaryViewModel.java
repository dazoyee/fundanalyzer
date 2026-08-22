package github.com.ioridazo.fundanalyzer.web.view.model.index;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 直近システムイベントの要約。
 * 既定では種別ごとの件数と最終発生日時だけを示し、明細はグループ単位に折りたたむ。
 *
 * @param errorCount       ERROR の件数
 * @param warningCount     WARNING の件数
 * @param totalCount       イベント総件数
 * @param latestOccurredAt 最も新しい発生日時。イベントがないときは null
 * @param groups           企業・書類単位に集約したグループ（発生日時の降順）
 */
public record SystemEventSummaryViewModel(
        long errorCount,
        long warningCount,
        long totalCount,
        String latestOccurredAt,
        List<SystemEventGroupViewModel> groups
) {

    /**
     * イベントエンティティのリストから要約を組み立てる。
     *
     * @param entities 発生日時の降順に並んだイベント
     * @return 要約
     */
    public static SystemEventSummaryViewModel of(final List<SystemEventEntity> entities) {
        final List<SystemEventViewModel> events = entities.stream()
                .map(SystemEventViewModel::of)
                .toList();
        final Map<String, List<SystemEventViewModel>> grouped = new LinkedHashMap<>();
        for (final SystemEventViewModel event : events) {
            grouped.computeIfAbsent(groupKey(event), key -> new ArrayList<>()).add(event);
        }
        return new SystemEventSummaryViewModel(
                countByType(events, SystemEventType.ERROR),
                countByType(events, SystemEventType.WARNING),
                events.size(),
                events.isEmpty() ? null : events.get(0).occurredAt(),
                grouped.values().stream()
                        .map(SystemEventGroupViewModel::of)
                        .toList()
        );
    }

    private static String groupKey(final SystemEventViewModel event) {
        if (event.companyCode() == null || event.documentId() == null) {
            // 企業・書類を特定できないイベントは発生源ごとにまとめる
            return "source:" + event.source();
        }
        return "document:" + event.companyCode() + "/" + event.documentId();
    }

    private static long countByType(final List<SystemEventViewModel> events, final SystemEventType type) {
        return events.stream()
                .filter(event -> type.toValue().equals(event.eventType()))
                .count();
    }
}
