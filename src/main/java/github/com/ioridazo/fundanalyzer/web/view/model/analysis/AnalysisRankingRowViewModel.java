package github.com.ioridazo.fundanalyzer.web.view.model.analysis;

import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnalysisRankingRowViewModel(
        String code,
        String name,
        BigDecimal discountRate,
        BigDecimal grahamIndex,
        LocalDate targetDate,
        boolean stale
) {

    public static AnalysisRankingRowViewModel of(
            final CompanyValuationViewModel valuationViewModel,
            final boolean stale) {
        return new AnalysisRankingRowViewModel(
                valuationViewModel.code(),
                valuationViewModel.name(),
                valuationViewModel.discountRate(),
                valuationViewModel.grahamIndex(),
                valuationViewModel.targetDate(),
                stale
        );
    }
}
