package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.time.LocalDate;
import java.util.List;

@ConfigAutowireable
@Dao
public interface StockPriceDao {

    @Select
    List<StockPriceEntity> selectByCodeAndDate(String code, LocalDate targetDate);

    @Select
    List<StockPriceEntity> selectByCode(String code);

    @Insert
    Result<StockPriceEntity> insert(StockPriceEntity stockPriceEntity);
}
