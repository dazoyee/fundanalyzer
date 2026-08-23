package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;

import java.time.LocalDate;

public record FinancialStatementKeyViewModel(
        LocalDate periodStart,
        LocalDate periodEnd,
        String documentTypeCode,
        String documentTypeName,
        LocalDate submitDate
) {

    /**
     * 財務諸表ブロックの見出しに表示する期間の説明を返す。
     * <p>
     * 半期報告書の期間は EDINET のメタデータ上は事業年度そのものが入るため、そのまま並べると
     * 書類が実際に対象とする中間期と誤解される。事業年度であることを明示して区別する。
     *
     * @return 期間の説明
     */
    public String periodLabel() {
        if (isSemiannual()) {
            return "事業年度 " + periodStart + " 〜 " + periodEnd + " の中間期";
        }
        return periodStart + " 〜 " + periodEnd;
    }

    private boolean isSemiannual() {
        final DocumentTypeCode type = DocumentTypeCode.fromValue(documentTypeCode);
        return DocumentTypeCode.DTC_160.equals(type) || DocumentTypeCode.DTC_170.equals(type);
    }

    public static FinancialStatementKeyViewModel of(final FinancialStatementEntity entity) {
        final DocumentTypeCode documentTypeCode = DocumentTypeCode.fromValue(entity.getDocumentTypeCode());
        return new FinancialStatementKeyViewModel(
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                documentTypeCode.toValue(),
                documentTypeCode.getName(),
                entity.getSubmitDate()
        );
    }
}
