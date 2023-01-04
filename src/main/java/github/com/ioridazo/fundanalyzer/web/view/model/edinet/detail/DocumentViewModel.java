package github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail;

import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import lombok.Value;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class DocumentViewModel {
    private final String companyName;
    private final DocumentDetailViewModel document;
    private final FinanceValueViewModel financeValue;

    public static DocumentViewModel of(final Company company, final Document document, final FinanceValueViewModel fundamentalValueViewModel) {
        return new DocumentViewModel(
                company.getCompanyName(),
                DocumentDetailViewModel.of(document),
                fundamentalValueViewModel
        );
    }
}
