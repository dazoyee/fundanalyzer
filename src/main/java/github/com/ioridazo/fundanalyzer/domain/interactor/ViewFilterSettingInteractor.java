package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.ViewFilterSettingEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewFilterSettingSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewFilterSettingUseCase;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerBadDataException;
import github.com.ioridazo.fundanalyzer.web.model.ViewFilterSettingInputData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ViewFilterSettingInteractor implements ViewFilterSettingUseCase {

    // DB列 discount_rate/outlier_of_standard_deviation/diff_forecast_stock は DECIMAL(10,2)
    private static final BigDecimal DECIMAL_SCALE2_MAX = new BigDecimal("99999999.99");
    // DB列 coefficient_of_variation は DECIMAL(10,3)
    private static final BigDecimal DECIMAL_SCALE3_MAX = new BigDecimal("9999999.999");
    // 提出日の表示範囲として現実的な上限（10年）
    private static final int CORPORATE_SIZE_MAX = 3650;

    private final ViewFilterSettingSpecification viewFilterSettingSpecification;

    public ViewFilterSettingInteractor(final ViewFilterSettingSpecification viewFilterSettingSpecification) {
        this.viewFilterSettingSpecification = viewFilterSettingSpecification;
    }

    @Override
    public ViewFilterSettingEntity getSetting() {
        return viewFilterSettingSpecification.findSetting();
    }

    @Override
    public void updateSetting(final ViewFilterSettingInputData inputData) {
        final BigDecimal discountRate = parseDecimal(inputData.discountRate(), "割安度", 2, DECIMAL_SCALE2_MAX);
        final BigDecimal outlierOfStandardDeviation =
                parseDecimal(inputData.outlierOfStandardDeviation(), "標準偏差の外れ値", 2, DECIMAL_SCALE2_MAX);
        final BigDecimal coefficientOfVariation =
                parseDecimal(inputData.coefficientOfVariation(), "変動係数", 3, DECIMAL_SCALE3_MAX);
        final BigDecimal diffForecastStock =
                parseDecimal(inputData.diffForecastStock(), "予想株価差", 2, DECIMAL_SCALE2_MAX);
        final Integer corporateSize = parseCorporateSize(inputData.corporateSize());

        viewFilterSettingSpecification.updateSetting(
                discountRate,
                outlierOfStandardDeviation,
                coefficientOfVariation,
                diffForecastStock,
                corporateSize
        );
    }

    private BigDecimal parseDecimal(final String value, final String fieldName, final int scale, final BigDecimal max) {
        if (value == null || value.isBlank()) {
            throw new FundanalyzerBadDataException(fieldName + "を入力してください。", null);
        }
        final BigDecimal decimal;
        try {
            decimal = new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new FundanalyzerBadDataException(fieldName + "は数値で入力してください。", e);
        }
        if (decimal.compareTo(BigDecimal.ZERO) < 0) {
            throw new FundanalyzerBadDataException(fieldName + "は0以上で入力してください。", null);
        }
        if (decimal.scale() > scale) {
            throw new FundanalyzerBadDataException(fieldName + "は小数第" + scale + "位までで入力してください。", null);
        }
        if (decimal.compareTo(max) > 0) {
            throw new FundanalyzerBadDataException(fieldName + "は" + max + "以下で入力してください。", null);
        }
        return decimal;
    }

    private Integer parseCorporateSize(final String corporateSize) {
        if (corporateSize == null || corporateSize.isBlank()) {
            throw new FundanalyzerBadDataException("提出日の表示範囲(日数)を入力してください。", null);
        }
        final Integer size;
        try {
            size = Integer.valueOf(corporateSize);
        } catch (NumberFormatException e) {
            throw new FundanalyzerBadDataException("提出日の表示範囲(日数)は整数で入力してください。", e);
        }
        if (size < 1) {
            throw new FundanalyzerBadDataException("提出日の表示範囲(日数)は1以上で入力してください。", null);
        }
        if (size > CORPORATE_SIZE_MAX) {
            throw new FundanalyzerBadDataException("提出日の表示範囲(日数)は" + CORPORATE_SIZE_MAX + "以下で入力してください。", null);
        }
        return size;
    }
}
