package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnusedReturnValue")
@ConfigAutowireable
@Dao
public interface FinancialStatementDao {

    @Select
    Optional<FinancialStatementEntity> selectByUniqueKey(
            String documentId, String financialStatementId, String subjectId);

    @Select
    List<FinancialStatementEntity> selectByCode(String edinetCode);

    @Select
    List<FinancialStatementEntity> selectByCodeAndPeriod(
            String edinetCode, LocalDate periodEnd, String documentTypeCode, LocalDate submitDate);

    @Transactional
    @Insert
    Result<FinancialStatementEntity> insert(FinancialStatementEntity financialStatementEntity);
}
