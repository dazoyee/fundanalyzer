package github.com.ioridazo.fundanalyzer.web.view.model.valuation;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.ValuationViewBean;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompanyValuationViewModel(

        // 証券コード
        String code,

        // 会社名
        String name,

        // 対象日
        LocalDate targetDate,

        // 株価終値
        BigDecimal stockPrice,

        // グレアム指数
        BigDecimal grahamIndex,

        // 割安値
        BigDecimal discountValue,

        // 割安度
        BigDecimal discountRate,

        // 提出日
        LocalDate submitDate,

        // 提出日の株価終値
        BigDecimal stockPriceOfSubmitDate,

        // 提出日からの日数
        Long daySinceSubmitDate,

        // 提出日との株価の差
        BigDecimal differenceFromSubmitDate,

        // 提出日との株価比率
        BigDecimal submitDateRatio,

        // 提出日のグレアム指数
        BigDecimal grahamIndexOfSubmitDate,

        // 企業価値
        BigDecimal corporateValue,

        // 予想配当利回り
        BigDecimal dividendYield
) {

    public static CompanyValuationViewModel of(final ValuationViewBean viewBean) {
        return new CompanyValuationViewModel(
                viewBean.getCode(),
                viewBean.getName(),
                viewBean.getTargetDate(),
                viewBean.getStockPrice(),
                viewBean.getGrahamIndex().orElse(null),
                viewBean.getDiscountValue(),
                viewBean.getDiscountRate(),
                viewBean.getSubmitDate(),
                viewBean.getStockPriceOfSubmitDate(),
                viewBean.getDaySinceSubmitDate(),
                viewBean.getDifferenceFromSubmitDate(),
                viewBean.getSubmitDateRatio(),
                viewBean.getGrahamIndexOfSubmitDate().orElse(null),
                viewBean.getCorporateValue(),
                viewBean.getDividendYield().orElse(null)
        );
    }

    /**
     * グレアム指数のみ差し替えた新インスタンスを返す（業種内zスコア表示用）。
     *
     * @param newGrahamIndex 差し替えるグレアム指数（業種内zスコア。算出不能時は null）
     * @return グレアム指数を差し替えた新インスタンス
     */
    public CompanyValuationViewModel withGrahamIndex(final BigDecimal newGrahamIndex) {
        return new CompanyValuationViewModel(
                code, name, targetDate, stockPrice, newGrahamIndex, discountValue, discountRate,
                submitDate, stockPriceOfSubmitDate, daySinceSubmitDate, differenceFromSubmitDate,
                submitDateRatio, grahamIndexOfSubmitDate, corporateValue, dividendYield);
    }
}
