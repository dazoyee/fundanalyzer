package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class SystemEventDaoTest {

    @Autowired
    private SystemEventDao systemEventDao;

    @DisplayName("insert/selectRecent : H2 で INSERT/SELECT が動作し新しい順に取得できる")
    @Test
    void insertAndSelectRecent() {
        systemEventDao.insert(SystemEventEntity.of(
                SystemEventType.WARNING,
                "FinancialStatementSpecification",
                "first",
                LocalDateTime.parse("2026-07-20T09:00:00")
        ));
        systemEventDao.insert(SystemEventEntity.of(
                SystemEventType.ERROR,
                "AnalysisScheduler",
                "second",
                LocalDateTime.parse("2026-07-20T10:00:00")
        ));

        final List<SystemEventEntity> actual = systemEventDao.selectRecent(
                LocalDateTime.parse("2026-07-20T00:00:00"), 2);

        assertEquals(2, actual.size());
        assertEquals(SystemEventType.ERROR, actual.get(0).getEventTypeEnum());
        assertEquals("second", actual.get(0).getMessage());
        assertEquals(SystemEventType.WARNING, actual.get(1).getEventTypeEnum());
        assertEquals("first", actual.get(1).getMessage());
    }

    @DisplayName("selectRecent : 発生日時が下限より古いイベントは取得しない")
    @Test
    void selectRecentExcludesOlderThanSince() {
        systemEventDao.insert(SystemEventEntity.of(
                SystemEventType.WARNING,
                "FinancialStatementSpecification",
                "old",
                LocalDateTime.parse("2026-07-10T09:00:00")
        ));
        systemEventDao.insert(SystemEventEntity.of(
                SystemEventType.WARNING,
                "FinancialStatementSpecification",
                "new",
                LocalDateTime.parse("2026-07-20T09:00:00")
        ));

        final List<SystemEventEntity> actual = systemEventDao.selectRecent(
                LocalDateTime.parse("2026-07-13T00:00:00"), 100);

        assertEquals(1, actual.size());
        assertEquals("new", actual.get(0).getMessage());
    }

    @DisplayName("countRecentByType : 発生日時の下限で絞ってから種別件数を集計する")
    @Test
    void countRecentByType() {
        final LocalDateTime baseTime = LocalDateTime.parse("2026-07-20T09:00:00");
        for (int i = 0; i < 3; i++) {
            systemEventDao.insert(SystemEventEntity.of(
                    SystemEventType.ERROR,
                    "AnalysisScheduler",
                    "error-" + i,
                    baseTime.plusMinutes(i)
            ));
        }
        systemEventDao.insert(SystemEventEntity.of(
                SystemEventType.WARNING,
                "AnalysisScheduler",
                "warning-latest",
                baseTime.plusMinutes(3)
        ));
        // 期間外のため集計対象にならない
        systemEventDao.insert(SystemEventEntity.of(
                SystemEventType.ERROR,
                "AnalysisScheduler",
                "error-out-of-window",
                LocalDateTime.parse("2026-07-10T09:00:00")
        ));

        final LocalDateTime since = LocalDateTime.parse("2026-07-13T00:00:00");
        final long recentErrorCount = systemEventDao.countRecentByType(SystemEventType.ERROR.name(), since, 100);
        final long recentWarningCount = systemEventDao.countRecentByType(SystemEventType.WARNING.name(), since, 100);

        assertEquals(3L, recentErrorCount);
        assertEquals(1L, recentWarningCount);
    }

    @DisplayName("countRecentByType : 上限件数で切り取ってから種別件数を集計する")
    @Test
    void countRecentByTypeAppliesLimit() {
        final LocalDateTime baseTime = LocalDateTime.parse("2026-07-20T09:00:00");
        for (int i = 0; i < 3; i++) {
            systemEventDao.insert(SystemEventEntity.of(
                    SystemEventType.ERROR,
                    "AnalysisScheduler",
                    "error-" + i,
                    baseTime.plusMinutes(i)
            ));
        }
        systemEventDao.insert(SystemEventEntity.of(
                SystemEventType.WARNING,
                "AnalysisScheduler",
                "warning-latest",
                baseTime.plusMinutes(3)
        ));

        final LocalDateTime since = LocalDateTime.parse("2026-07-13T00:00:00");

        // 上限 2 件だと新しい順に WARNING 1 件と ERROR 1 件だけが対象になる
        assertEquals(1L, systemEventDao.countRecentByType(SystemEventType.ERROR.name(), since, 2));
        assertEquals(1L, systemEventDao.countRecentByType(SystemEventType.WARNING.name(), since, 2));
    }
}
