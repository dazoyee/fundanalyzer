package github.com.ioridazo.fundanalyzer.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.entity.master.PlSubject;
import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;

import java.util.List;

@ConfigAutowireable
@Dao
public interface PlSubjectDao {

    @Select
    List<PlSubject> selectAll();

    @Select
    PlSubject selectById(final String id);

    @Select
    PlSubject selectByUniqueKey(final String outlineSubjectId, final String detailSubjectId);

    @Select
    List<PlSubject> selectByOutlineSubjectId(final String outlineSubjectId);
}
