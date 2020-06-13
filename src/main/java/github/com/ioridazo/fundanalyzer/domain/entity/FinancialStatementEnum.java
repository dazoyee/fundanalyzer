package github.com.ioridazo.fundanalyzer.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum FinancialStatementEnum {

    BALANCE_SHEET("1", "貸借対照表", "jpcrp_cor:BalanceSheetTextBlock"),
    CONSOLIDATED_BALANCE_SHEET("1", "連結貸借対照表", "BalanceSheetTextBlock"),
    PROFIT_AND_LESS_STATEMENT("2", "損益計算書", "StatementOfIncomeTextBlock"),
    INCOME_AND_SURPLUS_STATEMENT("2", "損益及び剰余金計算書", "jpcrp_cor:ConsolidatedStatementOfIncomeTextBlock"),
    CASH_FLOW_STATEMENT("3", "キャッシュ・フロー計算書", ""),
    TOTAL_NUMBER_OF_SHARES("4", "株式総数", "株式総数");

    private final String id;

    private final String name;

    private final String keyWord;

    FinancialStatementEnum(String id, String name, String keyWord) {
        this.id = id;
        this.name = name;
        this.keyWord = keyWord;
    }

    @JsonCreator
    public static FinancialStatementEnum fromValue(String code) {
        return Arrays.stream(values())
                .filter(v -> v.id.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(code)));
    }

    @JsonValue
    public String toValue() {
        return this.id;
    }

    @JsonValue
    public String getName() {
        return this.name;
    }

    @JsonValue
    public String getKeyWord() {
        return this.keyWord;
    }

    @Override
    public String toString() {
        return String.format("DocTypeCode[id = %s]", this.id);
    }
}
