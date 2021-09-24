package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ConfigAutowireable
@Dao
public interface DocumentDao {

    @Select
    DocumentEntity selectByDocumentId(String documentId);

    // 検索結果が1件以上存在することを保証する
    @Select(ensureResult = true)
    DocumentEntity selectDocumentBy(String edinetCode, String documentTypeCode, LocalDate submitDate, String yearOfPeriod);

    @Select
    Optional<DocumentEntity> maxSubmitDateByEdinetCodeAndType(String edinetCode, List<String> documentTypeCode);

    @Select
    List<DocumentEntity> selectBySubmitDate(LocalDate submitDate);

    @Select
    List<LocalDate> selectDistinctSubmitDateByDocumentTypeCode(List<String> documentTypeCode);

    @Select
    List<DocumentEntity> selectByTypeAndSubmitDate(List<String> documentTypeCode, LocalDate submitDate);

    @Select
    List<DocumentEntity> selectByTypeAndPeriod(String documentTypeCode, String yearOfPeriod);

    @Select
    List<DocumentEntity> selectByDayOfSubmitDate(String day);

    @Transactional
    @Insert(excludeNull = true)
    Result<DocumentEntity> insert(DocumentEntity documentEntity);

    @Update(excludeNull = true)
    Result<DocumentEntity> update(DocumentEntity documentEntity);
}
