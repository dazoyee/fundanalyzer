package github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail;

import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import lombok.Value;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class DocumentViewModel {
    private final String companyName;
    private final DocumentDetailViewModel document;
    private final FinanceValueViewModel fundamentalValue;

    public static DocumentViewModel of(final Company company, final Document document, final FinanceValue financeValue) {
        return new DocumentViewModel(
                company.getCompanyName(),
                DocumentDetailViewModel.of(document),
                FinanceValueViewModel.of(financeValue)
        );
    }
}
