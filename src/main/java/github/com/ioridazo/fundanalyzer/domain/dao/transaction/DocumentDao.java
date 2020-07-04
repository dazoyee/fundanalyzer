package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
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
public interface DocumentDao {

    @Select
    Document selectByDocId(final String docId);

    @Select
    List<Document> selectByDateAndDocTypeCode(final LocalDate submitDate, final String docTypeCode);

    @Insert(excludeNull = true)
    Result<Document> insert(final Document document);

    @Update(excludeNull = true)
    Result<Document> update(final Document document);
}
