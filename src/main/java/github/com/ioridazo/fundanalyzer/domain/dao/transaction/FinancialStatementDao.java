package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatement;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.util.List;
import java.util.Optional;

@ConfigAutowireable
@Dao
public interface FinancialStatementDao {

    @Select
    List<FinancialStatement> selectByCode(final String companyCode);

    @Select
    Optional<FinancialStatement> selectByUniqueKey(
            final String edinetCode,
            final String financialStatementId,
            final String subjectId,
            final String dayOfYear);

    @Select
    List<FinancialStatement> selectByEdinetCodeAndFsAndYear(
            final String edinetCode,
            final String financialStatementId,
            final String dayOfYear);

    @Insert
    Result<FinancialStatement> insert(final FinancialStatement financialStatement);
}
