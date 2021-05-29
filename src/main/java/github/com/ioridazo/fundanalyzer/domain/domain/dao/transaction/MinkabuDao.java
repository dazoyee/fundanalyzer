package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.MinkabuEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ConfigAutowireable
@Dao
public interface MinkabuDao {

    @Select
    Optional<MinkabuEntity> selectByCodeAndDate(String code, LocalDate targetDate);

    @Select
    List<MinkabuEntity> selectByCode(String code);

    @Insert
    Result<MinkabuEntity> insert(MinkabuEntity minkabuEntity);
}
