package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class FinancialStatementViewModel {
    private final LocalDate submitDate;
    private final FinancialStatementKeyViewModel key;
    private final List<FinancialStatementValueViewModel> bs;
    private final List<FinancialStatementValueViewModel> pl;
}
