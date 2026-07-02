package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorporateActionSpecificationのテスト")
class CorporateActionSpecificationTest {

    private static final String COMPANY_CODE = "1234";

    @Mock
    private CompanySpecification companySpecification;

    @Mock
    private FinancialStatementSpecification financialStatementSpecification;

    @Mock
    private StockPriceDao stockPriceDao;

    private CorporateActionSpecification specification;

    @BeforeEach
    void setUp() {
        this.specification = new CorporateActionSpecification(
                companySpecification,
                financialStatementSpecification,
                stockPriceDao,
                0.6,
                0.1
        );
        when(companySpecification.findCompanyByCode(COMPANY_CODE)).thenReturn(Optional.of(company()));
    }

    @Nested
    @DisplayName("findActions メソッド")
    class FindActions {

        @Test
        @DisplayName("分割5倍を確定検知できる→確定アクションを返す")
        void shouldDetectConfirmedSplit() {
            when(financialStatementSpecification.findByCompany(any()))
                    .thenReturn(List.of(
                            statement(LocalDate.of(2023, 3, 31), 100L, LocalDate.of(2023, 5, 1)),
                            statement(LocalDate.of(2023, 6, 30), 500L, LocalDate.of(2023, 8, 1))
                    ));
            when(stockPriceDao.selectByCode(COMPANY_CODE))
                    .thenReturn(List.of(
                            price(LocalDate.of(2023, 7, 31), 1000.0),
                            price(LocalDate.of(2023, 8, 1), 200.0)
                    ));

            final List<CorporateActionSpecification.CorporateAction> actions = specification.findActions(COMPANY_CODE);

            assertEquals(1, actions.size());
            assertEquals(0, new BigDecimal("5.0").compareTo(actions.get(0).ratio()));
            assertEquals(LocalDate.of(2023, 8, 1), actions.get(0).effectiveDate());
            assertTrue(actions.get(0).confirmed());
        }

        @Test
        @DisplayName("併合0.2倍を確定検知できる→確定アクションを返す")
        void shouldDetectConfirmedReverseSplit() {
            when(financialStatementSpecification.findByCompany(any()))
                    .thenReturn(List.of(
                            statement(LocalDate.of(2023, 3, 31), 500L, LocalDate.of(2023, 5, 1)),
                            statement(LocalDate.of(2023, 6, 30), 100L, LocalDate.of(2023, 8, 1))
                    ));
            when(stockPriceDao.selectByCode(COMPANY_CODE))
                    .thenReturn(List.of(
                            price(LocalDate.of(2023, 7, 31), 200.0),
                            price(LocalDate.of(2023, 8, 1), 1000.0)
                    ));

            final List<CorporateActionSpecification.CorporateAction> actions = specification.findActions(COMPANY_CODE);

            assertEquals(1, actions.size());
            assertEquals(0, new BigDecimal("0.2").compareTo(actions.get(0).ratio()));
            assertTrue(actions.get(0).confirmed());
        }

        @Test
        @DisplayName("株式数変化のみで価格クリフ無し→増資とみなし検知しない")
        void shouldIgnoreShareIncreaseWithoutCliff() {
            when(financialStatementSpecification.findByCompany(any()))
                    .thenReturn(List.of(
                            statement(LocalDate.of(2023, 3, 31), 100L, LocalDate.of(2023, 5, 1)),
                            statement(LocalDate.of(2023, 6, 30), 200L, LocalDate.of(2023, 8, 1))
                    ));
            when(stockPriceDao.selectByCode(COMPANY_CODE))
                    .thenReturn(List.of(
                            price(LocalDate.of(2023, 7, 31), 1000.0),
                            price(LocalDate.of(2023, 8, 1), 980.0)
                    ));

            final List<CorporateActionSpecification.CorporateAction> actions = specification.findActions(COMPANY_CODE);

            assertTrue(actions.isEmpty());
        }

        @Test
        @DisplayName("価格クリフのみ有報未更新→暫定アクションを返す")
        void shouldDetectProvisionalActionFromCliffOnly() {
            when(financialStatementSpecification.findByCompany(any())).thenReturn(List.of());
            when(stockPriceDao.selectByCode(COMPANY_CODE))
                    .thenReturn(List.of(
                            price(LocalDate.of(2023, 7, 31), 1000.0),
                            price(LocalDate.of(2023, 8, 1), 200.0)
                    ));

            final List<CorporateActionSpecification.CorporateAction> actions = specification.findActions(COMPANY_CODE);

            assertEquals(1, actions.size());
            assertEquals(0, new BigDecimal("5.0").compareTo(actions.get(0).ratio()));
            assertFalse(actions.get(0).confirmed());
        }

        @Test
        @DisplayName("暫定検知は有報データが揃うと確定に昇格する")
        void shouldUpgradeProvisionalActionToConfirmed() {
            when(stockPriceDao.selectByCode(COMPANY_CODE))
                    .thenReturn(List.of(
                            price(LocalDate.of(2023, 7, 31), 1000.0),
                            price(LocalDate.of(2023, 8, 1), 200.0)
                    ));
            when(financialStatementSpecification.findByCompany(any())).thenReturn(List.of());

            final List<CorporateActionSpecification.CorporateAction> provisional = specification.findActions(COMPANY_CODE);

            assertEquals(1, provisional.size());
            assertFalse(provisional.get(0).confirmed());

            when(financialStatementSpecification.findByCompany(any()))
                    .thenReturn(List.of(
                            statement(LocalDate.of(2023, 3, 31), 100L, LocalDate.of(2023, 5, 1)),
                            statement(LocalDate.of(2023, 6, 30), 500L, LocalDate.of(2023, 8, 1))
                    ));

            final List<CorporateActionSpecification.CorporateAction> confirmed = specification.findActions(COMPANY_CODE);

            assertEquals(1, confirmed.size());
            assertTrue(confirmed.get(0).confirmed());
            assertEquals(0, new BigDecimal("5.0").compareTo(confirmed.get(0).ratio()));
        }
    }

    @Nested
    @DisplayName("sharesFactorAt メソッド")
    class SharesFactorAt {

        @Test
        @DisplayName("複数アクションの累積係数を返す")
        void shouldReturnCumulativeFactor() {
            when(financialStatementSpecification.findByCompany(any()))
                    .thenReturn(List.of(
                            statement(LocalDate.of(2023, 3, 31), 100L, LocalDate.of(2023, 5, 1)),
                            statement(LocalDate.of(2023, 6, 30), 200L, LocalDate.of(2023, 8, 1)),
                            statement(LocalDate.of(2023, 9, 30), 1000L, LocalDate.of(2023, 11, 1))
                    ));
            when(stockPriceDao.selectByCode(COMPANY_CODE))
                    .thenReturn(List.of(
                            price(LocalDate.of(2023, 7, 31), 1000.0),
                            price(LocalDate.of(2023, 8, 1), 500.0),
                            price(LocalDate.of(2023, 10, 31), 1200.0),
                            price(LocalDate.of(2023, 11, 1), 240.0)
                    ));

            final BigDecimal factor = specification.sharesFactorAt(COMPANY_CODE, LocalDate.of(2023, 11, 1));

            assertEquals(0, new BigDecimal("10.0").compareTo(factor));
        }

        @Test
        @DisplayName("アクションが無ければ1を返す")
        void shouldReturnOneWhenNoActionExists() {
            when(financialStatementSpecification.findByCompany(any())).thenReturn(List.of());
            when(stockPriceDao.selectByCode(COMPANY_CODE)).thenReturn(List.of());

            final BigDecimal factor = specification.sharesFactorAt(COMPANY_CODE, LocalDate.of(2023, 11, 1));

            assertEquals(0, BigDecimal.ONE.compareTo(factor));
        }
    }

    @Nested
    @DisplayName("adjustToBasis メソッド")
    class AdjustToBasis {

        @Test
        @DisplayName("分割後価格を旧株（基準日）基準へ補正できる")
        void shouldAdjustPriceToBasisDate() {
            when(financialStatementSpecification.findByCompany(any()))
                    .thenReturn(List.of(
                            statement(LocalDate.of(2023, 3, 31), 100L, LocalDate.of(2023, 5, 1)),
                            statement(LocalDate.of(2023, 6, 30), 500L, LocalDate.of(2023, 8, 1))
                    ));
            when(stockPriceDao.selectByCode(COMPANY_CODE))
                    .thenReturn(List.of(
                            price(LocalDate.of(2023, 7, 31), 1000.0),
                            price(LocalDate.of(2023, 8, 1), 200.0)
                    ));

            final BigDecimal adjusted = specification.adjustToBasis(
                    new BigDecimal("1003"),
                    COMPANY_CODE,
                    LocalDate.of(2023, 8, 2),
                    LocalDate.of(2023, 7, 31)
            );

            assertEquals(0, new BigDecimal("5015").compareTo(adjusted));
        }

        @Test
        @DisplayName("confirmedOnly=trueのとき未確定アクションを除外する")
        void shouldIgnoreUnconfirmedActionsWhenConfirmedOnlyIsTrue() {
            when(financialStatementSpecification.findByCompany(any()))
                    .thenReturn(List.of(
                            statement(LocalDate.of(2023, 3, 31), 100L, LocalDate.of(2023, 5, 1)),
                            statement(LocalDate.of(2023, 6, 30), 500L, LocalDate.of(2023, 8, 1))
                    ));
            when(stockPriceDao.selectByCode(COMPANY_CODE))
                    .thenReturn(List.of(
                            price(LocalDate.of(2023, 7, 31), 1000.0),
                            price(LocalDate.of(2023, 8, 1), 200.0),
                            price(LocalDate.of(2023, 9, 30), 200.0),
                            price(LocalDate.of(2023, 10, 1), 100.0)
                    ));

            final BigDecimal factor = specification.sharesFactorAt(COMPANY_CODE, LocalDate.of(2023, 10, 2), true);
            final BigDecimal adjusted = specification.adjustToBasis(
                    new BigDecimal("100"),
                    COMPANY_CODE,
                    LocalDate.of(2023, 10, 2),
                    LocalDate.of(2023, 7, 31),
                    true
            );

            assertEquals(0, new BigDecimal("5.0").compareTo(factor));
            assertEquals(0, new BigDecimal("500").compareTo(adjusted));
        }
    }

    private Company company() {
        return new Company(
                COMPANY_CODE,
                "テスト企業",
                1,
                "テスト業種",
                "E00001",
                null,
                null,
                100,
                "0331",
                false,
                false,
                true
        );
    }

    private FinancialStatementEntity statement(
            final LocalDate periodEnd,
            final long value,
            final LocalDate submitDate) {
        return new FinancialStatementEntity(
                1,
                COMPANY_CODE,
                "E00001",
                FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.getId(),
                "0",
                periodEnd.minusMonths(3),
                periodEnd,
                value,
                "120",
                null,
                submitDate,
                "DOC-" + submitDate,
                "MANUAL",
                LocalDateTime.of(2023, 1, 1, 0, 0)
        );
    }

    private StockPriceEntity price(final LocalDate targetDate, final double stockPrice) {
        return new StockPriceEntity(
                1,
                COMPANY_CODE,
                targetDate,
                stockPrice,
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
                "TEST",
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 1, 1, 0, 0)
        );
    }
}
