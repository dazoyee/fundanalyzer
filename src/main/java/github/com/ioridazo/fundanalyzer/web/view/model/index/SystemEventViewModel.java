package github.com.ioridazo.fundanalyzer.web.view.model.index;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;

import java.time.format.DateTimeFormatter;

/**
 * システムイベント 1 件の表示情報。
 *
 * @param eventType       種別
 * @param eventTypeLabel  種別のラベル
 * @param badgeClass      種別に対応するバッジ用 CSS クラス
 * @param source          イベント発生源
 * @param message         メッセージ全文
 * @param occurredAt      発生日時
 * @param companyCode     メッセージから読み取った企業コード。読み取れないときは null
 * @param documentId      メッセージから読み取った書類ID。読み取れないときは null
 * @param detail          明細行に表示する要約
 */
public record SystemEventViewModel(
        String eventType,
        String eventTypeLabel,
        String badgeClass,
        String source,
        String message,
        String occurredAt,
        String companyCode,
        String documentId,
        String detail
) {

    private static final DateTimeFormatter OCCURRED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static SystemEventViewModel of(final SystemEventEntity entity) {
        final SystemEventType type = entity.getEventTypeEnum();
        final String message = entity.getMessage();
        return new SystemEventViewModel(
                type.toValue(),
                type == SystemEventType.ERROR ? "ERROR" : "WARNING",
                type == SystemEventType.ERROR
                        ? "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
                        : "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300",
                entity.getSource(),
                message,
                entity.getOccurredAt().format(OCCURRED_AT_FORMATTER),
                SystemEventMessageFields.companyCode(message).orElse(null),
                SystemEventMessageFields.documentId(message).orElse(null),
                SystemEventMessageFields.detail(message)
        );
    }
}
