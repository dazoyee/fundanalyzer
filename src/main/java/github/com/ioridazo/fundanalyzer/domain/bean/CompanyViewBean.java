package github.com.ioridazo.fundanalyzer.domain.bean;

import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class CompanyViewBean {

    // 証券コード
    private final String code;

    // 会社名
    private final String name;

    // 提出日
    private final LocalDate submitDate;

    // 企業価値
    private final BigDecimal corporateValue;

    // 標準偏差
    private final BigDecimal standardDeviation;

    // 提出日株価
    private final BigDecimal stockPriceOfSubmitDate;

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
}
