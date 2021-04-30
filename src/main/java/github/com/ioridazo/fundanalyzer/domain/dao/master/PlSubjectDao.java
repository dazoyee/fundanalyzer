package github.com.ioridazo.fundanalyzer.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.entity.master.PlSubjectEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;

import java.util.List;

@ConfigAutowireable
@Dao
public interface PlSubjectDao {

    @Select
    List<PlSubjectEntity> selectAll();

    @Select
    PlSubjectEntity selectById(String id);

    @Select
    PlSubjectEntity selectByUniqueKey(String outlineSubjectId, String detailSubjectId);

    @Select
    List<PlSubjectEntity> selectByOutlineSubjectId(String outlineSubjectId);
}
