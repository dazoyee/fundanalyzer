package github.com.ioridazo.fundanalyzer.web.view.model.corporate;

import github.com.ioridazo.fundanalyzer.domain.logic.view.bean.CorporateViewBean;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class CorporateViewModel {

    // 証券コード
    private final String code;

    // 会社名
    private final String name;

    // 提出日
    private final LocalDate submitDate;

    // 最新企業価値
    private final BigDecimal latestCorporateValue;

    // 平均企業価値
    private final BigDecimal averageCorporateValue;

    // 標準偏差
    private final BigDecimal standardDeviation;

    // 変動係数
    private final BigDecimal coefficientOfVariation;

    // 提出日株価平均
    private final BigDecimal averageStockPrice;

    // 株価取得日
    private final LocalDate importDate;

    // 最新株価
    private final BigDecimal latestStockPrice;

    // 割安値
    private final BigDecimal discountValue;

    // 割安度
    private final BigDecimal discountRate;

    // 対象年カウント
    private final BigDecimal countYear;

    // みんかぶ株価予想
    private final BigDecimal forecastStock;

    public static CorporateViewModel of(final CorporateViewBean viewBean) {
        return new CorporateViewModel(
                viewBean.getCode(),
                viewBean.getName(),
                viewBean.getSubmitDate(),
                viewBean.getLatestCorporateValue(),
                viewBean.getAverageCorporateValue(),
                viewBean.getStandardDeviation(),
                viewBean.getCoefficientOfVariation(),
                viewBean.getAverageStockPrice(),
                viewBean.getImportDate(),
                viewBean.getLatestStockPrice(),
                viewBean.getDiscountValue(),
                viewBean.getDiscountRate(),
                viewBean.getCountYear(),
                viewBean.getForecastStock()
        );
    }
}
