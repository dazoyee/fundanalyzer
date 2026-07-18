package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Component
public class StockCompanyStalenessSpecification {

    private final CompanySpecification companySpecification;
    private final StockSpecification stockSpecification;
    private final int companyStalenessAlertDays;

    public StockCompanyStalenessSpecification(
            final CompanySpecification companySpecification,
            final StockSpecification stockSpecification,
            @Value("${app.config.stock.company-staleness-alert-days}") final int companyStalenessAlertDays) {
        this.companySpecification = companySpecification;
        this.stockSpecification = stockSpecification;
        this.companyStalenessAlertDays = companyStalenessAlertDays;
    }

    LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    public List<StaleCompany> findStaleCompanies() {
        final LocalDate today = nowLocalDate();
        final LocalDate thresholdDate = today.minusDays(companyStalenessAlertDays);

        return companySpecification.inquiryAllTargetCompanies().stream()
                .map(company -> toStaleCompany(company, today))
                .filter(staleCompany -> staleCompany.targetDate() == null || staleCompany.targetDate().isBefore(thresholdDate))
                .sorted(Comparator.comparingLong(StaleCompany::staleDays).reversed()
                        .thenComparing(StaleCompany::code))
                .toList();
    }

    private StaleCompany toStaleCompany(final Company company, final LocalDate today) {
        return stockSpecification.findLatestStock(company.code())
                .map(stock -> StaleCompany.of(company.getCode4(), stock, today))
                .orElseGet(() -> StaleCompany.withoutStock(company.getCode4(), companyStalenessAlertDays + 1L));
    }

    public record StaleCompany(String code, LocalDate targetDate, long staleDays) {

        static StaleCompany of(final String code, final StockPriceEntity stockPriceEntity, final LocalDate today) {
            return new StaleCompany(
                    code,
                    stockPriceEntity.getTargetDate(),
                    ChronoUnit.DAYS.between(stockPriceEntity.getTargetDate(), today)
            );
        }

        static StaleCompany withoutStock(final String code, final long staleDays) {
            return new StaleCompany(code, null, staleDays);
        }
    }
}
