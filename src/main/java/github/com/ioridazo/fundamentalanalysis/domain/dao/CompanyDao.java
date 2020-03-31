package github.com.ioridazo.fundamentalanalysis.domain.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyDao {

    private final JdbcTemplate jdbc;

    public CompanyDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
}
