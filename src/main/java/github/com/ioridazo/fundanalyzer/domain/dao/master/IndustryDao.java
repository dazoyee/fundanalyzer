package github.com.ioridazo.fundanalyzer.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

@ConfigAutowireable
@Dao
public interface IndustryDao {

    @Select
    Industry selectByName(final String name);

    @Select
    String countByName(final String name);

    @Insert
    Result<Industry> insert(final Industry industry);
}
