package github.com.ioridazo.fundanalyzer.domain.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.IndustryEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.util.List;

@ConfigAutowireable
@Dao
public interface IndustryDao {

    @Select
    List<IndustryEntity> selectAll();

    // 係数列(operating_profit_weight / current_liabilities_ratio / cost_of_equity)は意図的に除外し、DB の DEFAULT(10 / 1.2 / 0.08)を適用する
    @Insert(include = {"name", "createdAt"})
    Result<IndustryEntity> insert(IndustryEntity industryEntity);
}
