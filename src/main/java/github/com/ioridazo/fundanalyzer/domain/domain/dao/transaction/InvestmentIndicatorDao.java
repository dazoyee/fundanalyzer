package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.InvestmentIndicatorEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnusedReturnValue")
@ConfigAutowireable
@Dao
public interface InvestmentIndicatorDao {

    @Select
    Optional<InvestmentIndicatorEntity> selectByCodeAndTargetDate(String code, LocalDate targetDate);

    @Select
    List<InvestmentIndicatorEntity> selectByCode(String code);

    @Select
    List<InvestmentIndicatorEntity> selectByAnalysisResultId(Integer analysisResultId);

    @Insert
    Result<InvestmentIndicatorEntity> insert(InvestmentIndicatorEntity investmentIndicatorEntity);
}
