package github.com.ioridazo.fundanalyzer.domain.dao.master;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BalanceSheetSubjectDao {

    private final JdbcTemplate jdbc;

    public BalanceSheetSubjectDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
}
