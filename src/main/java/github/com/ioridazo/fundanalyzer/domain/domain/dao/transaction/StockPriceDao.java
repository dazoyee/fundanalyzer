package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Delete;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ConfigAutowireable
@Dao
public interface StockPriceDao {

    @Select
    Optional<StockPriceEntity> selectByUniqueKey(String code, LocalDate targetDate, String sourceOf);

    @Select
    List<StockPriceEntity> selectByCodeAndDate(String code, LocalDate targetDate);

    @Select
    List<StockPriceEntity> selectByCode(String code);

    @Select
    List<LocalDate> selectDistinctTargetDate();

    @Insert
    Result<StockPriceEntity> insert(StockPriceEntity stockPriceEntity);

    @Update(excludeNull = true)
    Result<StockPriceEntity> update(StockPriceEntity stockPriceEntity);

    @Delete(sqlFile = true)
    int delete(LocalDate targetDate);
}
