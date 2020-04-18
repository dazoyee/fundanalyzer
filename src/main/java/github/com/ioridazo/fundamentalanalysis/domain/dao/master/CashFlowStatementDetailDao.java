package github.com.ioridazo.fundamentalanalysis.domain.dao.master;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CashFlowStatementDetailDao {

    private final JdbcTemplate jdbc;

    public CashFlowStatementDetailDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
}
