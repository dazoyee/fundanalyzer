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
public interface EdinetListViewDao {

    @Select
    List<EdinetListViewBean> selectAll();

    @Insert
    Result<EdinetListViewBean> insert(final EdinetListViewBean edinetListViewBean);

    @Update
    Result<EdinetListViewBean> update(final EdinetListViewBean edinetListViewBean);
}
