package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ValuationSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ViewValuationInteractor のテスト")
class ViewValuationInteractorTest {

    private CompanySpecification companySpecification;
    private ValuationSpecification valuationSpecification;
    private ViewSpecification viewSpecification;
    private SlackClient slackClient;

    private ViewValuationInteractor interactor;

    @BeforeEach
    void setUp() {
        companySpecification = mock(CompanySpecification.class);
        valuationSpecification = mock(ValuationSpecification.class);
        viewSpecification = mock(ViewSpecification.class);
        slackClient = mock(SlackClient.class);

        interactor = spy(new ViewValuationInteractor(
                companySpecification,
                valuationSpecification,
                viewSpecification,
                slackClient
        ));
        interactor.updateViewEnabled = true;
        interactor.rankingExcludeDays = 90;
    }

    private ValuationEntity emptyValuation() {
        return emptyValuation(null);
    }

    private ValuationEntity emptyValuation(final Integer id) {
        return new ValuationEntity(
                id, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    private CompanyValuationViewModel valuationView(
            final String code,
            final BigDecimal discountRate,
            final long daySinceSubmitDate) {
        return new CompanyValuationViewModel(
                code,
                "テスト企業",
                LocalDate.parse("2026-07-01"),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(500),
                discountRate,
                LocalDate.parse("2024-03-01"),
                BigDecimal.valueOf(900),
                daySinceSubmitDate,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1.1),
                BigDecimal.valueOf(0.4),
                BigDecimal.valueOf(1500),
                BigDecimal.valueOf(2.0)
        );
    }

    @Nested
    @DisplayName("viewValuation(CodeInputData) メソッド")
    class ViewValuationByCode {

        @DisplayName("viewValuation : 企業コード指定で評価結果を targetDate 降順に返却する")
        @Test
        void sortsByTargetDateDesc() {
            final CodeInputData inputData = CodeInputData.of("12340");
            final ValuationEntity entityA = emptyValuation(1);
            final ValuationEntity entityB = emptyValuation(2);

            final CompanyValuationViewModel oldView = new CompanyValuationViewModel(
                    "1234", "テスト企業", LocalDate.parse("2024-04-01"),
                    BigDecimal.valueOf(1000), BigDecimal.valueOf(0.5), BigDecimal.valueOf(500), BigDecimal.valueOf(2.0),
                    LocalDate.parse("2024-03-01"), BigDecimal.valueOf(900), 1L, BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1.1), BigDecimal.valueOf(0.4), BigDecimal.valueOf(1500), BigDecimal.valueOf(2.0));
            final CompanyValuationViewModel newView = new CompanyValuationViewModel(
                    "1234", "テスト企業", LocalDate.parse("2024-05-01"),
                    null, null, null, BigDecimal.valueOf(2.0),
                    null, null, 30L, null, null, null, null, null);
            // N+1解消: 全エンティティを1回取得しバッチ生成する実装に追随
            when(valuationSpecification.findAllValuationEntities("12340")).thenReturn(List.of(entityA, entityB));
            when(viewSpecification.generateCompanyValuationViewsBatch(List.of(entityA, entityB)))
                    .thenReturn(List.of(oldView, newView));

            final List<CompanyValuationViewModel> actual = interactor.viewValuation(inputData);

            assertAll(
                    () -> assertEquals(2, actual.size()),
                    () -> assertEquals(LocalDate.parse("2024-05-01"), actual.get(0).targetDate()),
                    () -> assertEquals(LocalDate.parse("2024-04-01"), actual.get(1).targetDate())
            );
        }
    }

    @Nested
    @DisplayName("viewAllValuation メソッド")
    class ViewAllValuation {

        @DisplayName("viewAllValuation : 提出日（daySinceSubmitDate=0）の行を除外する")
        @Test
        void excludesSubmitDateRow() {
            final CompanyValuationViewModel onSubmit = valuationView("1000", BigDecimal.valueOf(2.0), 0L);
            final CompanyValuationViewModel afterSubmit = valuationView("2000", BigDecimal.valueOf(2.0), 5L);
            when(companySpecification.targetCode4Set()).thenReturn(Set.of("1000", "2000"));
            when(viewSpecification.findAllCompanyValuationView()).thenReturn(List.of(onSubmit, afterSubmit));
            doReturn(LocalDate.of(2026, 7, 18)).when(interactor).nowLocalDate();

            final List<CompanyValuationViewModel> actual = interactor.viewAllValuation();

            assertAll(
                    () -> assertEquals(1, actual.size()),
                    () -> assertEquals("2000", actual.get(0).code())
            );
        }

        @DisplayName("viewAllValuation : 生存対象外コードを除外する")
        @Test
        void excludesRemovedCompanies() {
            when(companySpecification.targetCode4Set()).thenReturn(Set.of("1000"));
            when(viewSpecification.findAllCompanyValuationView()).thenReturn(List.of(
                    valuationView("1000", BigDecimal.valueOf(2.0), 5L),
                    valuationView("2000", BigDecimal.valueOf(3.0), 5L)
            ));
            doReturn(LocalDate.of(2026, 7, 18)).when(interactor).nowLocalDate();

            final List<CompanyValuationViewModel> actual = interactor.viewAllValuation();

            assertEquals(List.of("1000"), actual.stream().map(CompanyValuationViewModel::code).toList());
        }

        @DisplayName("viewAllValuation : 対象日が90日を超えて古い行を除外し、境界日は残す")
        @Test
        void excludesStaleTargetDate() {
            when(companySpecification.targetCode4Set()).thenReturn(Set.of("2000", "3000"));
            doReturn(LocalDate.of(2026, 7, 18)).when(interactor).nowLocalDate();
            when(viewSpecification.findAllCompanyValuationView()).thenReturn(List.of(
                    new CompanyValuationViewModel(
                            "2000", "テスト企業", LocalDate.of(2026, 4, 18), BigDecimal.valueOf(1000), BigDecimal.valueOf(0.5),
                            BigDecimal.valueOf(500), BigDecimal.valueOf(2.0), LocalDate.parse("2024-03-01"),
                            BigDecimal.valueOf(900), 5L, BigDecimal.valueOf(100), BigDecimal.valueOf(1.1),
                            BigDecimal.valueOf(0.4), BigDecimal.valueOf(1500), BigDecimal.valueOf(2.0)),
                    new CompanyValuationViewModel(
                            "3000", "テスト企業", LocalDate.of(2026, 4, 19), BigDecimal.valueOf(1000), BigDecimal.valueOf(0.5),
                            BigDecimal.valueOf(500), BigDecimal.valueOf(2.0), LocalDate.parse("2024-03-01"),
                            BigDecimal.valueOf(900), 5L, BigDecimal.valueOf(100), BigDecimal.valueOf(1.1),
                            BigDecimal.valueOf(0.4), BigDecimal.valueOf(1500), BigDecimal.valueOf(2.0))
            ));

            final List<CompanyValuationViewModel> actual = interactor.viewAllValuation();

            assertEquals(List.of("3000"), actual.stream().map(CompanyValuationViewModel::code).toList());
        }
    }

    @Nested
    @DisplayName("updateView() メソッド")
    class UpdateView {

        @DisplayName("updateView : 全対象企業の最新評価をビューに upsert し、Slack 通知が有効なら送信する")
        @Test
        void upsertsAllAndNotifiesSlack() {
            final Company company = new Company(
                    "10000", "対象企業", null, null, "edinet1", null, null, null, null, false, false, true);
            when(companySpecification.inquiryAllTargetCompanies()).thenReturn(List.of(company));
            final ValuationEntity entity = emptyValuation();
            when(valuationSpecification.findLatestValuation("10000")).thenReturn(Optional.of(entity));
            final CompanyValuationViewModel view = valuationView("1000", BigDecimal.valueOf(2.0), 5L);
            when(viewSpecification.generateCompanyValuationView(entity)).thenReturn(view);

            interactor.updateView();

            assertAll(
                    () -> verify(viewSpecification, times(1)).upsert(view),
                    () -> verify(slackClient, times(1)).sendMessage(anyString())
            );
        }

        @DisplayName("updateView : Slack 通知が無効なら送信しない")
        @Test
        void skipsSlackWhenDisabled() {
            interactor.updateViewEnabled = false;
            when(companySpecification.inquiryAllTargetCompanies()).thenReturn(List.of());

            interactor.updateView();

            verify(slackClient, times(0)).sendMessage(anyString());
        }
    }

    @Nested
    @DisplayName("updateView(CodeInputData) メソッド")
    class UpdateViewByCode {

        @DisplayName("updateView : 指定企業の最新評価が存在すればビューに upsert する")
        @Test
        void upsertsWhenPresent() {
            final CodeInputData inputData = CodeInputData.of("1234");
            final ValuationEntity entity = emptyValuation();
            when(valuationSpecification.findLatestValuation("1234")).thenReturn(Optional.of(entity));
            final CompanyValuationViewModel view = valuationView("1234", BigDecimal.valueOf(2.0), 5L);
            when(viewSpecification.generateCompanyValuationView(entity)).thenReturn(view);

            interactor.updateView(inputData);

            verify(viewSpecification, times(1)).upsert(view);
        }

        @DisplayName("updateView : 指定企業の最新評価が存在しなければ何もしない")
        @Test
        void skipsWhenAbsent() {
            final CodeInputData inputData = CodeInputData.of("1234");
            when(valuationSpecification.findLatestValuation("1234")).thenReturn(Optional.empty());

            interactor.updateView(inputData);

            verify(viewSpecification, times(0)).upsert(any(CompanyValuationViewModel.class));
        }

        @DisplayName("updateView : 例外発生時は握りつぶしてログ出力のみとする")
        @Test
        void swallowsException() {
            final CodeInputData inputData = CodeInputData.of("1234");
            when(valuationSpecification.findLatestValuation("1234")).thenThrow(new RuntimeException("boom"));

            assertDoesNotThrow(() -> interactor.updateView(inputData));
            assertTrue(true);
        }
    }

    @Nested
    @DisplayName("computeGrahamIndustryZScore メソッド")
    class ComputeGrahamIndustryZScore {

        @DisplayName("computeGrahamIndustryZScore : 同一業種内のグレアム指数を標準化する")
        @Test
        void sameIndustry_standardized() {
            final Map<String, Integer> industryByCode = Map.of("1234", 1, "5678", 1, "9012", 1);
            final List<CompanyValuationViewModel> valuations = List.of(
                    cvvm("1234", BigDecimal.valueOf(10)),
                    cvvm("5678", BigDecimal.valueOf(20)),
                    cvvm("9012", BigDecimal.valueOf(30)));

            final Map<String, BigDecimal> result =
                    ViewValuationInteractor.computeGrahamIndustryZScore(valuations, industryByCode);

            assertAll(
                    () -> assertEquals(new BigDecimal("-1.22"), result.get("1234")),
                    () -> assertEquals(new BigDecimal("0.00"), result.get("5678")),
                    () -> assertEquals(new BigDecimal("1.22"), result.get("9012")));
        }

        @DisplayName("computeGrahamIndustryZScore : 業種ごとに独立して標準化される（業種を跨いで混ざらない）")
        @Test
        void differentIndustries_doNotMix() {
            final Map<String, Integer> industryByCode = Map.of(
                    "1234", 1, "5678", 1, "9012", 1,
                    "3456", 2, "7890", 2, "2345", 2);
            final List<CompanyValuationViewModel> valuations = List.of(
                    cvvm("1234", BigDecimal.valueOf(10)),
                    cvvm("5678", BigDecimal.valueOf(20)),
                    cvvm("9012", BigDecimal.valueOf(30)),
                    cvvm("3456", BigDecimal.valueOf(100)),
                    cvvm("7890", BigDecimal.valueOf(200)),
                    cvvm("2345", BigDecimal.valueOf(300)));

            final Map<String, BigDecimal> result =
                    ViewValuationInteractor.computeGrahamIndustryZScore(valuations, industryByCode);

            assertAll(
                    () -> assertEquals(new BigDecimal("-1.22"), result.get("1234")),
                    () -> assertEquals(new BigDecimal("-1.22"), result.get("3456")),
                    () -> assertEquals(new BigDecimal("1.22"), result.get("2345")));
        }

        @DisplayName("computeGrahamIndustryZScore : グレアム指数が null の社は母集団・結果から除外される")
        @Test
        void nullGraham_excluded() {
            final Map<String, Integer> industryByCode = Map.of("1234", 1, "5678", 1, "9012", 1, "3456", 1);
            final List<CompanyValuationViewModel> valuations = List.of(
                    cvvm("1234", BigDecimal.valueOf(10)),
                    cvvm("5678", null),
                    cvvm("9012", BigDecimal.valueOf(20)),
                    cvvm("3456", BigDecimal.valueOf(30)));

            final Map<String, BigDecimal> result =
                    ViewValuationInteractor.computeGrahamIndustryZScore(valuations, industryByCode);

            assertAll(
                    () -> assertTrue(result.get("5678") == null),
                    () -> assertEquals(new BigDecimal("-1.22"), result.get("1234")),
                    () -> assertEquals(new BigDecimal("1.22"), result.get("3456")));
        }

        @DisplayName("computeGrahamIndustryZScore : 業種内社数が3未満のときは算出しない")
        @Test
        void lessThanThree_notComputed() {
            final Map<String, Integer> industryByCode = Map.of("1234", 1, "5678", 1);
            final List<CompanyValuationViewModel> valuations = List.of(
                    cvvm("1234", BigDecimal.valueOf(10)),
                    cvvm("5678", BigDecimal.valueOf(20)));

            final Map<String, BigDecimal> result =
                    ViewValuationInteractor.computeGrahamIndustryZScore(valuations, industryByCode);

            assertTrue(result.isEmpty());
        }

        @DisplayName("computeGrahamIndustryZScore : 標準偏差が0のときは算出しない（ゼロ除算回避）")
        @Test
        void zeroStandardDeviation_notComputed() {
            final Map<String, Integer> industryByCode = Map.of("1234", 1, "5678", 1, "9012", 1);
            final List<CompanyValuationViewModel> valuations = List.of(
                    cvvm("1234", BigDecimal.valueOf(15)),
                    cvvm("5678", BigDecimal.valueOf(15)),
                    cvvm("9012", BigDecimal.valueOf(15)));

            final Map<String, BigDecimal> result =
                    ViewValuationInteractor.computeGrahamIndustryZScore(valuations, industryByCode);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findGrahamIndustryZScore メソッド")
    class FindGrahamIndustryZScore {

        @DisplayName("findGrahamIndustryZScore : 全対象企業の業種と評価ビューから業種内zスコアを返す")
        @Test
        void returnsZScoreByCode() {
            when(companySpecification.industryIdByCode4()).thenReturn(Map.of("1234", 1, "5678", 1, "9012", 1));
            doReturn(List.of(
                    cvvm("1234", BigDecimal.valueOf(10)),
                    cvvm("5678", BigDecimal.valueOf(20)),
                    cvvm("9012", BigDecimal.valueOf(30))))
                    .when(interactor).viewAllValuation();

            final Map<String, BigDecimal> result = interactor.findGrahamIndustryZScore();

            assertAll(
                    () -> assertEquals(new BigDecimal("-1.22"), result.get("1234")),
                    () -> assertEquals(new BigDecimal("1.22"), result.get("9012")));
        }
    }

    private static CompanyValuationViewModel cvvm(final String code, final BigDecimal grahamIndex) {
        return new CompanyValuationViewModel(
                code, code, null, null, grahamIndex, null, null, null, null, null, null, null, null, null, null);
    }

}
