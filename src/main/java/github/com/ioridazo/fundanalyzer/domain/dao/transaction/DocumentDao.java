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
    Document selectByDocumentId(final String documentId);

    // 検索結果が1件以上存在することを保証する
    @Select(ensureResult = true)
    Document selectDocumentIdBy(final String edinetCode, final String documentTypeCode, final String yearOfPeriod);

    @Select
    List<Document> selectByDocumentTypeCode(final String documentTypeCode);

    @Select
    List<Document> selectByTypeAndSubmitDate(final String documentTypeCode, final LocalDate submitDate);

    @Select
    List<Document> selectByTypeAndPeriod(final String documentTypeCode, final String yearOfPeriod);

    @Insert(excludeNull = true)
    Result<Document> insert(final Document document);

    @Update(excludeNull = true)
    Result<Document> update(final Document document);
}
