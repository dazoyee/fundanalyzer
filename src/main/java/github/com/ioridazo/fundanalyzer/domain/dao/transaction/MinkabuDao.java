package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Minkabu;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.util.List;

@ConfigAutowireable
@Dao
public interface MinkabuDao {

    @Select
    List<Minkabu> selectByCode(String code);

    @Insert
    Result<Minkabu> insert(Minkabu minkabu);
}
