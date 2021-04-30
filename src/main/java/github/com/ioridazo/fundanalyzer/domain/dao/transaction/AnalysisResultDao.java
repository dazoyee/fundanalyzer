package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResultEntity;
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
public interface AnalysisResultDao {

    @Select
    Optional<AnalysisResultEntity> selectByUniqueKey(
            String companyCode,
            LocalDate documentPeriod,
            String documentTypeCode,
            LocalDate submitDate);

    @Select
    List<AnalysisResultEntity> selectByCompanyCode(String code);

    @Select
    List<AnalysisResultEntity> selectByPeriod(LocalDate documentPeriod);

    @Insert
    Result<AnalysisResultEntity> insert(AnalysisResultEntity analysisResultEntity);
}
