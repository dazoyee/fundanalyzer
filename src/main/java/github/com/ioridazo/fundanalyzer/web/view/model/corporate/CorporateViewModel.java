package github.com.ioridazo.fundanalyzer.web.view.model.corporate;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.CorporateViewBean;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.stream.Stream;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CorporateViewModel {

    // 証券コード
    private String code;

    // 会社名
    private String name;

    // 提出日
    private LocalDate submitDate;

    // 最新書類種別コード
    private String latestDocumentTypeCode;

    // 有価証券報告書フラグ
    private boolean isMainReport;

    // 最新企業価値
    private BigDecimal latestCorporateValue;

    // 3年平均企業価値
    private BigDecimal threeAverageCorporateValue;

    // 3年標準偏差
    private BigDecimal threeStandardDeviation;

    // 3年変動係数
    private BigDecimal threeCoefficientOfVariation;

    // 5年平均企業価値
    private BigDecimal fiveAverageCorporateValue;

    // 5年標準偏差
    private BigDecimal fiveStandardDeviation;

    // 5年変動係数
    private BigDecimal fiveCoefficientOfVariation;

    // 10年平均企業価値
    private BigDecimal tenAverageCorporateValue;

    // 10年標準偏差
    private BigDecimal tenStandardDeviation;

    // 10年変動係数
    private BigDecimal tenCoefficientOfVariation;

    // 全平均企業価値
    private BigDecimal allAverageCorporateValue;

    // 全標準偏差
    private BigDecimal allStandardDeviation;

    // 全変動係数
    private BigDecimal allCoefficientOfVariation;

    // 提出日株価平均
    private BigDecimal averageStockPrice;

    // 株価取得日
    private LocalDate importDate;

    // 最新株価
    private BigDecimal latestStockPrice;

    // 3年割安値
    private BigDecimal threeDiscountValue;

    // 3年割安度
    private BigDecimal threeDiscountRate;

    // 5年割安値
    private BigDecimal fiveDiscountValue;

    // 5年割安度
    private BigDecimal fiveDiscountRate;

    // 10年割安値
    private BigDecimal tenDiscountValue;

    // 10年割安度
    private BigDecimal tenDiscountRate;

    // 全割安値
    private BigDecimal allDiscountValue;

    // 全割安度
    private BigDecimal allDiscountRate;

    // 対象年カウント
    private BigDecimal countYear;

    // みんかぶ株価予想
    private BigDecimal forecastStock;

    // 株価企業価値率
    private BigDecimal priceCorporateValueRatio;

    // PER
    private BigDecimal per;

    // PBR
    private BigDecimal pbr;

    // BPS
    private BigDecimal bps;

    // EPS
    private BigDecimal eps;

    // ROE
    private BigDecimal roe;

    // ROA
    private BigDecimal roa;

    // グレアム指数
    private BigDecimal grahamIndex;

    private static final int THIRD_DECIMAL_PLACE = 3;

    public static CorporateViewModel of(
            final String code,
            final String name,
            final LocalDate submitDate,
            final String latestDocumentTypeCode,
            final boolean isMainReport,
            final BigDecimal latestCorporateValue,
            final BigDecimal threeAverageCorporateValue,
            final BigDecimal threeStandardDeviation,
            final BigDecimal threeCoefficientOfVariation,
            final BigDecimal fiveAverageCorporateValue,
            final BigDecimal fiveStandardDeviation,
            final BigDecimal fiveCoefficientOfVariation,
            final BigDecimal tenAverageCorporateValue,
            final BigDecimal tenStandardDeviation,
            final BigDecimal tenCoefficientOfVariation,
            final BigDecimal allAverageCorporateValue,
            final BigDecimal allStandardDeviation,
            final BigDecimal allCoefficientOfVariation,
            final BigDecimal averageStockPrice,
            final LocalDate importDate,
            final BigDecimal latestStockPrice,
            final BigDecimal threeDiscountValue,
            final BigDecimal threeDiscountRate,
            final BigDecimal fiveDiscountValue,
            final BigDecimal fiveDiscountRate,
            final BigDecimal tenDiscountValue,
            final BigDecimal tenDiscountRate,
            final BigDecimal allDiscountValue,
            final BigDecimal allDiscountRate,
            final BigDecimal countYear,
            final BigDecimal forecastStock,
            final BigDecimal priceCorporateValueRatio,
            final BigDecimal per,
            final BigDecimal pbr,
            final BigDecimal bps,
            final BigDecimal eps,
            final BigDecimal roe,
            final BigDecimal roa,
            final BigDecimal grahamIndex) {
        return new CorporateViewModel(
                code,
                name,
                submitDate,
                latestDocumentTypeCode,
                isMainReport,
                latestCorporateValue,
                threeAverageCorporateValue,
                threeStandardDeviation,
                threeCoefficientOfVariation,
                fiveAverageCorporateValue,
                fiveStandardDeviation,
                fiveCoefficientOfVariation,
                tenAverageCorporateValue,
                tenStandardDeviation,
                tenCoefficientOfVariation,
                allAverageCorporateValue,
                allStandardDeviation,
                allCoefficientOfVariation,
                averageStockPrice,
                importDate,
                latestStockPrice,
                threeDiscountValue,
                threeDiscountRate,
                fiveDiscountValue,
                fiveDiscountRate,
                tenDiscountValue,
                tenDiscountRate,
                allDiscountValue,
                allDiscountRate,
                countYear,
                forecastStock,
                priceCorporateValueRatio,
                per,
                pbr,
                bps,
                eps,
                roe,
                roa,
                grahamIndex
        );
    }

    public static CorporateViewModel of(final CorporateViewBean viewBean) {
        return new CorporateViewModel(
                viewBean.getCode(),
                viewBean.getName(),
                viewBean.getSubmitDate().orElse(null),
                viewBean.getLatestDocumentTypeCode(),
                Stream.of(DocumentTypeCode.DTC_120, DocumentTypeCode.DTC_130)
                        .map(DocumentTypeCode::toValue)
                        .anyMatch(dtc -> viewBean.getLatestDocumentTypeCode().equals(dtc)),
                viewBean.getLatestCorporateValue().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getThreeAverageCorporateValue().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getThreeStandardDeviation().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getThreeCoefficientOfVariation().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getFiveAverageCorporateValue().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getFiveStandardDeviation().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getFiveCoefficientOfVariation().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getTenAverageCorporateValue().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getTenStandardDeviation().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getTenCoefficientOfVariation().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getAllAverageCorporateValue().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getAllStandardDeviation().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getAllCoefficientOfVariation().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getAverageStockPrice().orElse(null),
                viewBean.getImportDate().orElse(null),
                viewBean.getLatestStockPrice().orElse(null),
                viewBean.getThreeDiscountValue().orElse(null),
                viewBean.getThreeDiscountRate().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getFiveDiscountValue().orElse(null),
                viewBean.getFiveDiscountRate().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getTenDiscountValue().orElse(null),
                viewBean.getTenDiscountRate().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getAllDiscountValue().orElse(null),
                viewBean.getAllDiscountRate().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getCountYear().orElse(null),
                viewBean.getForecastStock().orElse(null),
                viewBean.getPriceCorporateValueRatio().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getPer().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getPbr().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getBps().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getEps().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getRoe().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getRoa().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null),
                viewBean.getGrahamIndex().map(v -> v.setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)).orElse(null)
        );
    }
}
