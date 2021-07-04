package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
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
    List<AnalysisResultEntity> selectByCompanyCodeAndType(String code, List<String> documentTypeCode);

    @Select
    List<AnalysisResultEntity> selectByCodeAndPeriod(String code, LocalDate documentPeriod);

    @Insert
    Result<AnalysisResultEntity> insert(AnalysisResultEntity analysisResultEntity);
}
