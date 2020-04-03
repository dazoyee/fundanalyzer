package github.com.ioridazo.fundamentalanalysis.domain.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BalanceSheetDao {

    private final JdbcTemplate jdbc;

    public BalanceSheetDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
}
