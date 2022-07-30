package github.com.ioridazo.fundanalyzer.web.view.model.valuation;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class ValuationViewModel {

    // 証券コード
    private final String code;

    // 会社（業種）名
    private final String name;

    // 対象日
    private final LocalDate targetDate;

    // 株価終値
    private final BigDecimal stockPrice;

    // 提出日との差
    private final BigDecimal differenceFromSubmitDate;

    // 提出日比率
    private final BigDecimal submitDateRatio;

    // 提出日
    private final LocalDate submitDate;

    // 提出日株価平均
    private final BigDecimal averageStockPrice;

    // 最新企業価値
    private final BigDecimal latestCorporateValue;

    // 割安度
    private final BigDecimal discountRate;

    public static ValuationViewModel of(final ValuationEntity entity, final Company company) {
        return new ValuationViewModel(
                entity.getCompanyCode().length() == 5 ? entity.getCompanyCode().substring(0, 4) : entity.getCompanyCode(),
                company.getCompanyName(),
                entity.getTargetDate(),
                entity.getStockPrice(),
                entity.getDifferenceFromSubmitDate(),
                entity.getSubmitDateRatio(),
                entity.getSubmitDate(),
                entity.getStockPriceOfSubmitDate(),
                entity.getCorporateValue(),
                entity.getDiscountRate()
        );
    }

    public static ValuationViewModel ofIndustry(
            final String industryName,
            final double differenceFromSubmitDate,
            final double submitDateRatio) {
        return new ValuationViewModel(
                null,
                industryName,
                null,
                null,
                BigDecimal.valueOf(differenceFromSubmitDate).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(submitDateRatio).setScale(2, RoundingMode.HALF_UP),
                null,
                null,
                null,
                null
        );
    }
}
