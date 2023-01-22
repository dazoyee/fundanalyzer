package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum SourceOfStockPrice {

    NIKKEI("1", "日経"),
    KABUOJI3("2", "kabuoji3"),
    YAHOO_FINANCE("3", "Yahoo!ファイナンス"),
    MINKABU("4", "みんかぶ"),
    ;

    private final String value;

    private final String memo;

    SourceOfStockPrice(final String value, final String memo) {
        this.value = value;
        this.memo = memo;
    }

    @JsonCreator
    public static SourceOfStockPrice fromValue(final String value) {
        return Arrays.stream(values())
                .filter(v -> v.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(value)));
    }

    @JsonValue
    public String toValue() {
        return this.value;
    }

    @JsonValue
    public String getMemo() {
        return memo;
    }

    @Override
    public String toString() {
        return "SourceOfStockPrice{" +
                "value='" + value + '\'' +
                ", memo='" + memo + '\'' +
                '}';
    }
}
