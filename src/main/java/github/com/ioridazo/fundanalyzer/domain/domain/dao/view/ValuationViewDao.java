package github.com.ioridazo.fundanalyzer.domain.domain.dao.view;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.ValuationViewBean;
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
public interface ValuationViewDao {

    @Select
    List<ValuationViewBean> selectAll();

    @Select
    Optional<ValuationViewBean> selectByCode(String code);

    @Insert
    Result<ValuationViewBean> insert(ValuationViewBean valuationViewBean);

    @Update
    Result<ValuationViewBean> update(ValuationViewBean valuationViewBean);
}
