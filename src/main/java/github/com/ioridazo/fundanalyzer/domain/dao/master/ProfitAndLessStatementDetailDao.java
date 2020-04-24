package github.com.ioridazo.fundanalyzer.domain.dao.master;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfitAndLessStatementDetailDao {

    private final JdbcTemplate jdbc;

    public ProfitAndLessStatementDetailDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
}
