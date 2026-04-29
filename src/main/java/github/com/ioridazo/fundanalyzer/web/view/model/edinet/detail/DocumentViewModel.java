package github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail;

import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;

/**
 * EDINET ドキュメントビュー
 *
 * @param companyName  企業名
 * @param document     ドキュメント詳細
 * @param financeValue 財務値
 */
public record DocumentViewModel(
        String companyName,
        DocumentDetailViewModel document,
        FinanceValueViewModel financeValue) {

    /**
     * 静的ファクトリ
     *
     * @param company                    企業
     * @param document                   ドキュメント
     * @param fundamentalValueViewModel  財務値ビュー
     * @return DocumentViewModel
     */
    public static DocumentViewModel of(final Company company, final Document document, final FinanceValueViewModel fundamentalValueViewModel) {
        return new DocumentViewModel(
                company.companyName(),
                DocumentDetailViewModel.of(document),
                fundamentalValueViewModel
        );
    }

    public String getCompanyName() {
        return companyName;
    }

    public DocumentDetailViewModel getDocument() {
        return document;
    }

    public FinanceValueViewModel getFinanceValue() {
        return financeValue;
    }
}
