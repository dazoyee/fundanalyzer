package github.com.ioridazo.fundanalyzer.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.entity.master.BsSubject;
import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;

import java.util.List;

@ConfigAutowireable
@Dao
public interface BsSubjectDao {

    @Select
    List<BsSubject> selectAll();

    @Select
    BsSubject selectById(final String id);

    @Select
    BsSubject selectByUniqueKey(final String outlineSubjectId, final String detailSubjectId);

    @Select
    List<BsSubject> selectByOutlineSubjectId(final String outlineSubjectId);
}
