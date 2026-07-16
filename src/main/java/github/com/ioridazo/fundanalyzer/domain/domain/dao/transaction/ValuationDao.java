package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnusedReturnValue")
@ConfigAutowireable
@Dao
public interface ValuationDao {

    @Select
    Optional<ValuationEntity> selectByUnique(String code, LocalDate targetDate, LocalDate submitDate);

    @Select
    List<ValuationEntity> selectByCode(String code);

    @Select
    List<ValuationEntity> selectByCodeAndSubmitDate(String code, LocalDate submitDate);

    /**
     * 全件数を取得する（再計算バッチの事前確認用）。
     *
     * @return 全件数
     */
    @Select
    int countAll();

    @Insert
    Result<ValuationEntity> insert(ValuationEntity valuationEntity);

    /**
     * analysis_result の企業価値（再計算後の現行係数値）を基に、係数依存の割引値・割引率を一括更新する。
     *
     * <p>相関サブクエリで {@code analysis_result.corporate_value} を都度参照する集合 UPDATE のため、
     * MySQL/H2 いずれの方言でも同一 SQL で動作する（MySQL の UPDATE JOIN 構文は不使用）。
     *
     * @return 更新件数
     */
    @Update(sqlFile = true)
    int updateDerivedValuesFromAnalysisResult();
}
