package github.com.ioridazo.fundanalyzer.domain.dao.master;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FinancialStatementDao {

    private final JdbcTemplate jdbc;

    public FinancialStatementDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
}
