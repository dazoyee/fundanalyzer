package github.com.ioridazo.fundanalyzer.domain.bean;

import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class CompanyViewBean {

    // 提出日
    private final LocalDate submitDate;

    // 証券コード
    private final String code;

    // 会社名
    private final String name;

    // 企業価値
    private final BigDecimal corporateValue;

    // 対象年
    private final Integer period;
}
