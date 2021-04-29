package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import lombok.Value;

import java.util.List;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class CorporateDetailViewModel {

    private final CompanyViewModel company;

    private final CorporateViewModel corporate;

    private final List<AnalysisResultViewModel> analysisResultList;

    private final List<FinancialStatementViewModel> financialStatement;

    private final List<MinkabuViewModel> minkabuList;

    private final List<StockPriceViewModel> stockPriceList;
}
