package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.ValuationDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.Consolidated;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.ListCategories;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.service.InvestmentIndicatorReconciliationService;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.seasar.doma.jdbc.Sql;
import org.seasar.doma.jdbc.SqlLogType;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValuationSpecificationTest {

    private static ValuationEntity valuationEntity(final LocalDate targetDate, final LocalDate submitDate) {
        return new ValuationEntity(
                null,
                "code",
                submitDate,
                targetDate,
                null,
                BigDecimal.TEN,
                null,
                BigDecimal.TEN,
                (long) 10,
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.TEN,
                null,
                null
        );
    }

    private ValuationDao valuationDao;
    private CompanySpecification companySpecification;
    private StockSpecification stockSpecification;
    private InvestmentIndicatorReconciliationService investmentIndicatorReconciliationService;
    private CorporateActionSpecification corporateActionSpecification;

    private ValuationSpecification valuationSpecification;

    @BeforeEach
    void setUp() {
        valuationDao = mock(ValuationDao.class);
        companySpecification = mock(CompanySpecification.class);
        stockSpecification = mock(StockSpecification.class);
        investmentIndicatorReconciliationService = mock(InvestmentIndicatorReconciliationService.class);
        corporateActionSpecification = mock(CorporateActionSpecification.class);
        when(corporateActionSpecification.adjustToBasis(any(), any(), any(), any(), eq(true)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(investmentIndicatorReconciliationService.reconcile(any(), anyList(), anyList()))
                .thenReturn(List.of());

        valuationSpecification = new ValuationSpecification(
                valuationDao,
                companySpecification,
                stockSpecification,
                investmentIndicatorReconciliationService,
                corporateActionSpecification
        );
    }

    @Nested
    @DisplayName("係数一括再計算バッチ向けメソッドのテスト")
    class Recalculation {

        @DisplayName("countAll : DAO の件数をそのまま返却する")
        @Test
        void countAll() {
            when(valuationDao.countAll()).thenReturn(42);

            assertEquals(42, valuationSpecification.countAll());
        }

        @DisplayName("findAllValuationEntities : DAO の結果をそのまま返却する")
        @Test
        void findAllValuationEntities() {
            final ValuationEntity entity = valuationEntity(LocalDate.parse("2022-07-09"), LocalDate.parse("2022-07-01"));
            when(valuationDao.selectAll()).thenReturn(List.of(entity));

            final List<ValuationEntity> actual = valuationSpecification.findAllValuationEntities();

            assertEquals(1, actual.size());
            assertEquals(entity, actual.get(0));
        }

        @DisplayName("updateDerivedValuesFromAnalysisResult : DAO の更新件数をそのまま返却する")
        @Test
        void updateDerivedValuesFromAnalysisResult() {
            when(valuationDao.updateDerivedValuesFromAnalysisResult()).thenReturn(5);

            assertEquals(5, valuationSpecification.updateDerivedValuesFromAnalysisResult());
            verify(valuationDao, times(1)).updateDerivedValuesFromAnalysisResult();
        }
    }

    @Nested
    class findValuationView {

        @DisplayName("findAllValuationView : 企業ごとの評価結果を取得する")
        @Test
        void get() {
            var code = "code";
            when(valuationDao.selectByCode(code)).thenReturn(List.of(
                    valuationEntity(LocalDate.parse("2022-07-09"), LocalDate.parse("2022-07-01")),
                    valuationEntity(LocalDate.parse("2022-07-10"), LocalDate.parse("2022-07-01")),
                    valuationEntity(LocalDate.parse("2022-08-11"), LocalDate.parse("2021-08-11")),
                    valuationEntity(LocalDate.parse("2022-08-11"), LocalDate.parse("2022-08-11"))
            ));

            var actual = valuationSpecification.findValuation(code);

            assertAll(
                    () -> assertEquals(LocalDate.parse("2022-07-09"), actual.get(0).getTargetDate()),
                    () -> assertEquals(LocalDate.parse("2022-07-01"), actual.get(0).getSubmitDate()),
                    () -> assertEquals(LocalDate.parse("2022-07-10"), actual.get(1).getTargetDate()),
                    () -> assertEquals(LocalDate.parse("2022-07-01"), actual.get(1).getSubmitDate()),
                    () -> assertEquals(LocalDate.parse("2022-08-11"), actual.get(2).getTargetDate()),
                    () -> assertEquals(LocalDate.parse("2022-08-11"), actual.get(2).getSubmitDate())
            );
            assertEquals(3, actual.size());
        }
    }

    @Nested
    class evaluate {

        private final String companyCode = "code";
        private final LocalDate submitDate = LocalDate.parse("2022-06-12");
        private final LocalDate targetDate = LocalDate.parse("2022-07-02");

        @DisplayName("evaluate : マッピングを確認する")
        @Test
        void mapping() {
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, 600.0)));

            final ValuationEntity actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, 500.0),
                    new AnalysisResultEntity(
                            4,
                            companyCode,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            null,
                            submitDate,
                            "documentId",
                            null
                    ));

            assertAll(
                    () -> assertEquals("code", actual.getCompanyCode(), "companyCode"),
                    () -> assertEquals(LocalDate.parse("2022-06-12"), actual.getSubmitDate(), "submitDate"),
                    () -> assertEquals(LocalDate.parse("2022-07-02"), actual.getTargetDate(), "targetDate"),
                    () -> assertEquals(1, actual.getStockPriceId().orElse(null), "stockPriceId"),
                    () -> assertEquals(BigDecimal.valueOf(500.0), actual.getStockPrice(), "stockPrice"),
                    // investment_indicator への書き込みは停止しているため、紐づく ID は常に null を保存する
                    () -> assertNull(actual.getInvestmentIndicatorId().orElse(null), "investmentIndicatorId"),
                    () -> assertNull(actual.getGrahamIndex().orElse(null), "grahamIndex"),
                    () -> assertEquals(20, actual.getDaySinceSubmitDate(), "daySinceSubmitDate"),
                    () -> assertEquals(BigDecimal.valueOf(-100.0), actual.getDifferenceFromSubmitDate(), "differenceFromSubmitDate"),
                    () -> assertEquals(BigDecimal.valueOf(0.83), actual.getSubmitDateRatio(), "submitDateRatio"),
                    () -> assertEquals(BigDecimal.valueOf(1500.0), actual.getDiscountValue(), "discountValue"),
                    () -> assertEquals(BigDecimal.valueOf(400, 2), actual.getDiscountRate(), "discountRate")
            );
        }

        @DisplayName("evaluate : ドメインサービスが返した投資指標のグレアム指数を保存する")
        @Test
        void usesGrahamIndexFromReconciliationService() {
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, 600.0)));
            final AnalysisResultEntity analysisResultEntity = new AnalysisResultEntity(
                    4, companyCode, null, BigDecimal.valueOf(2000),
                    null, null, null,
                    submitDate, "documentId", null
            );
            final StockPriceEntity targetStock = stockPrice(targetDate, 500.0);
            final IndicatorValue indicatorValue = new IndicatorValue(
                    BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(6.5), targetDate);
            when(investmentIndicatorReconciliationService.reconcile(
                    companyCode, List.of(targetStock), List.of(analysisResultEntity)))
                    .thenReturn(List.of(indicatorValue));

            final ValuationEntity actual = valuationSpecification.evaluate(targetStock, analysisResultEntity);

            assertEquals(0, BigDecimal.valueOf(6.5).compareTo(actual.getGrahamIndex().orElseThrow()));
        }

        @DisplayName("evaluate : 補正後株価で割安度を計算する")
        @Test
        void usesAdjustedStockPrice() {
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, 600.0)));
            when(corporateActionSpecification.adjustToBasis(
                    any(), eq(companyCode), eq(targetDate), eq(submitDate), eq(true)))
                    .thenReturn(BigDecimal.valueOf(1000.0));

            final ValuationEntity actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, 500.0),
                    new AnalysisResultEntity(
                            4,
                            companyCode,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            null,
                            submitDate,
                            "documentId",
                            null
                    ));

            assertAll(
                    () -> assertEquals(0, BigDecimal.valueOf(1000.0).compareTo(actual.getStockPrice())),
                    () -> assertEquals(0, BigDecimal.valueOf(400.0).compareTo(actual.getDifferenceFromSubmitDate())),
                    () -> assertEquals(0, BigDecimal.valueOf(167, 2).compareTo(actual.getSubmitDateRatio())),
                    () -> assertEquals(0, BigDecimal.valueOf(1000.0).compareTo(actual.getDiscountValue())),
                    () -> assertEquals(0, BigDecimal.valueOf(200, 2).compareTo(actual.getDiscountRate()))
            );
        }

        @DisplayName("evaluate : daySinceSubmitDateを確認する")
        @ParameterizedTest
        @CsvSource({
                "2022-07-03, 2022-07-02, 1",
                "2022-07-02, 2022-07-02, 0",
                "2022-07-01, 2022-07-02, -1",
        })
        void daySinceSubmitDate(String targetDate, String submitDate, String day) {
            when(stockSpecification.findStock(companyCode, LocalDate.parse(submitDate)))
                    .thenReturn(Optional.of(stockPrice(LocalDate.parse(submitDate), 600.0)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(LocalDate.parse(targetDate), 500.0),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            null,
                            LocalDate.parse(submitDate),
                            null,
                            null
                    )
            );

            assertEquals(Long.valueOf(day), actual.getDaySinceSubmitDate());
        }

        @DisplayName("evaluate : differenceFromSubmitDateを確認する")
        @ParameterizedTest
        @CsvSource({
                "100.0, 200.0, -100",
                "200.0, 200.0, 0",
                "300.0, 200.0, 100",
                "100.0, 100.5, -0.5",
        })
        void differenceFromSubmitDate(Double stockPrice, Double stockPriceOfSubmitDate, Double difference) {
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, stockPriceOfSubmitDate)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, stockPrice),
                    new AnalysisResultEntity(
                            null,
                            companyCode,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            null,
                            LocalDate.parse("2022-06-12"),
                            null,
                            null
                    )
            );

            assertEquals(BigDecimal.valueOf(difference), actual.getDifferenceFromSubmitDate());
        }

        @DisplayName("evaluate : submitDateRatioを確認する")
        @ParameterizedTest
        @CsvSource({
                "100.0, 200.0, 50",
                "200.0, 200.0, 100",
                "300.0, 200.0, 150",
                "201.0, 120.5, 167",
        })
        void submitDateRatio(Double stockPrice, Double stockPriceOfSubmitDate, Long submitDateRatio) {
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, stockPriceOfSubmitDate)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, stockPrice),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(2000),
                            null,
                            null,
                            null,
                            LocalDate.parse("2022-06-12"),
                            null,
                            null
                    )
            );

            assertEquals(BigDecimal.valueOf(submitDateRatio, 2), actual.getSubmitDateRatio());
        }

        @DisplayName("evaluate : discountValueを確認する")
        @ParameterizedTest
        @CsvSource({
                "100.0, 200.0, 100.0",
                "200.0, 200.0, 0.0",
                "300.0, 200.0, -100.0",
                "201.0, 120.5, -80.5",
        })
        void discountValue(Double stockPrice, Double latestCorporateValue, Double discountValue) {
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, 600.0)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, stockPrice),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(latestCorporateValue),
                            null,
                            null,
                            null,
                            LocalDate.parse("2022-06-12"),
                            null,
                            null
                    )
            );

            assertEquals(BigDecimal.valueOf(discountValue), actual.getDiscountValue());
        }

        @DisplayName("evaluate : discountRateを確認する")
        @ParameterizedTest
        @CsvSource({
                "100.0, 200.0, 200",
                "200.0, 200.0, 100",
                "300.0, 200.0, 67",
                "201.0, 120.5, 60",
        })
        void discountRate(Double stockPrice, Double latestCorporateValue, Long discountRate) {
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(stockPrice(submitDate, 600.0)));

            var actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, stockPrice),
                    new AnalysisResultEntity(
                            null,
                            null,
                            null,
                            BigDecimal.valueOf(latestCorporateValue),
                            null,
                            null,
                            null,
                            LocalDate.parse("2022-06-12"),
                            null,
                            null
                    )
            );

            assertEquals(BigDecimal.valueOf(discountRate, 2), actual.getDiscountRate());
        }

        private StockPriceEntity stockPrice(LocalDate targetDate, Double stockPrice) {
            return new StockPriceEntity(
                    1,
                    "code",
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
                    "4.01%",
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    private static ValuationEntity valuationEntityOf(
            final Integer id,
            final String companyCode,
            final LocalDate submitDate,
            final LocalDate targetDate,
            final BigDecimal stockPrice,
            final Long daySinceSubmitDate) {
        return new ValuationEntity(
                id,
                companyCode,
                submitDate,
                targetDate,
                null,
                stockPrice,
                null,
                null,
                daySinceSubmitDate,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Nested
    @DisplayName("findLatestValuation(String, LocalDate) のテスト")
    class FindLatestValuationByCodeAndSubmitDate {

        private final String companyCode = "1234";
        private final LocalDate submitDate = LocalDate.parse("2022-06-12");

        @DisplayName("データが存在する場合 → targetDate が最大の評価結果を返す")
        @Test
        void returnsLatestByTargetDate() {
            final ValuationEntity older = valuationEntityOf(
                    1, companyCode, submitDate, LocalDate.parse("2022-06-13"), BigDecimal.valueOf(100), 1L);
            final ValuationEntity newer = valuationEntityOf(
                    2, companyCode, submitDate, LocalDate.parse("2022-08-20"), BigDecimal.valueOf(200), 69L);
            final ValuationEntity middle = valuationEntityOf(
                    3, companyCode, submitDate, LocalDate.parse("2022-07-15"), BigDecimal.valueOf(150), 33L);
            when(valuationDao.selectByCodeAndSubmitDate(companyCode, submitDate))
                    .thenReturn(List.of(older, newer, middle));

            final Optional<ValuationEntity> actual =
                    valuationSpecification.findLatestValuation(companyCode, submitDate);

            assertAll(
                    () -> assertTrue(actual.isPresent()),
                    () -> assertEquals(2, actual.orElseThrow().getId()),
                    () -> assertEquals(LocalDate.parse("2022-08-20"), actual.orElseThrow().getTargetDate())
            );
        }

        @DisplayName("データが存在しない場合 → 空 Optional を返す")
        @Test
        void returnsEmptyWhenNoData() {
            when(valuationDao.selectByCodeAndSubmitDate(companyCode, submitDate))
                    .thenReturn(List.of());

            final Optional<ValuationEntity> actual =
                    valuationSpecification.findLatestValuation(companyCode, submitDate);

            assertFalse(actual.isPresent());
        }
    }

    @Nested
    @DisplayName("findValuationOfSubmitDate メソッド")
    class FindValuationOfSubmitDate {

        private final String companyCode = "1234";
        private final LocalDate submitDate = LocalDate.parse("2022-06-12");

        @DisplayName("データが存在する場合 → daySinceSubmitDate が最小の評価結果を返す")
        @Test
        void returnsMinDaySinceSubmitDate() {
            final ValuationEntity day0 = valuationEntityOf(
                    1, companyCode, submitDate, LocalDate.parse("2022-06-12"), BigDecimal.valueOf(100), 0L);
            final ValuationEntity day10 = valuationEntityOf(
                    2, companyCode, submitDate, LocalDate.parse("2022-06-22"), BigDecimal.valueOf(120), 10L);
            final ValuationEntity day3 = valuationEntityOf(
                    3, companyCode, submitDate, LocalDate.parse("2022-06-15"), BigDecimal.valueOf(110), 3L);
            when(valuationDao.selectByCodeAndSubmitDate(companyCode, submitDate))
                    .thenReturn(List.of(day10, day0, day3));

            final Optional<ValuationEntity> actual =
                    valuationSpecification.findValuationOfSubmitDate(companyCode, submitDate);

            assertAll(
                    () -> assertTrue(actual.isPresent()),
                    () -> assertEquals(1, actual.orElseThrow().getId()),
                    () -> assertEquals(0L, actual.orElseThrow().getDaySinceSubmitDate())
            );
        }

        @DisplayName("データが存在しない場合 → 空 Optional を返す")
        @Test
        void returnsEmptyWhenNoData() {
            when(valuationDao.selectByCodeAndSubmitDate(companyCode, submitDate))
                    .thenReturn(List.of());

            final Optional<ValuationEntity> actual =
                    valuationSpecification.findValuationOfSubmitDate(companyCode, submitDate);

            assertFalse(actual.isPresent());
        }
    }

    @Nested
    @DisplayName("findLatestValuation(String) メソッド")
    class FindLatestValuationByCode {

        private final String companyCode = "1234";

        @DisplayName("複数提出日が混在する場合 → 最新提出日かつ最新対象日のレコードを返す")
        @Test
        void returnsLatestSubmitDateAndTargetDate() {
            final ValuationEntity oldSubmit1 = valuationEntityOf(
                    1, companyCode, LocalDate.parse("2021-06-12"), LocalDate.parse("2021-06-12"), BigDecimal.valueOf(100), 0L);
            final ValuationEntity oldSubmit2 = valuationEntityOf(
                    2, companyCode, LocalDate.parse("2021-06-12"), LocalDate.parse("2021-12-31"), BigDecimal.valueOf(110), 200L);
            final ValuationEntity latestSubmit1 = valuationEntityOf(
                    3, companyCode, LocalDate.parse("2022-06-12"), LocalDate.parse("2022-06-12"), BigDecimal.valueOf(200), 0L);
            final ValuationEntity latestSubmitTargetMax = valuationEntityOf(
                    4, companyCode, LocalDate.parse("2022-06-12"), LocalDate.parse("2022-09-30"), BigDecimal.valueOf(220), 110L);
            when(valuationDao.selectByCode(companyCode))
                    .thenReturn(List.of(oldSubmit1, oldSubmit2, latestSubmit1, latestSubmitTargetMax));

            final Optional<ValuationEntity> actual =
                    valuationSpecification.findLatestValuation(companyCode);

            assertAll(
                    () -> assertTrue(actual.isPresent()),
                    () -> assertEquals(4, actual.orElseThrow().getId()),
                    () -> assertEquals(LocalDate.parse("2022-06-12"), actual.orElseThrow().getSubmitDate()),
                    () -> assertEquals(LocalDate.parse("2022-09-30"), actual.orElseThrow().getTargetDate())
            );
        }

        @DisplayName("データが存在しない場合 → 空 Optional を返す")
        @Test
        void returnsEmptyWhenNoData() {
            when(valuationDao.selectByCode(companyCode)).thenReturn(List.of());

            final Optional<ValuationEntity> actual =
                    valuationSpecification.findLatestValuation(companyCode);

            assertFalse(actual.isPresent());
        }
    }

    @Nested
    @DisplayName("insert メソッド")
    class Insert {

        private final String companyCode = "code";
        private final LocalDate submitDate = LocalDate.parse("2022-06-12");
        private final LocalDate targetDate = LocalDate.parse("2022-07-02");

        private StockPriceEntity stockPrice(final Double price) {
            return new StockPriceEntity(
                    1,
                    companyCode,
                    targetDate,
                    price,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null
            );
        }

        private AnalysisResultEntity analysisResult(final BigDecimal corporateValue) {
            return new AnalysisResultEntity(
                    4,
                    companyCode,
                    null,
                    corporateValue,
                    null,
                    null,
                    null,
                    submitDate,
                    "documentId",
                    null
            );
        }

        @BeforeEach
        void setUp() {
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.of(new StockPriceEntity(
                            9, companyCode, submitDate, 600.0,
                            null, null, null, null,
                            null, null, null, null,
                            null, null, null, null,
                            null, null
                    )));
        }

        @DisplayName("正常系 → ValuationDao.insert が呼び出され例外が発生しない")
        @Test
        void normal() {
            assertDoesNotThrow(() -> valuationSpecification.insert(
                    stockPrice(500.0), analysisResult(BigDecimal.valueOf(2000))));
        }

        @DisplayName("UniqueConstraintException が発生した場合 → 例外を握りつぶしてスキップする")
        @Test
        void uniqueConstraintException() {
            final DataIntegrityViolationException wrapper =
                    new DataIntegrityViolationException(
                            "duplicate",
                            new UniqueConstraintException(SqlLogType.FORMATTED, Mockito.mock(Sql.class), null));
            doThrow(wrapper).when(valuationDao).insert(any(ValuationEntity.class));
            when(companySpecification.findCompanyByCode(companyCode))
                    .thenReturn(Optional.of(new Company(
                            companyCode,
                            "テスト株式会社",
                            1,
                            "業種",
                            "E12345",
                            ListCategories.LISTED,
                            Consolidated.CONSOLIDATED,
                            10000,
                            "0331",
                            false,
                            false,
                            true
                    )));

            assertDoesNotThrow(() -> valuationSpecification.insert(
                    stockPrice(500.0), analysisResult(BigDecimal.valueOf(2000))));
        }

        @DisplayName("UniqueConstraintException かつ企業情報が見つからない場合 → 例外を握りつぶしてスキップする")
        @Test
        void uniqueConstraintExceptionWithoutCompany() {
            final DataIntegrityViolationException wrapper =
                    new DataIntegrityViolationException(
                            "duplicate",
                            new UniqueConstraintException(SqlLogType.FORMATTED, Mockito.mock(Sql.class), null));
            doThrow(wrapper).when(valuationDao).insert(any(ValuationEntity.class));
            when(companySpecification.findCompanyByCode(companyCode))
                    .thenReturn(Optional.empty());

            assertDoesNotThrow(() -> valuationSpecification.insert(
                    stockPrice(500.0), analysisResult(BigDecimal.valueOf(2000))));
        }

        @DisplayName("SQLIntegrityConstraintViolationException が発生した場合 → 例外を握りつぶしてスキップする")
        @Test
        void integrityConstraintException() {
            final DataIntegrityViolationException wrapper =
                    new DataIntegrityViolationException(
                            "fk-violation",
                            new SQLIntegrityConstraintViolationException("FK violation"));
            doThrow(wrapper).when(valuationDao).insert(any(ValuationEntity.class));
            when(companySpecification.findCompanyByCode(companyCode))
                    .thenReturn(Optional.of(new Company(
                            companyCode,
                            "テスト株式会社",
                            1,
                            "業種",
                            "E12345",
                            ListCategories.LISTED,
                            Consolidated.CONSOLIDATED,
                            10000,
                            "0331",
                            false,
                            false,
                            true
                    )));

            assertDoesNotThrow(() -> valuationSpecification.insert(
                    stockPrice(500.0), analysisResult(BigDecimal.valueOf(2000))));
        }

        @DisplayName("ハンドリング対象外の NestedRuntimeException が発生した場合 → 例外を再スローする")
        @Test
        void otherNestedRuntimeExceptionRethrown() {
            final DataIntegrityViolationException wrapper =
                    new DataIntegrityViolationException("unknown", new IllegalStateException("boom"));
            doThrow(wrapper).when(valuationDao).insert(any(ValuationEntity.class));

            assertThrows(DataIntegrityViolationException.class, () -> valuationSpecification.insert(
                    stockPrice(500.0), analysisResult(BigDecimal.valueOf(2000))));
        }
    }

    @Nested
    @DisplayName("evaluate メソッドにおける提出日株価取得の分岐")
    class EvaluateStockPriceOfSubmitDate {

        private final String companyCode = "code";
        private final LocalDate submitDate = LocalDate.parse("2022-06-12");
        private final LocalDate targetDate = LocalDate.parse("2022-07-02");

        private StockPriceEntity stockPrice(final LocalDate target, final Double price) {
            return new StockPriceEntity(
                    1,
                    companyCode,
                    target,
                    price,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null
            );
        }

        private AnalysisResultEntity analysisResult() {
            return new AnalysisResultEntity(
                    4, companyCode, null, BigDecimal.valueOf(2000),
                    null, null, null,
                    submitDate, "documentId", null
            );
        }

        @DisplayName("過去valuationが存在する → そこから提出日株価を取得する")
        @Test
        void fromExistingValuation() {
            final ValuationEntity existing = new ValuationEntity(
                    1,
                    companyCode,
                    submitDate,
                    submitDate,
                    null,
                    BigDecimal.valueOf(800.0),
                    null,
                    null,
                    0L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDateTime.now()
            );
            when(valuationDao.selectByCodeAndSubmitDate(companyCode, submitDate))
                    .thenReturn(List.of(existing));

            final ValuationEntity actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, 500.0), analysisResult());

            assertAll(
                    () -> assertEquals(BigDecimal.valueOf(-300.0), actual.getDifferenceFromSubmitDate()),
                    () -> assertEquals(BigDecimal.valueOf(63, 2), actual.getSubmitDateRatio())
            );
        }

        @DisplayName("過去valuationなし・findStockなし・getAverageStockPriceあり → 平均株価から取得する")
        @Test
        void fromAverageStockPrice() {
            when(valuationDao.selectByCodeAndSubmitDate(companyCode, submitDate))
                    .thenReturn(List.of());
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.empty());
            when(stockSpecification.getAverageStockPrice(companyCode, submitDate))
                    .thenReturn(Optional.of(BigDecimal.valueOf(400.0)));

            final ValuationEntity actual = valuationSpecification.evaluate(
                    stockPrice(targetDate, 500.0), analysisResult());

            assertAll(
                    () -> assertEquals(BigDecimal.valueOf(100.0), actual.getDifferenceFromSubmitDate()),
                    () -> assertEquals(BigDecimal.valueOf(125, 2), actual.getSubmitDateRatio())
            );
        }

        @DisplayName("提出日株価がいずれの経路でも取得できない場合 → FundanalyzerNotExistException を投げる")
        @Test
        void throwsWhenNoStockPriceAvailable() {
            when(valuationDao.selectByCodeAndSubmitDate(companyCode, submitDate))
                    .thenReturn(List.of());
            when(stockSpecification.findStock(companyCode, submitDate))
                    .thenReturn(Optional.empty());
            when(stockSpecification.getAverageStockPrice(companyCode, submitDate))
                    .thenReturn(Optional.empty());

            assertThrows(FundanalyzerNotExistException.class,
                    () -> valuationSpecification.evaluate(
                            stockPrice(targetDate, 500.0), analysisResult()));
        }
    }
}
