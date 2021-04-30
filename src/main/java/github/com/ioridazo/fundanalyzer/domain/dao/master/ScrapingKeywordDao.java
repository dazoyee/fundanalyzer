package github.com.ioridazo.fundanalyzer.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.entity.master.ScrapingKeywordEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;

import java.util.List;

@ConfigAutowireable
@Dao
public interface ScrapingKeywordDao {

    @Select
    List<ScrapingKeywordEntity> selectByFinancialStatementId(String financialStatementId);
}
