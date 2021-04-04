package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatement;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ConfigAutowireable
@Dao
public interface FinancialStatementDao {

    @Select
    List<FinancialStatement> selectByCode(String companyCode);

    @Select
    Optional<FinancialStatement> selectByUniqueKey(
            String edinetCode,
            String financialStatementId,
            String subjectId,
            String dayOfYear,
            String documentTypeCode,
            LocalDate submitDate);

    @Select
    List<FinancialStatement> selectByEdinetCodeAndFsAndYear(
            String edinetCode,
            String financialStatementId,
            String dayOfYear);

    @Transactional
    @Insert
    Result<FinancialStatement> insert(FinancialStatement financialStatement);
}
