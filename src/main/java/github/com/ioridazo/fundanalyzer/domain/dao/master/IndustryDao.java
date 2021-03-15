package github.com.ioridazo.fundanalyzer.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.util.List;

@ConfigAutowireable
@Dao
public interface IndustryDao {

    @Select
    List<Industry> selectAll();

    @Select
    Industry selectById(Integer id);

    @Select
    Industry selectByName(String name);

    @Insert(include = {"name", "createdAt"})
    Result<Industry> insert(Industry industry);
}
