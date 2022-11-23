package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.InvestmentIndicatorEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.util.List;

@ConfigAutowireable
@Dao
public interface InvestmentIndicatorDao {

    @Select
    List<InvestmentIndicatorEntity> selectByCode(String code);

    @Insert
    Result<InvestmentIndicatorEntity> insert(InvestmentIndicatorEntity investmentIndicatorEntity);
}
