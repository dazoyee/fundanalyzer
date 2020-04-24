package github.com.ioridazo.fundamentalanalysis.domain.dao.master;

import github.com.ioridazo.fundamentalanalysis.domain.entity.master.Company;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class CompanyDao {

    private final JdbcTemplate jdbc;

    public CompanyDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Company company) {
        jdbc.update(
                "INSERT INTO company VALUES (?,?,?,?,?,?,?,?,?,?)",
                company.getCode(),
                company.getCompanyName(),
                company.getIndustryId(),
                company.getEdinetCode(),
                company.getListCategories().toValue(),
                company.getConsolidated().toValue(),
                company.getCapitalStock(),
                company.getSettlementDate(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
