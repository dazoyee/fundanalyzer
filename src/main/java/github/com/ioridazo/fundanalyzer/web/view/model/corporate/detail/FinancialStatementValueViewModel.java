package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import lombok.Value;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class FinancialStatementValueViewModel {
    private final String subject;
    private final Long value;
}
