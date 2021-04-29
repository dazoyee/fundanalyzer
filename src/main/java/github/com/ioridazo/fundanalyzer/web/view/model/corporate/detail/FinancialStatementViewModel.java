package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import lombok.Value;

import java.util.List;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class FinancialStatementViewModel {
    private final FinancialStatementKeyViewModel key;
    private final List<FinancialStatementValueViewModel> bs;
    private final List<FinancialStatementValueViewModel> pl;
}
