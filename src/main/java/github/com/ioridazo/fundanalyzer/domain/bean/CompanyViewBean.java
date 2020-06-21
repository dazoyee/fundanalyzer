package github.com.ioridazo.fundanalyzer.domain.bean;

import lombok.Value;

import java.math.BigDecimal;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class CompanyViewBean {

    // 証券コード
    private final String code;

    // 会社名
    private final String name;

    // 企業価値
    private final BigDecimal corporateValue;

    // 対象年
    private final Integer period;
}
