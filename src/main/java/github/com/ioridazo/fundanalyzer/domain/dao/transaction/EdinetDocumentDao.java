package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocumentEntity;
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
    List<EdinetDocumentEntity> selectAll();

    @Select
    EdinetDocumentEntity selectByDocId(String docId);

    @Transactional
    @Insert
    Result<EdinetDocumentEntity> insert(EdinetDocumentEntity edinetDocumentEntity);
}
