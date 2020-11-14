package github.com.ioridazo.fundanalyzer.domain.bean;

import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.util.List;

@ConfigAutowireable
@Dao
public interface CorporateViewDao {

    @Select
    List<CorporateViewBean> selectAll();

    @Select
    CorporateViewBean selectByCode(final String code);

    @Insert
    Result<CorporateViewBean> insert(final CorporateViewBean corporateViewBean);

    @Update
    Result<CorporateViewBean> update(final CorporateViewBean corporateViewBean);
}
