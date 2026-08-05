package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.usecase.AnalyzeUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.SystemEventUseCase;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DashboardPresenterのテスト")
class DashboardPresenterTest {

    private ViewService viewService;
    private AnalyzeUseCase analyzeUseCase;
    private SystemEventUseCase systemEventUseCase;
    private DashboardPresenter presenter;

    @BeforeEach
    void setUp() {
        this.viewService = mock(ViewService.class);
        this.analyzeUseCase = mock(AnalyzeUseCase.class);
        this.systemEventUseCase = mock(SystemEventUseCase.class);
        this.presenter = new DashboardPresenter(viewService, analyzeUseCase, systemEventUseCase);
    }

    @Nested
    @DisplayName("dashboardViewV3 メソッド")
    class DashboardViewV3 {

        @Test
        @DisplayName("フルページ表示では dashboard-v2 と KPI 属性を返す")
        void returnsDashboardViewWithKpis() {
            final ExtendedModelMap model = new ExtendedModelMap();
            final CompanyTablePage companyTable = new CompanyTablePage(List.of(), 0, 123L, 0, 1, Sort.by("code"));
            final EdinetListViewModel edinet = EdinetListViewModel.of(
                    LocalDate.parse("2026-07-21"), 10, 8, 7, 6, "", "", 1);
            final EdinetListTablePage edinetTable = new EdinetListTablePage(
                    List.of(edinet), 1, 1L, 0, 1, Sort.by(Sort.Direction.DESC, "submitDate"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(companyTable);
            when(viewService.findEdinetListTable(any(EdinetListTableQuery.class))).thenReturn(edinetTable);
            when(analyzeUseCase.countAnalyzed()).thenReturn(77);
            when(systemEventUseCase.countRecentByType(SystemEventType.ERROR, 20)).thenReturn(3L);
            when(systemEventUseCase.countRecentByType(SystemEventType.WARNING, 20)).thenReturn(5L);

            final String result = presenter.dashboardViewV3(model);

            assertEquals("dashboard-v2", result);
            assertEquals(123L, model.getAttribute("companyCount"));
            assertEquals(77, model.getAttribute("analyzedCount"));
            assertEquals(edinet, model.getAttribute("latestEdinet"));
            assertEquals(3L, model.getAttribute("recentErrorCount"));
            assertEquals(5L, model.getAttribute("recentWarningCount"));
        }

        @Test
        @DisplayName("会社件数と EDINET は先頭1件取得の問い合わせで呼ばれる")
        void usesSingleRowQueries() {
            final Model model = mock(Model.class);
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(
                    new CompanyTablePage(List.of(), 0, 0L, 0, 1, Sort.by("code")));
            when(viewService.findEdinetListTable(any(EdinetListTableQuery.class))).thenReturn(
                    new EdinetListTablePage(List.of(), 0, 0L, 0, 1, Sort.by("submitDate")));
            when(analyzeUseCase.countAnalyzed()).thenReturn(0);
            when(systemEventUseCase.countRecentByType(SystemEventType.ERROR, 20)).thenReturn(0L);
            when(systemEventUseCase.countRecentByType(SystemEventType.WARNING, 20)).thenReturn(0L);

            presenter.dashboardViewV3(model);

            final ArgumentCaptor<CompanyTableQuery> companyCaptor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            final ArgumentCaptor<EdinetListTableQuery> edinetCaptor = ArgumentCaptor.forClass(EdinetListTableQuery.class);
            verify(viewService).findCompanyTable(companyCaptor.capture());
            verify(viewService).findEdinetListTable(edinetCaptor.capture());
            assertEquals("all", companyCaptor.getValue().target());
            assertEquals(1, companyCaptor.getValue().pageable().getPageSize());
            assertEquals("all", edinetCaptor.getValue().target());
            assertEquals(1, edinetCaptor.getValue().pageable().getPageSize());
        }
    }

    @Nested
    @DisplayName("dashboardKpi メソッド")
    class DashboardKpi {

        @Test
        @DisplayName("KPI フラグメントを返す")
        void returnsKpiFragment() {
            final Model model = mock(Model.class);
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(
                    new CompanyTablePage(List.of(), 0, 0L, 0, 1, Sort.by("code")));
            when(viewService.findEdinetListTable(any(EdinetListTableQuery.class))).thenReturn(
                    new EdinetListTablePage(List.of(), 0, 0L, 0, 1, Sort.by("submitDate")));
            when(analyzeUseCase.countAnalyzed()).thenReturn(0);
            when(systemEventUseCase.countRecentByType(SystemEventType.ERROR, 20)).thenReturn(0L);
            when(systemEventUseCase.countRecentByType(SystemEventType.WARNING, 20)).thenReturn(0L);

            final String result = presenter.dashboardKpi(model);

            assertEquals("fragments/dashboard-kpi :: kpi-grid", result);
        }
    }
}
