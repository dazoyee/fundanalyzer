package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StockCompanyStalenessSpecification {

    private final CompanySpecification companySpecification;
    private final StockPriceDao stockPriceDao;
    private final int companyStalenessAlertDays;

    public StockCompanyStalenessSpecification(
            final CompanySpecification companySpecification,
            final StockPriceDao stockPriceDao,
            @Value("${app.config.stock.company-staleness-alert-days}") final int companyStalenessAlertDays) {
        this.companySpecification = companySpecification;
        this.stockPriceDao = stockPriceDao;
        this.companyStalenessAlertDays = companyStalenessAlertDays;
    }

    LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    @Cacheable("staleStockCompanies")
    public List<StaleCompany> findStaleCompanies() {
        final LocalDate today = nowLocalDate();
        final LocalDate thresholdDate = today.minusDays(companyStalenessAlertDays);
        final Map<String, LocalDate> latestTargetDateByCode = stockPriceDao.selectLatestTargetDateAll().stream()
                .collect(Collectors.toMap(
                        latestTargetDate -> latestTargetDate.getCompanyCode(),
                        latestTargetDate -> latestTargetDate.getTargetDate(),
                        (left, right) -> right
                ));

        return companySpecification.inquiryAllTargetCompanies().stream()
                .map(company -> toStaleCompany(company, today, latestTargetDateByCode))
                .filter(staleCompany -> staleCompany.targetDate() == null || staleCompany.targetDate().isBefore(thresholdDate))
                .sorted(Comparator.comparingLong(StaleCompany::staleDays).reversed()
                        .thenComparing(StaleCompany::code))
                .toList();
    }

    private StaleCompany toStaleCompany(
            final Company company,
            final LocalDate today,
            final Map<String, LocalDate> latestTargetDateByCode) {
        return java.util.Optional.ofNullable(latestTargetDateByCode.get(company.code()))
                .map(targetDate -> StaleCompany.of(company.getCode4(), targetDate, today))
                .orElseGet(() -> StaleCompany.withoutStock(company.getCode4(), companyStalenessAlertDays + 1L));
    }

    public record StaleCompany(String code, LocalDate targetDate, long staleDays) {

        static StaleCompany of(final String code, final LocalDate targetDate, final LocalDate today) {
            return new StaleCompany(
                    code,
                    targetDate,
                    ChronoUnit.DAYS.between(targetDate, today)
            );
        }

        static StaleCompany withoutStock(final String code, final long staleDays) {
            return new StaleCompany(code, null, staleDays);
        }
    }
}
