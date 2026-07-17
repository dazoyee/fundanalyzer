package github.com.ioridazo.fundanalyzer.web.model;

public record ViewFilterSettingInputData(
        String discountRate,
        String outlierOfStandardDeviation,
        String coefficientOfVariation,
        String diffForecastStock,
        String corporateSize
) {
}
