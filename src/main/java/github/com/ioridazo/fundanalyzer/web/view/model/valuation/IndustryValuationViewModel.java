package github.com.ioridazo.fundanalyzer.web.view.model.valuation;

import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class IndustryValuationViewModel {

    // 業種名
    private final String name;

    // 提出日との差
    private final BigDecimal differenceFromSubmitDate;

    // 提出日比率
    private final BigDecimal submitDateRatio;

    private final Integer count;

    public static IndustryValuationViewModel of(
            final String industryName,
            final double differenceFromSubmitDate,
            final double submitDateRatio,
            final int count) {
        return new IndustryValuationViewModel(
                industryName,
                BigDecimal.valueOf(differenceFromSubmitDate).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(submitDateRatio).setScale(2, RoundingMode.HALF_UP),
                count
        );
    }
}
