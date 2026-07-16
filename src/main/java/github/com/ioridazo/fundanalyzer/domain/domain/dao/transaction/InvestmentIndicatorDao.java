package github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.InvestmentIndicatorEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * investment_indicator への書き込みは停止した（読み取り都度計算化のため {@code insert} は撤去済み）。
 * テーブル自体・既存データは残置しているため、参照系メソッドのみ残す。
 */
@ConfigAutowireable
@Dao
public interface InvestmentIndicatorDao {

    @Select
    Optional<InvestmentIndicatorEntity> selectByCodeAndTargetDate(String code, LocalDate targetDate);

    @Select
    List<InvestmentIndicatorEntity> selectByCode(String code);

    @Select
    List<InvestmentIndicatorEntity> selectByAnalysisResultId(Integer analysisResultId);
}
