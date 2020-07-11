package github.com.ioridazo.fundanalyzer.domain.dao.master;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CashFlowStatementSubjectDao {

    private final JdbcTemplate jdbc;

    public CashFlowStatementSubjectDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
}
