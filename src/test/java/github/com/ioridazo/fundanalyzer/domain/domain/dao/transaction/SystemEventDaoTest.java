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

        final List<SystemEventEntity> actual = systemEventDao.selectRecent(2);

        assertEquals(2, actual.size());
        assertEquals(SystemEventType.ERROR, actual.get(0).getEventTypeEnum());
        assertEquals("second", actual.get(0).getMessage());
        assertEquals(SystemEventType.WARNING, actual.get(1).getEventTypeEnum());
        assertEquals("first", actual.get(1).getMessage());
    }

    @DisplayName("countRecentByType : 直近 N 件を確定してから種別件数を集計する")
    @Test
    void countRecentByType() {
        final LocalDateTime baseTime = LocalDateTime.parse("2026-07-20T09:00:00");
        for (int i = 0; i < 21; i++) {
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
                baseTime.plusMinutes(21)
        ));

        final long recentErrorCount = systemEventDao.countRecentByType(SystemEventType.ERROR.name(), 20);
        final long recentWarningCount = systemEventDao.countRecentByType(SystemEventType.WARNING.name(), 20);

        assertEquals(19L, recentErrorCount);
        assertEquals(1L, recentWarningCount);
    }
}
