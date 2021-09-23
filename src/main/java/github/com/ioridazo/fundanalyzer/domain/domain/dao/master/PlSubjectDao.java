package github.com.ioridazo.fundanalyzer.domain.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.PlSubjectEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;

import java.util.List;

@ConfigAutowireable
@Dao
public interface PlSubjectDao {

    @Select
    List<PlSubjectEntity> selectAll();
}
