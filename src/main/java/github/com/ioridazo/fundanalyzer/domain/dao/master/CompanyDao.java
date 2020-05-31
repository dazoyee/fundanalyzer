package github.com.ioridazo.fundanalyzer.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.util.List;

@ConfigAutowireable
@Dao
public interface CompanyDao {

    @Select
    List<Company> selectAll();

    @Select
    Company selectByEdinetCode(final String edinetCode);

    @Insert
    Result<Company> insert(final Company company);
}
