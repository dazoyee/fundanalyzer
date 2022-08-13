package github.com.ioridazo.fundanalyzer.web.view.model.corporate;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.CorporateViewBean;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class CorporateViewModel {

    // 証券コード
    private final String code;

    // 会社名
    private final String name;

    // 提出日
    private final LocalDate submitDate;

    // 最新書類種別コード
    private final String latestDocumentTypeCode;

    // 有価証券報告書フラグ
    private final boolean isMainReport;

    // 最新企業価値
    private final BigDecimal latestCorporateValue;

    // 3年平均企業価値
    private final BigDecimal threeAverageCorporateValue;

    // 3年標準偏差
    private final BigDecimal threeStandardDeviation;

    // 3年変動係数
    private final BigDecimal threeCoefficientOfVariation;

    // 5年平均企業価値
    private final BigDecimal fiveAverageCorporateValue;

    // 5年標準偏差
    private final BigDecimal fiveStandardDeviation;

    // 5年変動係数
    private final BigDecimal fiveCoefficientOfVariation;

    // 10年平均企業価値
    private final BigDecimal tenAverageCorporateValue;

    // 10年標準偏差
    private final BigDecimal tenStandardDeviation;

    // 10年変動係数
    private final BigDecimal tenCoefficientOfVariation;

    // 全平均企業価値
    private final BigDecimal allAverageCorporateValue;

    // 全標準偏差
    private final BigDecimal allStandardDeviation;

    // 全変動係数
    private final BigDecimal allCoefficientOfVariation;

    // 提出日株価平均
    private final BigDecimal averageStockPrice;

    // 株価取得日
    private final LocalDate importDate;

    // 最新株価
    private final BigDecimal latestStockPrice;

    // 3年割安値
    private final BigDecimal threeDiscountValue;

    // 3年割安度
    private final BigDecimal threeDiscountRate;

    // 5年割安値
    private final BigDecimal fiveDiscountValue;

    // 5年割安度
    private final BigDecimal fiveDiscountRate;

    // 10年割安値
    private final BigDecimal tenDiscountValue;

    // 10年割安度
    private final BigDecimal tenDiscountRate;

    // 全割安値
    private final BigDecimal allDiscountValue;

    // 全割安度
    private final BigDecimal allDiscountRate;

    // 対象年カウント
    private final BigDecimal countYear;

    // みんかぶ株価予想
    private final BigDecimal forecastStock;

    public static CorporateViewModel of(final CorporateViewBean viewBean) {
        return new CorporateViewModel(
                viewBean.getCode(),
                viewBean.getName(),
                viewBean.getSubmitDate().orElse(null),
                viewBean.getLatestDocumentTypeCode(),
                Stream.of(DocumentTypeCode.DTC_120, DocumentTypeCode.DTC_130)
                        .map(DocumentTypeCode::toValue)
                        .anyMatch(dtc -> viewBean.getLatestDocumentTypeCode().equals(dtc)),
                viewBean.getLatestCorporateValue().orElse(null),
                viewBean.getThreeAverageCorporateValue().orElse(null),
                viewBean.getThreeStandardDeviation().orElse(null),
                viewBean.getThreeCoefficientOfVariation().orElse(null),
                viewBean.getFiveAverageCorporateValue().orElse(null),
                viewBean.getFiveStandardDeviation().orElse(null),
                viewBean.getFiveCoefficientOfVariation().orElse(null),
                viewBean.getTenAverageCorporateValue().orElse(null),
                viewBean.getTenStandardDeviation().orElse(null),
                viewBean.getTenCoefficientOfVariation().orElse(null),
                viewBean.getAllAverageCorporateValue().orElse(null),
                viewBean.getAllStandardDeviation().orElse(null),
                viewBean.getAllCoefficientOfVariation().orElse(null),
                viewBean.getAverageStockPrice().orElse(null),
                viewBean.getImportDate().orElse(null),
                viewBean.getLatestStockPrice().orElse(null),
                viewBean.getThreeDiscountValue().orElse(null),
                viewBean.getThreeDiscountRate().orElse(null),
                viewBean.getFiveDiscountValue().orElse(null),
                viewBean.getFiveDiscountRate().orElse(null),
                viewBean.getTenDiscountValue().orElse(null),
                viewBean.getTenDiscountRate().orElse(null),
                viewBean.getAllDiscountValue().orElse(null),
                viewBean.getAllDiscountRate().orElse(null),
                viewBean.getCountYear().orElse(null),
                viewBean.getForecastStock().orElse(null)
        );
    }
}
