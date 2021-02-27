package github.com.ioridazo.fundanalyzer.domain.logic.view.bean;

import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.time.LocalDate;
import java.util.List;

@ConfigAutowireable
@Dao
public interface CorporateViewDao {

    @Select
    List<CorporateViewBean> selectAll();

    @Select
    CorporateViewBean selectByCode(final String code);

    @Select
    List<CorporateViewBean> selectBySubmitDate(final LocalDate submitDate);

    @Insert
    Result<CorporateViewBean> insert(final CorporateViewBean corporateViewBean);

    @Update
    Result<CorporateViewBean> update(final CorporateViewBean corporateViewBean);
}
