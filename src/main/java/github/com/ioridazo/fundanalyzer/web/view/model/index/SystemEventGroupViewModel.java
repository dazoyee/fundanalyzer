package github.com.ioridazo.fundanalyzer.web.view.model.index;

import java.util.List;

/**
 * 同一企業・同一書類に対して発生したシステムイベントをまとめた表示単位。
 * 1 書類のスクレイピングで複数科目が警告されるため、
 * イベント単位で並べると一覧が同一書類の警告で埋まり全体像が読み取れなくなる。
 *
 * @param label            グループの見出し（企業コード、企業コードがなければイベント発生源）
 * @param documentId       書類ID。ラベル形式ではないメッセージのときは null
 * @param source           イベント発生源
 * @param eventTypeLabel   グループ内で最も重い種別のラベル
 * @param badgeClass       グループ内で最も重い種別のバッジ用 CSS クラス
 * @param count            グループ内のイベント件数
 * @param latestOccurredAt グループ内で最も新しい発生日時
 * @param events           グループに属するイベント（発生日時の降順）
 */
public record SystemEventGroupViewModel(
        String label,
        String documentId,
        String source,
        String eventTypeLabel,
        String badgeClass,
        int count,
        String latestOccurredAt,
        List<SystemEventViewModel> events
) {

    /**
     * イベントリストからグループを組み立てる。
     *
     * @param events 同一グループに属するイベント（発生日時の降順）
     * @return グループ
     */
    public static SystemEventGroupViewModel of(final List<SystemEventViewModel> events) {
        final SystemEventViewModel latest = events.get(0);
        final SystemEventViewModel severest = events.stream()
                .filter(event -> "ERROR".equals(event.eventType()))
                .findFirst()
                .orElse(latest);
        return new SystemEventGroupViewModel(
                latest.companyCode() != null ? latest.companyCode() : latest.source(),
                latest.documentId(),
                latest.source(),
                severest.eventTypeLabel(),
                severest.badgeClass(),
                events.size(),
                latest.occurredAt(),
                events
        );
    }
}
