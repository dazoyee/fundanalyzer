package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SourceOfStockPrice;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockCompanyStalenessSpecificationTest {

    private static final LocalDate FIXED_NOW = LocalDate.of(2026, 7, 18);

    @Mock
    private CompanySpecification companySpecification;
    @Mock
    private StockSpecification stockSpecification;

    @Nested
    @DisplayName("findStaleCompanies")
    class FindStaleCompanies {

        @Test
        @DisplayName("閾値超過の企業だけを staleDays 降順で返す")
        void returnsOnlyStaleCompanies() {
            final Company stale = company("9278");
            final Company fresh = company("1111");
            final Company noData = company("7034");
            when(companySpecification.inquiryAllTargetCompanies()).thenReturn(List.of(stale, fresh, noData));
            when(stockSpecification.findLatestStock("9278"))
                    .thenReturn(Optional.of(stock("9278", FIXED_NOW.minusDays(20))));
            when(stockSpecification.findLatestStock("1111"))
                    .thenReturn(Optional.of(stock("1111", FIXED_NOW.minusDays(14))));
            when(stockSpecification.findLatestStock("7034"))
                    .thenReturn(Optional.empty());

            final StockCompanyStalenessSpecification specification = newFixedNowSpecification(14);

            final List<StockCompanyStalenessSpecification.StaleCompany> actual = specification.findStaleCompanies();

            assertEquals(List.of("9278", "7034"), actual.stream().map(StockCompanyStalenessSpecification.StaleCompany::code).toList());
            assertEquals(List.of(20L, 15L), actual.stream().map(StockCompanyStalenessSpecification.StaleCompany::staleDays).toList());
        }
    }

    private StockCompanyStalenessSpecification newFixedNowSpecification(final int alertDays) {
        return new StockCompanyStalenessSpecification(companySpecification, stockSpecification, alertDays) {
            @Override
            LocalDate nowLocalDate() {
                return FIXED_NOW;
            }
        };
    }

    private Company company(final String code) {
        return new Company(code, code, 1, "業種", "E" + code, null, null, null, null, false, false, true);
    }

    private StockPriceEntity stock(final String code, final LocalDate targetDate) {
        return new StockPriceEntity(
                1,
                code,
                targetDate,
                1000.0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SourceOfStockPrice.NIKKEI.toValue(),
                LocalDateTime.of(2026, 7, 18, 0, 0),
                LocalDateTime.of(2026, 7, 18, 0, 0)
        );
    }
}
