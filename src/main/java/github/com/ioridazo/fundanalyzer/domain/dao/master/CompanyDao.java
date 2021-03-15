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
    Optional<Company> selectByEdinetCode(String edinetCode);

    @Select
    Optional<Company> selectByCode(String code);

    @Insert
    Result<Company> insert(Company company);

    @Update
    Result<Company> update(Company company);
}
