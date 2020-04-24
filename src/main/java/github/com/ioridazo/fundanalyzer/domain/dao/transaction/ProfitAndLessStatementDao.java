package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfitAndLessStatementDao {

    private final JdbcTemplate jdbc;

    public ProfitAndLessStatementDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
}
