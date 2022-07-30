package github.com.ioridazo.fundanalyzer.domain.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.CompanyEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ConfigAutowireable
@Dao
public interface CompanyDao {

    @Select
    Optional<CompanyEntity> selectByEdinetCode(String edinetCode);

    @Select
    Optional<CompanyEntity> selectByCode(String code);

    @Select
    Optional<LocalDateTime> maxUpdatedAt();

    @Select
    List<CompanyEntity> selectByCodeIsNotNull();

    @Select
    List<CompanyEntity> selectByFavorite();

    @Select
    List<CompanyEntity> selectByIndustryId(Integer industryId);

    @Insert
    Result<CompanyEntity> insert(CompanyEntity company);

    @Update(excludeNull = true)
    Result<CompanyEntity> update(CompanyEntity company);
}
