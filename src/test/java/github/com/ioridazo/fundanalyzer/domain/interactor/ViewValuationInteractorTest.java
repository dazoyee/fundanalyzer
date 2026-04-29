package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.IndustryEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.ValuationViewBean;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.IndustrySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ValuationSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    private IndustrySpecification industrySpecification;
    private CompanySpecification companySpecification;
    private ValuationSpecification valuationSpecification;
    private ViewSpecification viewSpecification;
    private SlackClient slackClient;

    private ViewValuationInteractor interactor;

    @BeforeEach
    void setUp() {
        industrySpecification = mock(IndustrySpecification.class);
        companySpecification = mock(CompanySpecification.class);
        valuationSpecification = mock(ValuationSpecification.class);
        viewSpecification = mock(ViewSpecification.class);
        slackClient = mock(SlackClient.class);

        interactor = spy(new ViewValuationInteractor(
                industrySpecification,
                companySpecification,
                valuationSpecification,
                viewSpecification,
                slackClient
        ));
        interactor.configDiscountRate = BigDecimal.valueOf(170);
        interactor.noTargetList = List.of();
        interactor.updateViewEnabled = true;
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
                LocalDate.parse("2024-04-01"),
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
    @DisplayName("viewValuation メソッド")
    class ViewValuation {

        @DisplayName("viewValuation : 割安度が 170% 以上 1000% 未満の企業のみ返却する")
        @Test
        void filtersByDiscountRate() {
            final CompanyValuationViewModel low = valuationView("1000", BigDecimal.valueOf(1.0), 5L);
            final CompanyValuationViewModel midA = valuationView("2000", BigDecimal.valueOf(2.0), 5L);
            final CompanyValuationViewModel midB = valuationView("3000", BigDecimal.valueOf(9.99), 5L);
            final CompanyValuationViewModel tooHigh = valuationView("4000", BigDecimal.valueOf(1000), 5L);

            doReturn(List.of(low, midA, midB, tooHigh)).when(interactor).viewAllValuation();

            final List<CompanyValuationViewModel> actual = interactor.viewValuation();

            assertAll(
                    () -> assertEquals(2, actual.size()),
                    () -> assertEquals("2000", actual.get(0).code()),
                    () -> assertEquals("3000", actual.get(1).code())
            );
        }
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
            when(valuationSpecification.findValuation("12340")).thenReturn(List.of(entityA, entityB));

            final CompanyValuationViewModel oldView = valuationView("1234", BigDecimal.valueOf(2.0), 1L);
            final CompanyValuationViewModel newView = new CompanyValuationViewModel(
                    "1234", "テスト企業", LocalDate.parse("2024-05-01"),
                    null, null, null, BigDecimal.valueOf(2.0),
                    null, null, 30L, null, null, null, null, null);
            when(viewSpecification.generateCompanyValuationView(entityA)).thenReturn(oldView);
            when(viewSpecification.generateCompanyValuationView(entityB)).thenReturn(newView);

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
            when(viewSpecification.findAllCompanyValuationView()).thenReturn(List.of(onSubmit, afterSubmit));

            final List<CompanyValuationViewModel> actual = interactor.viewAllValuation();

            assertAll(
                    () -> assertEquals(1, actual.size()),
                    () -> assertEquals("2000", actual.get(0).code())
            );
        }
    }

    @Nested
    @DisplayName("viewFavoriteValuation メソッド")
    class ViewFavoriteValuation {

        @DisplayName("viewFavoriteValuation : お気に入り登録された企業のみ返却する")
        @Test
        void filtersByFavorite() {
            final Company favorite = new Company(
                    "12340", "お気に入り企業", null, null, "edinet1", null, null, null, null, true, true);
            when(companySpecification.findFavoriteCompanies()).thenReturn(List.of(favorite));

            final CompanyValuationViewModel matched = valuationView("1234", BigDecimal.valueOf(2.0), 5L);
            final CompanyValuationViewModel unmatched = valuationView("9999", BigDecimal.valueOf(2.0), 5L);
            doReturn(List.of(matched, unmatched)).when(interactor).viewAllValuation();

            final List<CompanyValuationViewModel> actual = interactor.viewFavoriteValuation();

            assertAll(
                    () -> assertEquals(1, actual.size()),
                    () -> assertEquals("1234", actual.get(0).code())
            );
        }
    }

    @Nested
    @DisplayName("viewIndustryValuation メソッド")
    class ViewIndustryValuation {

        @DisplayName("viewIndustryValuation : 対象業種のみビューを生成する")
        @Test
        void generatesForTargetIndustries() {
            final IndustryEntity industryA = new IndustryEntity(1, "業種A", null);
            final IndustryEntity industryB = new IndustryEntity(2, "業種B", null);
            when(industrySpecification.inquiryIndustryList()).thenReturn(List.of(industryA, industryB));
            when(industrySpecification.isTarget(1)).thenReturn(true);
            when(industrySpecification.isTarget(2)).thenReturn(false);
            when(viewSpecification.findCompanyValuationViewList(1)).thenReturn(List.of());
            final IndustryValuationViewModel industryView = IndustryValuationViewModel.of(
                    "業種A", 0.0, 0.0, 0.0, 0);
            when(viewSpecification.generateIndustryValuationView(eqStr("業種A"), any())).thenReturn(industryView);

            final List<IndustryValuationViewModel> actual = interactor.viewIndustryValuation();

            assertAll(
                    () -> assertEquals(1, actual.size()),
                    () -> assertEquals("業種A", actual.get(0).name())
            );
        }

        private String eqStr(final String value) {
            return org.mockito.ArgumentMatchers.eq(value);
        }
    }

    @Nested
    @DisplayName("updateView() メソッド")
    class UpdateView {

        @DisplayName("updateView : 全対象企業の最新評価をビューに upsert し、Slack 通知が有効なら送信する")
        @Test
        void upsertsAllAndNotifiesSlack() {
            final Company company = new Company(
                    "10000", "対象企業", null, null, "edinet1", null, null, null, null, false, true);
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
}
