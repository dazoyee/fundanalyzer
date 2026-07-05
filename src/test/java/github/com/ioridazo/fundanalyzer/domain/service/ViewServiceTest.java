package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.usecase.BacktestUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DistributionUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewEdinetUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewValuationUseCase;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.presenter.Target;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.DistributionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ViewService のテスト")
class ViewServiceTest {

    private CompanyUseCase companyUseCase;
    private DocumentUseCase documentUseCase;
    private ViewCorporateUseCase viewCorporateUseCase;
    private ViewEdinetUseCase viewEdinetUseCase;
    private ViewValuationUseCase viewValuationUseCase;
    private BacktestUseCase backtestUseCase;
    private DistributionUseCase distributionUseCase;
    private ViewService service;

    @BeforeEach
    void setUp() {
        companyUseCase = mock(CompanyUseCase.class);
        documentUseCase = mock(DocumentUseCase.class);
        viewCorporateUseCase = mock(ViewCorporateUseCase.class);
        viewEdinetUseCase = mock(ViewEdinetUseCase.class);
        viewValuationUseCase = mock(ViewValuationUseCase.class);
        backtestUseCase = mock(BacktestUseCase.class);
        distributionUseCase = mock(DistributionUseCase.class);
        service = new ViewService(
                companyUseCase, documentUseCase,
                viewCorporateUseCase, viewEdinetUseCase, viewValuationUseCase, backtestUseCase, distributionUseCase);
    }

    @Nested
    @DisplayName("企業情報ビュー取得")
    class CorporateViews {

        @DisplayName("getCorporateView : viewCorporateUseCase.viewMain に委譲する")
        @Test
        void main() {
            when(viewCorporateUseCase.viewMain()).thenReturn(List.of());
            assertSame(List.of(), service.getCorporateView());
            verify(viewCorporateUseCase, times(1)).viewMain();
        }

        @DisplayName("getQuartCorporateView : viewCorporateUseCase.viewQuart に委譲する")
        @Test
        void quart() {
            when(viewCorporateUseCase.viewQuart()).thenReturn(List.of());
            service.getQuartCorporateView();
            verify(viewCorporateUseCase, times(1)).viewQuart();
        }

        @DisplayName("getAllCorporateView : viewCorporateUseCase.viewAll に委譲する")
        @Test
        void all() {
            when(viewCorporateUseCase.viewAll()).thenReturn(List.of());
            service.getAllCorporateView();
            verify(viewCorporateUseCase, times(1)).viewAll();
        }

        @DisplayName("getFavoriteCorporateView : viewCorporateUseCase.viewFavorite に委譲する")
        @Test
        void favorite() {
            when(viewCorporateUseCase.viewFavorite()).thenReturn(List.of());
            service.getFavoriteCorporateView();
            verify(viewCorporateUseCase, times(1)).viewFavorite();
        }

        @DisplayName("getStarCorporateView : viewCorporateUseCase.viewStar に委譲する")
        @Test
        void star() {
            when(viewCorporateUseCase.viewStar()).thenReturn(List.of());
            service.getStarCorporateView();
            verify(viewCorporateUseCase, times(1)).viewStar();
        }
    }

    @Nested
    @DisplayName("EDINET ビュー取得")
    class EdinetViews {

        @DisplayName("getEdinetListView : viewEdinetUseCase.viewMain に委譲する")
        @Test
        void main() {
            when(viewEdinetUseCase.viewMain()).thenReturn(List.of());
            service.getEdinetListView();
            verify(viewEdinetUseCase, times(1)).viewMain();
        }

        @DisplayName("getAllEdinetListView : viewEdinetUseCase.viewAll に委譲する")
        @Test
        void all() {
            when(viewEdinetUseCase.viewAll()).thenReturn(List.of());
            service.getAllEdinetListView();
            verify(viewEdinetUseCase, times(1)).viewAll();
        }
    }

    @Nested
    @DisplayName("更新日時取得")
    class UpdateDate {

        @DisplayName("getUpdateDate : companyUseCase.getUpdateDate に委譲する")
        @Test
        void delegates() {
            when(companyUseCase.getUpdateDate()).thenReturn("2024-04-01");
            assertEquals("2024-04-01", service.getUpdateDate());
        }
    }

    @Nested
    @DisplayName("詳細ビュー取得")
    class DetailViews {

        @DisplayName("getCorporateDetailView(code) : viewCorporateUseCase に委譲する")
        @Test
        void corporateDetail() {
            final CodeInputData input = CodeInputData.of("1234");
            service.getCorporateDetailView(input);
            verify(viewCorporateUseCase, times(1)).viewCorporateDetail(input);
        }

        @DisplayName("getCorporateDetailView(code, target) : Target 引数版に委譲する")
        @Test
        void corporateDetailWithTarget() {
            final CodeInputData input = CodeInputData.of("1234");
            service.getCorporateDetailView(input, Target.MAIN);
            verify(viewCorporateUseCase, times(1)).viewCorporateDetail(input, Target.MAIN);
        }

        @DisplayName("getEdinetDetailView : viewEdinetUseCase.viewEdinetDetail に委譲する")
        @Test
        void edinetDetail() {
            final DateInputData input = DateInputData.of(LocalDate.parse("2024-04-01"));
            service.getEdinetDetailView(input);
            verify(viewEdinetUseCase, times(1)).viewEdinetDetail(input);
        }
    }

    @Nested
    @DisplayName("ビュー更新")
    class UpdateViews {

        @DisplayName("updateCorporateView : viewCorporateUseCase.updateView に委譲する")
        @Test
        void corporate() {
            service.updateCorporateView();
            verify(viewCorporateUseCase, times(1)).updateView();
        }

        @DisplayName("updateEdinetView : viewEdinetUseCase.updateView に委譲する")
        @Test
        void edinet() {
            service.updateEdinetView();
            verify(viewEdinetUseCase, times(1)).updateView();
        }

        @DisplayName("updateEdinetListView : 書類除外と EDINET ビュー更新を順に呼ぶ")
        @Test
        void edinetList() {
            final DateInputData input = DateInputData.of(LocalDate.parse("2024-04-01"));
            service.updateEdinetListView(input);
            verify(documentUseCase, times(1)).removeDocument(input);
            verify(viewEdinetUseCase, times(1)).updateView(input);
        }

        @DisplayName("updateValuationView() : viewValuationUseCase.updateView() に委譲する")
        @Test
        void valuationAll() {
            service.updateValuationView();
            verify(viewValuationUseCase, times(1)).updateView();
        }

        @DisplayName("updateValuationView(code) : viewValuationUseCase.updateView(input) に委譲する")
        @Test
        void valuationByCode() {
            final CodeInputData input = CodeInputData.of("1234");
            service.updateValuationView(input);
            verify(viewValuationUseCase, times(1)).updateView(input);
        }
    }

    @Nested
    @DisplayName("評価ビュー取得")
    class ValuationViews {

        @DisplayName("getValuationView(code) : viewValuationUseCase.viewValuation(input) に委譲する")
        @Test
        void byCode() {
            final CodeInputData input = CodeInputData.of("1234");
            when(viewValuationUseCase.viewValuation(input)).thenReturn(List.of());
            service.getValuationView(input);
            verify(viewValuationUseCase, times(1)).viewValuation(input);
        }

        @DisplayName("getAllValuationView : viewValuationUseCase.viewAllValuation に委譲する")
        @Test
        void all() {
            when(viewValuationUseCase.viewAllValuation()).thenReturn(List.of());
            service.getAllValuationView();
            verify(viewValuationUseCase, times(1)).viewAllValuation();
        }

        @DisplayName("getDistributionView : distributionUseCase.distribution に委譲する")
        @Test
        void distribution() {
            final DistributionResult expected = new DistributionResult(List.of(), List.of(), null, null, null, List.of());
            when(distributionUseCase.distribution()).thenReturn(expected);

            assertSame(expected, service.getDistributionView());
            verify(distributionUseCase, times(1)).distribution();
        }

        @DisplayName("getGrahamIndustryZScore → 該当コードの業種内zを返す / 該当なしは null")
        @Test
        void grahamIndustryZScore() {
            when(viewValuationUseCase.findGrahamIndustryZScore())
                    .thenReturn(Map.of("1234", new BigDecimal("1.50")));

            assertEquals(new BigDecimal("1.50"), service.getGrahamIndustryZScore(CodeInputData.of("1234")));
            assertNull(service.getGrahamIndustryZScore(CodeInputData.of("9999")));
        }
    }

    @Nested
    @DisplayName("findCompanyTable メソッド")
    class FindCompanyTable {

        private CorporateViewModel sample(final String code, final String name, final LocalDate submitDate, final BigDecimal value) {
            final CorporateViewModel m = new CorporateViewModel();
            m.setCode(code);
            m.setName(name);
            m.setSubmitDate(submitDate);
            m.setLatestCorporateValue(value);
            return m;
        }

        @Test
        @DisplayName("target=null の場合 → viewCorporateUseCase.viewMain が呼ばれる")
        void targetNull_callsViewMain() {
            when(viewCorporateUseCase.viewMain()).thenReturn(List.of());
            service.findCompanyTable(new CompanyTableQuery(null, null, PageRequest.of(0, 25, Sort.by("code"))));
            verify(viewCorporateUseCase, times(1)).viewMain();
        }

        @Test
        @DisplayName("target=quart の場合 → viewCorporateUseCase.viewQuart が呼ばれる")
        void targetQuart_callsViewQuart() {
            when(viewCorporateUseCase.viewQuart()).thenReturn(List.of());
            service.findCompanyTable(new CompanyTableQuery("quart", null, PageRequest.of(0, 25, Sort.by("code"))));
            verify(viewCorporateUseCase, times(1)).viewQuart();
        }

        @Test
        @DisplayName("target=all の場合 → viewCorporateUseCase.viewAll が呼ばれる")
        void targetAll_callsViewAll() {
            when(viewCorporateUseCase.viewAll()).thenReturn(List.of());
            service.findCompanyTable(new CompanyTableQuery("all", null, PageRequest.of(0, 25, Sort.by("code"))));
            verify(viewCorporateUseCase, times(1)).viewAll();
        }

        @Test
        @DisplayName("target=favorite の場合 → viewCorporateUseCase.viewFavorite が呼ばれる")
        void targetFavorite_callsViewFavorite() {
            when(viewCorporateUseCase.viewFavorite()).thenReturn(List.of());
            service.findCompanyTable(new CompanyTableQuery("favorite", null, PageRequest.of(0, 25, Sort.by("code"))));
            verify(viewCorporateUseCase, times(1)).viewFavorite();
        }

        @Test
        @DisplayName("target=star の場合 → viewCorporateUseCase.viewStar が呼ばれる")
        void targetStar_callsViewStar() {
            when(viewCorporateUseCase.viewStar()).thenReturn(List.of());
            service.findCompanyTable(new CompanyTableQuery("star", null, PageRequest.of(0, 25, Sort.by("code"))));
            verify(viewCorporateUseCase, times(1)).viewStar();
        }

        @Test
        @DisplayName("keyword で code に partial match → 該当のみ返す")
        void keywordCodeMatch_filtersByCode() {
            when(viewCorporateUseCase.viewMain()).thenReturn(List.of(
                    sample("1234", "Alpha", LocalDate.of(2025, 1, 1), BigDecimal.TEN),
                    sample("5678", "Beta", LocalDate.of(2025, 1, 2), BigDecimal.ONE)
            ));
            final CompanyTablePage page = service.findCompanyTable(
                    new CompanyTableQuery(null, "12", PageRequest.of(0, 25, Sort.by("code"))));
            assertEquals(1L, page.totalElements());
            assertEquals("1234", page.companies().get(0).getCode());
        }

        @Test
        @DisplayName("keyword で name に case-insensitive partial match → 該当のみ返す")
        void keywordNameMatch_caseInsensitive() {
            when(viewCorporateUseCase.viewMain()).thenReturn(List.of(
                    sample("1234", "Alpha", LocalDate.of(2025, 1, 1), BigDecimal.TEN),
                    sample("5678", "Beta", LocalDate.of(2025, 1, 2), BigDecimal.ONE)
            ));
            final CompanyTablePage page = service.findCompanyTable(
                    new CompanyTableQuery(null, "alpha", PageRequest.of(0, 25, Sort.by("code"))));
            assertEquals(1L, page.totalElements());
            assertEquals("Alpha", page.companies().get(0).getName());
        }

        @Test
        @DisplayName("keyword=null と空文字 → filter なしで全件返す")
        void keywordNullOrBlank_returnsAll() {
            when(viewCorporateUseCase.viewMain()).thenReturn(List.of(
                    sample("1234", "Alpha", LocalDate.of(2025, 1, 1), BigDecimal.TEN),
                    sample("5678", "Beta", LocalDate.of(2025, 1, 2), BigDecimal.ONE)
            ));
            final CompanyTablePage pageNull = service.findCompanyTable(
                    new CompanyTableQuery(null, null, PageRequest.of(0, 25, Sort.by("code"))));
            final CompanyTablePage pageBlank = service.findCompanyTable(
                    new CompanyTableQuery(null, "  ", PageRequest.of(0, 25, Sort.by("code"))));
            assertEquals(2L, pageNull.totalElements());
            assertEquals(2L, pageBlank.totalElements());
        }

        @Test
        @DisplayName("sort=name ASC → name の昇順で並ぶ")
        void sortNameAsc() {
            when(viewCorporateUseCase.viewMain()).thenReturn(List.of(
                    sample("1234", "Beta", LocalDate.of(2025, 1, 2), BigDecimal.ONE),
                    sample("5678", "Alpha", LocalDate.of(2025, 1, 1), BigDecimal.TEN)
            ));
            final CompanyTablePage page = service.findCompanyTable(
                    new CompanyTableQuery(null, null, PageRequest.of(0, 25, Sort.by(Sort.Direction.ASC, "name"))));
            assertEquals("Alpha", page.companies().get(0).getName());
            assertEquals("Beta", page.companies().get(1).getName());
        }

        @Test
        @DisplayName("sort=submitDate DESC → submitDate の降順で並ぶ")
        void sortSubmitDateDesc() {
            when(viewCorporateUseCase.viewMain()).thenReturn(List.of(
                    sample("1234", "Alpha", LocalDate.of(2025, 1, 1), BigDecimal.TEN),
                    sample("5678", "Beta", LocalDate.of(2025, 6, 1), BigDecimal.ONE)
            ));
            final CompanyTablePage page = service.findCompanyTable(
                    new CompanyTableQuery(null, null, PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "submitDate"))));
            assertEquals(LocalDate.of(2025, 6, 1), page.companies().get(0).getSubmitDate());
            assertEquals(LocalDate.of(2025, 1, 1), page.companies().get(1).getSubmitDate());
        }

        @Test
        @DisplayName("page=1 size=2 → 3 件目以降を返す")
        void paging_skipAndLimit() {
            when(viewCorporateUseCase.viewMain()).thenReturn(List.of(
                    sample("0001", "A", LocalDate.of(2025, 1, 1), BigDecimal.ONE),
                    sample("0002", "B", LocalDate.of(2025, 1, 2), BigDecimal.ONE),
                    sample("0003", "C", LocalDate.of(2025, 1, 3), BigDecimal.ONE),
                    sample("0004", "D", LocalDate.of(2025, 1, 4), BigDecimal.ONE),
                    sample("0005", "E", LocalDate.of(2025, 1, 5), BigDecimal.ONE)
            ));
            final CompanyTablePage page = service.findCompanyTable(
                    new CompanyTableQuery(null, null, PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "code"))));
            assertEquals(5L, page.totalElements());
            assertEquals(3, page.totalPages());
            assertEquals(1, page.pageNumber());
            assertEquals(2, page.pageSize());
            assertEquals(2, page.companies().size());
            assertEquals("0003", page.companies().get(0).getCode());
            assertEquals("0004", page.companies().get(1).getCode());
        }

        @Test
        @DisplayName("空件数の場合 → totalPages=0 / 空 List を返す")
        void emptyResult() {
            when(viewCorporateUseCase.viewMain()).thenReturn(List.of());
            final CompanyTablePage page = service.findCompanyTable(
                    new CompanyTableQuery(null, null, PageRequest.of(0, 25, Sort.by("code"))));
            assertEquals(0L, page.totalElements());
            assertEquals(0, page.totalPages());
            assertTrue(page.companies().isEmpty());
        }

        @Test
        @DisplayName("ホワイトリスト外の sort field → 並び順は元のまま（applySort で comparator が null）")
        void unknownSortField_keepsOriginalOrder() {
            when(viewCorporateUseCase.viewMain()).thenReturn(List.of(
                    sample("9999", "Z", LocalDate.of(2025, 1, 1), BigDecimal.ONE),
                    sample("0001", "A", LocalDate.of(2025, 1, 2), BigDecimal.ONE)
            ));
            final CompanyTablePage page = service.findCompanyTable(
                    new CompanyTableQuery(null, null, PageRequest.of(0, 25, Sort.by(Sort.Direction.ASC, "secret"))));
            assertEquals("9999", page.companies().get(0).getCode());
            assertEquals("0001", page.companies().get(1).getCode());
        }
    }

    @Nested
    @DisplayName("findEdinetListTable メソッド")
    class FindEdinetListTable {

        private EdinetListViewModel edinet(final LocalDate submitDate, final long countAll) {
            return new EdinetListViewModel(
                    submitDate, countAll, 0L, 0L, 0L, "", "", 0L, 0L);
        }

        @Test
        @DisplayName("target=null → viewEdinetUseCase.viewMain が呼ばれる")
        void targetNull_callsViewMain() {
            when(viewEdinetUseCase.viewMain()).thenReturn(List.of());
            service.findEdinetListTable(new EdinetListTableQuery(
                    null, null, PageRequest.of(0, 25, Sort.by("submitDate"))));
            verify(viewEdinetUseCase, times(1)).viewMain();
        }

        @Test
        @DisplayName("target=all → viewEdinetUseCase.viewAll が呼ばれる")
        void targetAll_callsViewAll() {
            when(viewEdinetUseCase.viewAll()).thenReturn(List.of());
            service.findEdinetListTable(new EdinetListTableQuery(
                    "all", null, PageRequest.of(0, 25, Sort.by("submitDate"))));
            verify(viewEdinetUseCase, times(1)).viewAll();
        }

        @Test
        @DisplayName("keyword で submitDate に partial match")
        void keywordSubmitDateMatch() {
            when(viewEdinetUseCase.viewMain()).thenReturn(List.of(
                    edinet(LocalDate.of(2025, 1, 1), 10L),
                    edinet(LocalDate.of(2025, 6, 1), 20L)
            ));
            final EdinetListTablePage page = service.findEdinetListTable(
                    new EdinetListTableQuery(null, "2025-06",
                            PageRequest.of(0, 25, Sort.by("submitDate"))));
            assertEquals(1L, page.totalElements());
            assertEquals(LocalDate.of(2025, 6, 1), page.rows().get(0).submitDate());
        }

        @Test
        @DisplayName("sort=submitDate DESC → 提出日降順")
        void sortSubmitDateDesc() {
            when(viewEdinetUseCase.viewMain()).thenReturn(List.of(
                    edinet(LocalDate.of(2025, 1, 1), 10L),
                    edinet(LocalDate.of(2025, 6, 1), 20L)
            ));
            final EdinetListTablePage page = service.findEdinetListTable(
                    new EdinetListTableQuery(null, null,
                            PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "submitDate"))));
            assertEquals(LocalDate.of(2025, 6, 1), page.rows().get(0).submitDate());
            assertEquals(LocalDate.of(2025, 1, 1), page.rows().get(1).submitDate());
        }
    }
}
