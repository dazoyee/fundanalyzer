package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.MinkabuEntity;
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
    List<MinkabuEntity> selectByCode(String code);

    @Insert
    Result<MinkabuEntity> insert(MinkabuEntity minkabuEntity);
}
