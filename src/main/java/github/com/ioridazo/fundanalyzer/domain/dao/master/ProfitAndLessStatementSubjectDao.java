package github.com.ioridazo.fundanalyzer.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.entity.master.ProfitAndLessStatementSubject;
import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;

import java.util.List;

@ConfigAutowireable
@Dao
public interface ProfitAndLessStatementSubjectDao {

    @Select
    List<ProfitAndLessStatementSubject> selectAll();
}
