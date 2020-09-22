package github.com.ioridazo.fundanalyzer.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.util.List;
import java.util.Optional;

@ConfigAutowireable
@Dao
public interface CompanyDao {

    @Select
    List<Company> selectAll();

    @Select
    Optional<Company> selectByEdinetCode(final String edinetCode);

    @Select
    Optional<Company> selectByCode(final String code);

    @Insert
    Result<Company> insert(final Company company);

    @Update
    Result<Company> update(final Company company);
}
