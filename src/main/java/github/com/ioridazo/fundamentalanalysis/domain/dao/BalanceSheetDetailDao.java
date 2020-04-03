package github.com.ioridazo.fundamentalanalysis.domain.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BalanceSheetDetailDao {

    private final JdbcTemplate jdbc;

    public BalanceSheetDetailDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
}
