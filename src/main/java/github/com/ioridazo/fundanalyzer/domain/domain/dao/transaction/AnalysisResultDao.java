package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnusedReturnValue")
@ConfigAutowireable
@Dao
public interface AnalysisResultDao {

    @Select
    Optional<AnalysisResultEntity> selectById(Integer id);

    /**
     * 全件取得する（係数一括再計算バッチの走査対象）。
     *
     * @return 分析結果の全行
     */
    @Select
    List<AnalysisResultEntity> selectAll();

    /**
     * 全件数を取得する（再計算バッチの事前確認用）。
     *
     * @return 全件数
     */
    @Select
    int countAll();

    @Select
    Optional<AnalysisResultEntity> selectByDocumentId(String documentId);

    @Select
    Optional<AnalysisResultEntity> selectByUniqueKey(
            String companyCode,
            LocalDate documentPeriod,
            String documentTypeCode,
            LocalDate submitDate);

    @Select
    List<AnalysisResultEntity> selectByCompanyCodeAndType(String code, List<String> documentTypeCode);

    @Select
    List<AnalysisResultEntity> selectByCodeAndPeriod(String code, LocalDate documentPeriod);

    @Select
    List<AnalysisResultEntity> selectBySubmitDateAndCreatedAt(LocalDate submitDate, LocalDate nowLocalDate);

    @Insert
    Result<AnalysisResultEntity> insert(AnalysisResultEntity analysisResultEntity);

    /**
     * 企業価値・RIM理論株価を更新する（係数一括再計算バッチ専用。param ベースでエンティティ形状に依存しない）。
     *
     * @param id             ID
     * @param corporateValue 企業価値
     * @param rimValue       RIM理論株価（算出不能な場合は null）
     * @return 更新件数
     */
    @Update(sqlFile = true)
    int updateCorporateValueAndRimValue(Integer id, BigDecimal corporateValue, BigDecimal rimValue);
}
