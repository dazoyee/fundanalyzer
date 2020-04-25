package github.com.ioridazo.fundanalyzer.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum FinancialStatementEnum {

    BALANCE_SHEET("1", "貸借対照表"),
    PROFIT_AND_LESS_STATEMENT("2", "損益計算書"),
    CASH_FLOW_STATEMENT("3", "キャッシュ・フロー計算書"),

    ;

    private final String id;

    private final String name;

    FinancialStatementEnum(String id, String name) {
        this.id = id;
        this.name = name;
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

    @Override
    public String toString() {
        return String.format("DocTypeCode[id = %s]", this.id);
    }
}
