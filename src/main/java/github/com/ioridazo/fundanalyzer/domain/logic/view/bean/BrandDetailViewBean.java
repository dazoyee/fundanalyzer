package github.com.ioridazo.fundanalyzer.domain.logic.view.bean;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.MinkabuEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPriceEntity;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class BrandDetailViewBean {

    private final BrandDetailCorporateViewBean corporate;

    private final CorporateViewBean corporateView;

    private final List<AnalysisResultEntity> analysisResultEntityList;

    private final List<BrandDetailFinancialStatement> financialStatement;

    private final List<StockPriceEntity> stockPriceEntityList;

    private final List<MinkabuEntity> minkabuEntityList;

    @Value
    public static class BrandDetailFinancialStatement {
        private final LocalDate periodStart;
        private final LocalDate periodEnd;
        private final String documentTypeName;
        private final List<BrandDetailFinancialStatementValue> bs;
        private final List<BrandDetailFinancialStatementValue> pl;

        @Value
        public static class BrandDetailFinancialStatementValue {
            private final String subject;
            private final Long value;
        }
    }
}
