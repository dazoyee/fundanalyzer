package github.com.ioridazo.fundamentalanalysis.domain.dao.master;

import github.com.ioridazo.fundamentalanalysis.domain.entity.master.Industry;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IndustryDao {

    private final JdbcTemplate jdbc;

    public IndustryDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Industry findByName(String name) {
        return jdbc.queryForObject(
                "SELECT * FROM industry WHERE name = ?",
                new BeanPropertyRowMapper<>(Industry.class),
                name
        );
    }

    public void insert(Industry industry) {
        var count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM industry WHERE name = ?",
                String.class,
                industry.getName());

        if ("0".equals(count)) {
            jdbc.update(
                    "INSERT INTO industry (name) VALUES (?)",
                    industry.getName()
            );
        }
    }
}
