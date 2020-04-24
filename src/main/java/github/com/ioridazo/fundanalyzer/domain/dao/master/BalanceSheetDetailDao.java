package github.com.ioridazo.fundanalyzer.domain.dao.master;

import github.com.ioridazo.fundanalyzer.domain.entity.master.BalanceSheetDetail;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BalanceSheetDetailDao {

    private final JdbcTemplate jdbc;

    public BalanceSheetDetailDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<BalanceSheetDetail> findAll() {
        return jdbc.query(
                "SELECT * FROM balance_sheet_detail",
                new BeanPropertyRowMapper<>(BalanceSheetDetail.class)
        );
    }
}
