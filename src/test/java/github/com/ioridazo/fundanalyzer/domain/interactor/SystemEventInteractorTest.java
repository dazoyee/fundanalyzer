package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.SystemEventDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SystemEventInteractor のテスト")
class SystemEventInteractorTest {

    private SystemEventDao systemEventDao;
    private SystemEventInteractor interactor;

    @BeforeEach
    void setUp() {
        systemEventDao = mock(SystemEventDao.class);
        interactor = spy(new SystemEventInteractor(systemEventDao));
    }

    @Test
    @DisplayName("record : 種別・発生元・メッセージを1件保存する")
    void record_insertsOneEvent() {
        doReturn(LocalDateTime.parse("2026-07-20T10:15:30")).when(interactor).nowLocalDateTime();

        interactor.record(SystemEventType.ERROR, "AnalysisScheduler", "  failure  ");

        final ArgumentCaptor<SystemEventEntity> captor = ArgumentCaptor.forClass(SystemEventEntity.class);
        verify(systemEventDao, times(1)).insert(captor.capture());
        final SystemEventEntity actual = captor.getValue();
        assertEquals(SystemEventType.ERROR, actual.getEventTypeEnum());
        assertEquals("AnalysisScheduler", actual.getSource());
        assertEquals("想定外のエラーが発生しました。 failure", actual.getMessage());
        assertEquals(LocalDateTime.parse("2026-07-20T10:15:30"), actual.getOccurredAt());
    }

    @Test
    @DisplayName("record : WARNING はメッセージをそのまま保存する")
    void record_warningPreservesMessage() {
        doReturn(LocalDateTime.parse("2026-07-20T10:15:30")).when(interactor).nowLocalDateTime();

        interactor.record(SystemEventType.WARNING, "FinancialStatementSpecification", "warning");

        final ArgumentCaptor<SystemEventEntity> captor = ArgumentCaptor.forClass(SystemEventEntity.class);
        verify(systemEventDao, times(1)).insert(captor.capture());
        assertEquals("warning", captor.getValue().getMessage());
    }

    @Test
    @DisplayName("record : 空白メッセージならフォールバック文言を保存する")
    void record_blankMessageUsesFallback() {
        doReturn(LocalDateTime.parse("2026-07-20T10:15:30")).when(interactor).nowLocalDateTime();

        interactor.record(SystemEventType.WARNING, "FinancialStatementSpecification", "   ");

        final ArgumentCaptor<SystemEventEntity> captor = ArgumentCaptor.forClass(SystemEventEntity.class);
        verify(systemEventDao, times(1)).insert(captor.capture());
        assertEquals("詳細メッセージなし", captor.getValue().getMessage());
    }

    @Test
    @DisplayName("record : メッセージが1000文字を超える場合は切り詰める")
    void record_trimsTooLongMessage() {
        doReturn(LocalDateTime.parse("2026-07-20T10:15:30")).when(interactor).nowLocalDateTime();
        final String longMessage = "a".repeat(1200);

        interactor.record(SystemEventType.WARNING, "FinancialStatementSpecification", longMessage);

        final ArgumentCaptor<SystemEventEntity> captor = ArgumentCaptor.forClass(SystemEventEntity.class);
        verify(systemEventDao, times(1)).insert(captor.capture());
        assertEquals(1000, captor.getValue().getMessage().length());
        assertEquals("a".repeat(1000), captor.getValue().getMessage());
    }

    @Test
    @DisplayName("findRecent : 指定日数だけ遡った発生日時を下限に新しい順で取得する")
    void findRecent_returnsNewestFirst() {
        final List<SystemEventEntity> expected = List.of(
                SystemEventEntity.of(SystemEventType.WARNING, "StockScheduler", "newest", LocalDateTime.parse("2026-07-20T12:00:00")),
                SystemEventEntity.of(SystemEventType.ERROR, "AnalysisScheduler", "older", LocalDateTime.parse("2026-07-20T11:00:00"))
        );
        doReturn(LocalDateTime.parse("2026-07-20T12:00:00")).when(interactor).nowLocalDateTime();
        when(systemEventDao.selectRecent(any(LocalDateTime.class), anyInt())).thenReturn(expected);

        final List<SystemEventEntity> actual = interactor.findRecent(7, 100);

        verify(systemEventDao, times(1)).selectRecent(LocalDateTime.parse("2026-07-13T12:00:00"), 100);
        assertIterableEquals(expected, actual);
    }

    @Test
    @DisplayName("findRecent : days が 0 以下なら DAO を呼ばず空を返す")
    void findRecent_nonPositiveDaysReturnsEmpty() {
        assertIterableEquals(List.of(), interactor.findRecent(0, 100));
        verify(systemEventDao, times(0)).selectRecent(any(LocalDateTime.class), anyInt());
    }

    @Test
    @DisplayName("findRecent : limit が 0 以下なら DAO を呼ばず空を返す")
    void findRecent_nonPositiveLimitReturnsEmpty() {
        assertIterableEquals(List.of(), interactor.findRecent(7, 0));
        verify(systemEventDao, times(0)).selectRecent(any(LocalDateTime.class), anyInt());
    }

    @Test
    @DisplayName("countRecentByType : 指定種別の件数を返す")
    void countRecentByType_returnsCount() {
        doReturn(LocalDateTime.parse("2026-07-20T12:00:00")).when(interactor).nowLocalDateTime();
        when(systemEventDao.countRecentByType("ERROR", LocalDateTime.parse("2026-07-13T12:00:00"), 100))
                .thenReturn(4L);

        final long actual = interactor.countRecentByType(SystemEventType.ERROR, 7, 100);

        assertEquals(4L, actual);
        verify(systemEventDao, times(1))
                .countRecentByType("ERROR", LocalDateTime.parse("2026-07-13T12:00:00"), 100);
    }

    @Test
    @DisplayName("countRecentByType : days が 0 以下なら DAO を呼ばず 0 を返す")
    void countRecentByType_nonPositiveDaysReturnsZero() {
        assertEquals(0L, interactor.countRecentByType(SystemEventType.WARNING, 0, 100));
        verify(systemEventDao, times(0)).countRecentByType(anyString(), any(LocalDateTime.class), anyInt());
    }

    @Test
    @DisplayName("countRecentByType : limit が 0 以下なら DAO を呼ばず 0 を返す")
    void countRecentByType_nonPositiveLimitReturnsZero() {
        assertEquals(0L, interactor.countRecentByType(SystemEventType.WARNING, 7, 0));
        verify(systemEventDao, times(0)).countRecentByType(anyString(), any(LocalDateTime.class), anyInt());
    }
}
