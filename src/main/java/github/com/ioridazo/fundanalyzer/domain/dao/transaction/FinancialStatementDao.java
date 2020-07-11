package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatement;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

@ConfigAutowireable
@Dao
public interface FinancialStatementDao {

    // 検索結果が1件以上存在することを保証する
    @Select(ensureResult = true)
    FinancialStatement selectByUniqueKey(
            final String edinetCode,
            final String financialStatementId,
            final String subjectId,
            final String dayOfYear);

    @Insert
    Result<FinancialStatement> insert(final FinancialStatement financialStatement);
}
