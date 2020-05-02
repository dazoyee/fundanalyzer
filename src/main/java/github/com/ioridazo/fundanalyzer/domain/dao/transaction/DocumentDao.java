package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.util.List;

@ConfigAutowireable
@Dao
public interface DocumentDao {

    @Select
    List<Document> selectByDocTypeCode(final String docTypeCode);

    @Insert
    Result<Document> insert(final Document document);

    @Update
    Result<Document> update(final Document document);
}
