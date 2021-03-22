package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@ConfigAutowireable
@Dao
public interface DocumentDao {

    @Select
    Document selectByDocumentId(String documentId);

    // 検索結果が1件以上存在することを保証する
    @Select(ensureResult = true)
    Document selectDocumentIdBy(String edinetCode, String documentTypeCode, String yearOfPeriod);

    @Select
    List<Document> selectByDocumentTypeCode(List<String> documentTypeCode);

    @Select
    List<Document> selectByTypeAndSubmitDate(List<String> documentTypeCode, LocalDate submitDate);

    @Select
    List<Document> selectByTypeAndPeriod(String documentTypeCode, String yearOfPeriod);

    @Select
    List<Document> selectByDayOfSubmitDate(String day);

    @Transactional
    @Insert(excludeNull = true)
    Result<Document> insert(Document document);

    @Update(excludeNull = true)
    Result<Document> update(Document document);
}
