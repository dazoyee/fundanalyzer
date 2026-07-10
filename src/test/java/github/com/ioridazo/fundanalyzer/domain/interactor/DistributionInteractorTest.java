package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.IndustryEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.IndustrySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.DistributionResult;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DistributionInteractor のテスト")
class DistributionInteractorTest {

    private ViewSpecification viewSpecification;
    private IndustrySpecification industrySpecification;
    private DistributionInteractor interactor;

    @BeforeEach
    void setUp() {
        viewSpecification = mock(ViewSpecification.class);
        industrySpecification = mock(IndustrySpecification.class);
        interactor = new DistributionInteractor(viewSpecification, industrySpecification);
        interactor.discountBins = List.of(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(150),
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(300)
        );
        interactor.grahamBins = List.of(
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(22.5),
                BigDecimal.valueOf(40),
                BigDecimal.valueOf(70)
        );
    }

    @Nested
    @DisplayName("distribution メソッド")
    class Distribution {

        @Test
        @DisplayName("distribution : 全体ヒストグラムと中央値を集計し、対象外業種と最小件数未満の業種を除外する")
        void aggregatesAndFiltersIndustries() {
            final IndustryEntity targetIncluded = new IndustryEntity(1, "対象業種A", null);
            final IndustryEntity targetExcludedBySize = new IndustryEntity(2, "対象業種B", null);
            final IndustryEntity nonTarget = new IndustryEntity(3, "対象外業種", null);

            when(viewSpecification.findAllCompanyValuationView()).thenReturn(List.of(
                    valuation("1000", BigDecimal.valueOf(0.9), BigDecimal.valueOf(5)),
                    valuation("1001", BigDecimal.valueOf(1.2), null),
                    valuation("1002", null, BigDecimal.valueOf(30)),
                    valuation("1003", BigDecimal.valueOf(2.5), BigDecimal.valueOf(80))
            ));
            when(industrySpecification.inquiryIndustryList()).thenReturn(List.of(
                    targetIncluded, targetExcludedBySize, nonTarget
            ));
            when(industrySpecification.isTarget(1)).thenReturn(true);
            when(industrySpecification.isTarget(2)).thenReturn(true);
            when(industrySpecification.isTarget(3)).thenReturn(false);
            when(viewSpecification.findCompanyValuationViewList(1)).thenReturn(List.of(
                    valuation("2000", BigDecimal.valueOf(1.0), BigDecimal.valueOf(10)),
                    valuation("2001", BigDecimal.valueOf(1.3), null),
                    valuation("2002", BigDecimal.valueOf(2.0), BigDecimal.valueOf(30))
            ));
            when(viewSpecification.findCompanyValuationViewList(2)).thenReturn(List.of(
                    valuation("3000", BigDecimal.valueOf(1.5), BigDecimal.valueOf(20)),
                    valuation("3001", BigDecimal.valueOf(1.6), BigDecimal.valueOf(40))
            ));

            final DistributionResult actual = interactor.distribution();

            assertAll(
                    () -> assertEquals(6, actual.discountHistogram().size()),
                    () -> assertEquals(6, actual.grahamHistogram().size()),
                    () -> assertEquals(1L, actual.discountHistogram().get(0).count()),
                    () -> assertEquals(0L, actual.discountHistogram().get(1).count()),
                    () -> assertEquals(1L, actual.discountHistogram().get(2).count()),
                    () -> assertEquals(0L, actual.discountHistogram().get(3).count()),
                    () -> assertEquals(1L, actual.discountHistogram().get(4).count()),
                    () -> assertEquals(0L, actual.discountHistogram().get(5).count()),
                    () -> assertEquals(1L, actual.grahamHistogram().get(0).count()),
                    () -> assertEquals(0L, actual.grahamHistogram().get(1).count()),
                    () -> assertEquals(0L, actual.grahamHistogram().get(2).count()),
                    () -> assertEquals(1L, actual.grahamHistogram().get(3).count()),
                    () -> assertEquals(0L, actual.grahamHistogram().get(4).count()),
                    () -> assertEquals(1L, actual.grahamHistogram().get(5).count()),
                    () -> assertEquals(120.0, actual.discountMedian()),
                    () -> assertEquals(1, actual.industries().size()),
                    () -> assertEquals("対象業種A", actual.industries().get(0).industryName()),
                    () -> assertEquals(130.0, actual.industries().get(0).discountMedian()),
                    () -> assertEquals(3L, actual.industries().get(0).count()),
                    () -> assertTrue(actual.industries().stream()
                            .noneMatch(row -> "対象業種B".equals(row.industryName()))),
                    () -> assertTrue(actual.industries().stream()
                            .noneMatch(row -> "対象外業種".equals(row.industryName())))
            );

            verify(viewSpecification).findAllCompanyValuationView();
            verify(industrySpecification).inquiryIndustryList();
            verify(viewSpecification).findCompanyValuationViewList(1);
            verify(viewSpecification).findCompanyValuationViewList(2);
            verify(viewSpecification, never()).findCompanyValuationViewList(3);
        }
    }

    private CompanyValuationViewModel valuation(
            final String code,
            final BigDecimal discountRate,
            final BigDecimal grahamIndex) {
        return new CompanyValuationViewModel(
                code,
                "テスト企業",
                LocalDate.parse("2024-04-01"),
                BigDecimal.valueOf(1000),
                grahamIndex,
                BigDecimal.valueOf(500),
                discountRate,
                LocalDate.parse("2024-03-01"),
                BigDecimal.valueOf(900),
                10L,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1.1),
                BigDecimal.valueOf(0.4),
                BigDecimal.valueOf(1500),
                BigDecimal.valueOf(2.0)
        );
    }
}
