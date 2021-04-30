package github.com.ioridazo.fundanalyzer.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.entity.master.IndustryEntity;
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
    List<IndustryEntity> selectAll();

    @Select
    IndustryEntity selectById(Integer id);

    @Select
    IndustryEntity selectByName(String name);

    @Insert(include = {"name", "createdAt"})
    Result<IndustryEntity> insert(IndustryEntity industryEntity);
}
