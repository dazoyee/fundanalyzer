package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum QuarterType {

    QT_1("1", 1, "第1四半期"),
    QT_2("2", 2, "第2四半期"),
    QT_3("3", 3, "第3四半期"),
    QT_4("4", 4, "第4四半期"),
    QT_OTHER(null, null, "定義外");

    private final String code;

    private final Integer weight;

    private final String memo;

    QuarterType(final String code, final Integer weight, final String memo) {
        this.code = code;
        this.weight = weight;
        this.memo = memo;
    }

    @JsonCreator
    public static QuarterType fromValue(final String code) {
        return Arrays.stream(values())
                .filter(v -> v.code.equals(code))
                .findFirst()
                .orElse(QT_OTHER);
    }

    @JsonCreator
    public static QuarterType fromDocDescription(final String docDescription) {
        try {
            final String code = docDescription.substring(docDescription.indexOf("期第") + 2, docDescription.indexOf("四半期("));
            return Arrays.stream(values())
                    .filter(v -> v.code.equals(code))
                    .findFirst()
                    .orElse(QT_OTHER);
        } catch (NullPointerException | StringIndexOutOfBoundsException e) {
            return QT_OTHER;
        }
    }

    @JsonValue
    public String toValue() {
        return code;
    }

    public Integer getWeight() {
        return weight;
    }

    public String getMemo() {
        return memo;
    }

    @Override
    public String toString() {
        return "QuarterType{" +
                "code='" + code + '\'' +
                ", weight=" + weight +
                ", memo='" + memo + '\'' +
                '}';
    }
}
