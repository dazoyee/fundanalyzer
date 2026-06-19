package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.AnalysisResultViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CompanyViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.FinancialStatementViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.IndicatorViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.MinkabuViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.StockPriceViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorporatePresenterTest {

    private ViewService viewService;
    private CorporatePresenter presenter;

    @BeforeEach
    void setUp() {
        viewService = Mockito.mock(ViewService.class);
        presenter = new CorporatePresenter(viewService);
        presenter.targetTypeCodes = List.of("120", "130");
    }

    /**
     * シンプルな CorporateDetailViewModel を生成する。
     */
    private static CorporateDetailViewModel buildDetailView(
            final List<AnalysisResultViewModel> analysisResultList,
            final List<IndicatorViewModel> indicatorList,
            final List<MinkabuViewModel> minkabuList,
            final List<StockPriceViewModel> stockPriceList) {
        final CompanyViewModel company = new CompanyViewModel(
                "12345", "テスト会社", "情報・通信業", "E00001", Boolean.FALSE,
                100, "3月31日", "10.0", "1.0", "5.0",
                "1000000", "1000000000", "2.5", null
        );
        final CorporateViewModel corporate = new CorporateViewModel();
        final List<FinancialStatementViewModel> financial = List.of();
        return CorporateDetailViewModel.of(
                company,
                "0001",
                "0003",
                corporate,
                analysisResultList,
                indicatorList,
                financial,
                minkabuList,
                stockPriceList
        );
    }


    @Nested
    @DisplayName("corporateDetailViewV3 メソッド")
    class CorporateDetailViewV3 {

        @Test
        @DisplayName("呼び出された場合 → corporate-v2 view 名を返す")
        void returnsCorporateV2() {
            final String code = "9999";
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), List.of(), List.of());
            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            final String result = presenter.corporateDetailViewV3(code, null, model);

            assertEquals("corporate-v2", result);
            Mockito.verify(viewService).getCorporateDetailView(CodeInputData.of(code));
            Mockito.verify(model).addAttribute("corporate", view.getCompany());
        }

        @Test
        @DisplayName("target=quart → ViewService.getCorporateDetailView(CodeInputData, Target) が呼ばれて target Attribute が設定される")
        void targetQuart_callsTargetedView() {
            final String code = "9999";
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), List.of(), List.of());
            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code), Target.QUART)).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            final String result = presenter.corporateDetailViewV3(code, "quart", model);

            assertEquals("corporate-v2", result);
            Mockito.verify(model).addAttribute("target", "quart");
        }

        @Test
        @DisplayName("target なし → backwardCode / forwardCode が Model attribute に設定される (target 無し経路でも前後ナビが有効)")
        void noTarget_setsBackwardAndForwardAttributes() {
            final String code = "9999";
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), List.of(), List.of());
            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            presenter.corporateDetailViewV3(code, null, model);

            Mockito.verify(model).addAttribute("backwardCode", "0001");
            Mockito.verify(model).addAttribute("forwardCode", "0003");
        }

        @Test
        @DisplayName("グレアム指数の業種内zスコアが grahamIndustryZScore Model attribute に設定される")
        void setsGrahamIndustryZScoreAttribute() {
            final String code = "9999";
            final CorporateDetailViewModel view = buildDetailView(List.of(), List.of(), List.of(), List.of());
            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());
            Mockito.when(viewService.getGrahamIndustryZScore(CodeInputData.of(code)))
                    .thenReturn(new BigDecimal("-1.06"));

            final Model model = Mockito.mock(Model.class);
            presenter.corporateDetailViewV3(code, null, model);

            Mockito.verify(model).addAttribute("grahamIndustryZScore", new BigDecimal("-1.06"));
        }

        @Test
        @DisplayName("RIM理論株価と2モデル合意度（割安2/2）が Model attribute に設定される")
        void setsRimAndAgreement() {
            final String code = "9999";
            final CorporateViewModel corporate = new CorporateViewModel();
            corporate.setLatestCorporateValue(BigDecimal.valueOf(3000));
            corporate.setLatestStockPrice(BigDecimal.valueOf(1000));
            final CompanyViewModel company = new CompanyViewModel(
                    "12345", "テスト会社", "情報・通信業", "E00001", Boolean.FALSE,
                    100, "3月31日", "10.0", "1.0", "5.0",
                    "1000000", "1000000000", "2.5", null);
            final AnalysisResultViewModel ar = new AnalysisResultViewModel(
                    LocalDate.of(2026, 3, 31), LocalDate.of(2025, 3, 31),
                    BigDecimal.valueOf(3000), BigDecimal.valueOf(1500), "120", null);
            final CorporateDetailViewModel view = CorporateDetailViewModel.of(
                    company, "0001", "0003", corporate, List.of(ar),
                    List.of(), List.of(), List.of(), List.of());
            Mockito.when(viewService.getCorporateDetailView(CodeInputData.of(code))).thenReturn(view);
            Mockito.when(viewService.getValuationView(CodeInputData.of(code))).thenReturn(List.of());

            final Model model = Mockito.mock(Model.class);
            presenter.corporateDetailViewV3(code, null, model);

            Mockito.verify(model).addAttribute("rimValue", BigDecimal.valueOf(1500));
            Mockito.verify(model).addAttribute("agreementUndervalued", 2);
            Mockito.verify(model).addAttribute("agreementTotal", 2);
        }
    }
}
