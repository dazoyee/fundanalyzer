package github.com.ioridazo.fundanalyzer.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.entity.master.BsSubjectEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;

import java.util.List;

@ConfigAutowireable
@Dao
public interface BsSubjectDao {

    @Select
    List<BsSubjectEntity> selectAll();

    @Select
    BsSubjectEntity selectById(String id);

    @Select
    BsSubjectEntity selectByUniqueKey(String outlineSubjectId, String detailSubjectId);

    @Select
    List<BsSubjectEntity> selectByOutlineSubjectId(String outlineSubjectId);
}
