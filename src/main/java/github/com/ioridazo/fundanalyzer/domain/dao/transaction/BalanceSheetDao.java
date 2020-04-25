package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.BalanceSheet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BalanceSheetDao {

    private final JdbcTemplate jdbc;

    public BalanceSheetDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(final BalanceSheet balanceSheet) {
        jdbc.update(
                "INSERT INTO balance_sheet (company_id, financial_statement_id, detail_id, period, value) VALUES (?,?,?,?,?)",
                balanceSheet.getCompanyCode(),
                balanceSheet.getFinancialStatementId(),
                balanceSheet.getDetailId(),
                balanceSheet.getPeriod(),
                balanceSheet.getValue()
        );
    }
}
