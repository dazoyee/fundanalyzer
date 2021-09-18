package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum FinancialStatementEnum {

    BALANCE_SHEET("1", "貸借対照表", "bs"),
    PROFIT_AND_LESS_STATEMENT("2", "損益計算書", "pl"),
    CASH_FLOW_STATEMENT("3", "キャッシュ・フロー計算書", "cf"),
    TOTAL_NUMBER_OF_SHARES("4", "株式総数", "ns");

    private final String id;

    private final String name;

    private final String value;

    FinancialStatementEnum(final String id, final String name, final String value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    @JsonCreator
    public static FinancialStatementEnum fromId(final String id) {
        return Arrays.stream(values())
                .filter(v -> v.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(id)));
    }

    public static Optional<FinancialStatementEnum> fromValue(final String value) {
        return Arrays.stream(values())
                .filter(v -> v.value.equals(value))
                .findFirst();
    }

    @JsonValue
    public String getId() {
        return this.id;
    }

    @JsonValue
    public String getName() {
        return this.name;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "FinancialStatementEnum{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
