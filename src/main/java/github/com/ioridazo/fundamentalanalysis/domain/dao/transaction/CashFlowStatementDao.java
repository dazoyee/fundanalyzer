package github.com.ioridazo.fundamentalanalysis.domain.dao.transaction;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CashFlowStatementDao {

    private final JdbcTemplate jdbc;

    public CashFlowStatementDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
}
