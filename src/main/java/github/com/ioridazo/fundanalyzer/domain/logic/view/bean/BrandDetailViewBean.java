package github.com.ioridazo.fundanalyzer.domain.logic.view.bean;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Minkabu;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPrice;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class BrandDetailViewBean {

    private final BrandDetailCorporateViewBean corporate;

    private final CorporateViewBean corporateView;

    private final List<AnalysisResult> analysisResultList;

    private final List<BrandDetailFinancialStatement> financialStatement;

    private final List<StockPrice> stockPriceList;

    private final List<Minkabu> minkabuList;

    @Value
    public static class BrandDetailFinancialStatement {
        private final LocalDate periodStart;
        private final LocalDate periodEnd;
        private final List<BrandDetailFinancialStatementValue> bs;
        private final List<BrandDetailFinancialStatementValue> pl;

        @Value
        public static class BrandDetailFinancialStatementValue {
            private final String subject;
            private final Long value;
        }
    }
}
