package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPriceEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.util.List;

@ConfigAutowireable
@Dao
public interface StockPriceDao {

    @Select
    List<StockPriceEntity> selectByCode(String code);

    @Insert
    Result<StockPriceEntity> insert(StockPriceEntity stockPriceEntity);
}
