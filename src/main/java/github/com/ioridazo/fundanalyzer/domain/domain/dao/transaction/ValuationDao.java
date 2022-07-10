package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.time.LocalDate;
import java.util.List;

@ConfigAutowireable
@Dao
public interface ValuationDao {

    @Select
    List<ValuationEntity> selectByCode(String code);

    @Select
    List<ValuationEntity> selectByCodeAndSubmitDate(String code, LocalDate submitDate);

    @Insert
    Result<ValuationEntity> insert(ValuationEntity valuationEntity);
}
