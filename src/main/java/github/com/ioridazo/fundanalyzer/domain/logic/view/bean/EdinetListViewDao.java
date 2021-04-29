package github.com.ioridazo.fundanalyzer.domain.logic.view.bean;

import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ConfigAutowireable
@Dao
public interface EdinetListViewDao {

    @Select
    List<EdinetListViewBean> selectAll();

    @Select
    Optional<EdinetListViewBean> selectBySubmitDate(LocalDate submitDate);

    @Insert
    Result<EdinetListViewBean> insert(EdinetListViewBean edinetListViewBean);

    @Update
    Result<EdinetListViewBean> update(EdinetListViewBean edinetListViewBean);
}
