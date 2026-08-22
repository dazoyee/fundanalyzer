package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.time.LocalDateTime;
import java.util.List;

@ConfigAutowireable
@Dao
public interface SystemEventDao {

    @Insert
    Result<SystemEventEntity> insert(SystemEventEntity systemEventEntity);

    @Select
    List<SystemEventEntity> selectRecent(LocalDateTime occurredAtSince, int limit);

    @Select
    long countRecentByType(String eventType, LocalDateTime occurredAtSince, int limit);
}
