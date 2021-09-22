package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.EdinetDocumentEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ConfigAutowireable
@Dao
public interface EdinetDocumentDao {

    @Select
    EdinetDocumentEntity selectByDocId(String docId);

    @Select
    List<EdinetDocumentEntity> selectBySubmitDate(String submitDate);

    @Select
    int count(String submitDate);

    @Transactional
    @Insert
    Result<EdinetDocumentEntity> insert(EdinetDocumentEntity edinetDocumentEntity);
}
