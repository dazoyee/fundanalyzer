package github.com.ioridazo.fundanalyzer.web.view.model.index;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemEventSummaryViewModelTest {

    private static final String VALIDATION_SOURCE = "FinancialStatementSpecification";

    @Nested
    @DisplayName("of メソッド")
    class Of {

        @DisplayName("of : イベントがないとき件数0で最終発生日時はnull")
        @Test
        void empty() {
            final SystemEventSummaryViewModel actual = SystemEventSummaryViewModel.of(List.of());

            assertEquals(0L, actual.totalCount());
            assertEquals(0L, actual.errorCount());
            assertEquals(0L, actual.warningCount());
            assertNull(actual.latestOccurredAt());
            assertTrue(actual.groups().isEmpty());
        }

        @DisplayName("of : 同一企業・同一書類の警告は1グループにまとめる")
        @Test
        void groupsByDocument() {
            final SystemEventSummaryViewModel actual = SystemEventSummaryViewModel.of(List.of(
                    validationEvent("27760", "S100YXHH", "損益計算書", "10", "2026-08-20T02:06:49"),
                    validationEvent("27760", "S100YXHH", "貸借対照表", "25", "2026-08-20T02:06:48"),
                    validationEvent("38560", "S100YXGU", "損益計算書", "11", "2026-08-19T02:06:07")
            ));

            assertEquals(3L, actual.totalCount());
            assertEquals(3L, actual.warningCount());
            assertEquals(0L, actual.errorCount());
            assertEquals("2026-08-20 02:06:49", actual.latestOccurredAt());
            assertEquals(2, actual.groups().size());

            final SystemEventGroupViewModel first = actual.groups().get(0);
            assertEquals("27760", first.label());
            assertEquals("S100YXHH", first.documentId());
            assertEquals(2, first.count());
            assertEquals("WARNING", first.eventTypeLabel());
            assertEquals("2026-08-20 02:06:49", first.latestOccurredAt());

            final SystemEventGroupViewModel second = actual.groups().get(1);
            assertEquals("38560", second.label());
            assertEquals(1, second.count());
        }

        @DisplayName("of : 企業・書類を特定できないイベントは発生源ごとにまとめる")
        @Test
        void groupsBySourceWhenUnparsable() {
            final SystemEventSummaryViewModel actual = SystemEventSummaryViewModel.of(List.of(
                    schedulerError("AnalysisScheduler", "2026-08-20T03:00:00"),
                    schedulerError("AnalysisScheduler", "2026-08-19T03:00:00"),
                    schedulerError("StockScheduler", "2026-08-18T03:00:00")
            ));

            assertEquals(3L, actual.errorCount());
            assertEquals(2, actual.groups().size());
            assertEquals("AnalysisScheduler", actual.groups().get(0).label());
            assertNull(actual.groups().get(0).documentId());
            assertEquals(2, actual.groups().get(0).count());
            assertEquals("ERROR", actual.groups().get(0).eventTypeLabel());
        }

        @DisplayName("of : 明細はメッセージから財務諸表・科目・前回値・今回値・比率を抜き出して要約する")
        @Test
        void detailSummarizesMessage() {
            final SystemEventSummaryViewModel actual = SystemEventSummaryViewModel.of(List.of(
                    validationEvent("27760", "S100YXHH", "損益計算書", "10", "2026-08-20T02:06:49")));

            assertEquals(
                    "損益計算書 科目ID:10 1,430,000 → 21,879,000（比率 15.3）",
                    actual.groups().get(0).events().get(0).detail());
        }

        @DisplayName("of : ラベル形式ではないメッセージは明細にそのまま表示する")
        @Test
        void detailFallsBackToRawMessage() {
            final SystemEventSummaryViewModel actual = SystemEventSummaryViewModel.of(List.of(
                    schedulerError("StockScheduler", "2026-08-18T03:00:00")));

            assertEquals(
                    "想定外のエラーが発生しました。 株価更新に失敗しました",
                    actual.groups().get(0).events().get(0).detail());
        }

        @DisplayName("of : 同一グループにERRORが含まれるときはERRORを見出しの種別にする")
        @Test
        void severestTypeWins() {
            final SystemEventSummaryViewModel actual = SystemEventSummaryViewModel.of(List.of(
                    SystemEventEntity.of(
                            SystemEventType.WARNING,
                            "AnalysisScheduler",
                            "警告",
                            LocalDateTime.parse("2026-08-20T03:00:00")),
                    SystemEventEntity.of(
                            SystemEventType.ERROR,
                            "AnalysisScheduler",
                            "想定外のエラーが発生しました。 failure",
                            LocalDateTime.parse("2026-08-19T03:00:00"))
            ));

            assertEquals(1, actual.groups().size());
            assertEquals("ERROR", actual.groups().get(0).eventTypeLabel());
            assertEquals(1L, actual.errorCount());
            assertEquals(1L, actual.warningCount());
        }

        private SystemEventEntity validationEvent(
                final String companyCode,
                final String documentId,
                final String statement,
                final String subjectId,
                final String occurredAt) {
            return SystemEventEntity.of(
                    SystemEventType.WARNING,
                    VALIDATION_SOURCE,
                    "企業コード:" + companyCode + " EDINET:E02960 財務諸表:" + statement
                    + " 科目ID:" + subjectId + " 書類ID:" + documentId
                    + " 当期末:2025-01-31 前回期末:2024-01-31 前回値:1,430,000 今回値:21,879,000 比率:15.3",
                    LocalDateTime.parse(occurredAt)
            );
        }

        private SystemEventEntity schedulerError(final String source, final String occurredAt) {
            return SystemEventEntity.of(
                    SystemEventType.ERROR,
                    source,
                    "想定外のエラーが発生しました。 株価更新に失敗しました",
                    LocalDateTime.parse(occurredAt)
            );
        }
    }
}
