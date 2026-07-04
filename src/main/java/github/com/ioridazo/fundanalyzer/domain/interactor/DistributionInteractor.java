package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.IndustryEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.IndustrySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.service.DistributionCalculator;
import github.com.ioridazo.fundanalyzer.domain.usecase.DistributionUseCase;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.DistributionResult;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.IndustryInput;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 分布集計結果を構築する。
 */
@Component
public class DistributionInteractor implements DistributionUseCase {

    private static final int MIN_INDUSTRY_SIZE = 3;

    private final ViewSpecification viewSpecification;
    private final IndustrySpecification industrySpecification;

    @Value("${app.config.analysis.discount-bins}")
    List<BigDecimal> discountBins;
    @Value("${app.config.analysis.graham-bins}")
    List<BigDecimal> grahamBins;

    public DistributionInteractor(
            final ViewSpecification viewSpecification,
            final IndustrySpecification industrySpecification) {
        this.viewSpecification = viewSpecification;
        this.industrySpecification = industrySpecification;
    }

    /**
     * 分布集計結果を返す。
     *
     * @return 分布集計結果
     */
    @Override
    @Cacheable("distribution")
    public DistributionResult distribution() {
        final List<CompanyValuationViewModel> all = viewSpecification.findAllCompanyValuationView();
        final List<Double> discountRatesPercent = all.stream()
                .map(CompanyValuationViewModel::discountRate)
                .filter(Objects::nonNull)
                .map(rate -> rate.doubleValue() * 100.0)
                .toList();
        final List<Double> grahamIndexes = all.stream()
                .map(CompanyValuationViewModel::grahamIndex)
                .map(grahamIndex -> grahamIndex == null ? null : grahamIndex.doubleValue())
                .toList();

        final List<IndustryInput> industries = industrySpecification.inquiryIndustryList().stream()
                .filter(entity -> industrySpecification.isTarget(entity.id()))
                .map(this::toIndustryInput)
                .toList();

        return DistributionCalculator.aggregate(
                discountRatesPercent,
                grahamIndexes,
                industries,
                discountBins,
                grahamBins,
                MIN_INDUSTRY_SIZE
        );
    }

    private IndustryInput toIndustryInput(final IndustryEntity entity) {
        final List<CompanyValuationViewModel> valuations = viewSpecification.findCompanyValuationViewList(entity.id());
        final List<Double> discountRates = valuations.stream()
                .map(CompanyValuationViewModel::discountRate)
                .filter(Objects::nonNull)
                .map(rate -> rate.doubleValue() * 100.0)
                .toList();
        final List<Double> grahamIndexes = valuations.stream()
                .map(CompanyValuationViewModel::grahamIndex)
                .map(grahamIndex -> grahamIndex == null ? null : grahamIndex.doubleValue())
                .toList();
        return new IndustryInput(entity.name(), discountRates, grahamIndexes);
    }
}
