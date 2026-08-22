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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardPresenter {

    private static final String DASHBOARD_V2 = "dashboard-v2";
    private static final String DASHBOARD_KPI_FRAGMENT = "fragments/dashboard-kpi :: kpi-grid";

    @Value("${app.config.view.system-event.size}")
    private int systemEventDays;

    @Value("${app.config.view.system-event.max-count}")
    private int systemEventMaxCount;

    private final ViewService viewService;
    private final AnalyzeUseCase analyzeUseCase;
    private final SystemEventUseCase systemEventUseCase;

    public DashboardPresenter(
            final ViewService viewService,
            final AnalyzeUseCase analyzeUseCase,
            final SystemEventUseCase systemEventUseCase) {
        this.viewService = viewService;
        this.analyzeUseCase = analyzeUseCase;
        this.systemEventUseCase = systemEventUseCase;
    }

    @GetMapping("/v3/dashboard")
    public String dashboardViewV3(final Model model) {
        addCommonAttributes(model);
        return DASHBOARD_V2;
    }

    @GetMapping("/v3/dashboard/kpi")
    public String dashboardKpi(final Model model) {
        addCommonAttributes(model);
        return DASHBOARD_KPI_FRAGMENT;
    }

    private void addCommonAttributes(final Model model) {
        final CompanyTablePage companyTable = viewService.findCompanyTable(new CompanyTableQuery(
                "all",
                null,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "submitDate").and(Sort.by(Sort.Direction.DESC, "code")))
        ));
        final int analyzedCount = analyzeUseCase.countAnalyzed();
        final EdinetListTablePage edinetTable = viewService.findEdinetListTable(new EdinetListTableQuery(
                "all",
                null,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "submitDate"))
        ));
        final EdinetListViewModel latestEdinet = edinetTable.rows().stream().findFirst().orElse(null);

        model.addAttribute("companyCount", companyTable.totalElements());
        model.addAttribute("analyzedCount", analyzedCount);
        model.addAttribute("latestEdinet", latestEdinet);
        model.addAttribute("systemEventDays", systemEventDays);
        model.addAttribute("recentErrorCount", systemEventUseCase.countRecentByType(
                SystemEventType.ERROR, systemEventDays, systemEventMaxCount));
        model.addAttribute("recentWarningCount", systemEventUseCase.countRecentByType(
                SystemEventType.WARNING, systemEventDays, systemEventMaxCount));
    }
}
