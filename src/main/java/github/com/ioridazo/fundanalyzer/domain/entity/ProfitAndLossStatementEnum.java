package github.com.ioridazo.fundanalyzer.domain.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProfitAndLossStatementEnum {
    //    ("賃貸事業収入"),
//    ("その他賃貸事業収入"),
//    ("営業収益合計"),
//    ("賃貸事業費用"),
//    ("不動産等売却損"),
//    ("資産運用報酬"),
//    ("資産保管及び一般事務委託手数料"),
//    ("役員報酬"),
//    ("会計監査人報酬"),
//    ("その他営業費用"),
//    ("営業費用合計"),
    OPERATING_PROFIT("3", "営業利益"),
    OPERATING_PROFIT2("4", "営業利益又は営業損失（△）"),
//    ("受取利息"),
//    ("未払分配金戻入"),
//    ("還付加算金"),
//    ("その他"),
//    ("営業外収益合計"),
//    ("支払利息"),
//    ("投資法人債利息"),
//    ("投資法人債発行費償却"),
//    ("融資手数料"),
//    ("その他"),
//    ("営業外費用合計"),
//    ("経常利益"),
//    ("災害による損失"),
//    ("特別損失合計"),
//    ("税引前当期純利益"),
//    ("法人税、住民税及び事業税"),
//    ("法人税等合計"),
//    ("当期純利益"),
//    ("前期繰越利益"),
//    ("当期未処分利益又は当期未処理損失（△）"),
    ;

    private final String id;

    private final String subject;

    ProfitAndLossStatementEnum(final String id, final String subject) {
        this.id = id;
        this.subject = subject;
    }

//    @JsonCreator
//    public static ProfitAndLossStatementEnum fromValue(String subject) {
//        return Arrays.stream(values())
//                .filter(v -> v.subject.equals(subject))
//                .findFirst()
//                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(subject)));
//    }

    @JsonValue
    public String toValue() {
        return this.id;
    }

    @JsonValue
    public String getSubject() {
        return this.subject;
    }

//    @Override
//    public String toString() {
//        return String.format("ProfitAndLossStatementEnum[code = %s]", this.subject);
//    }
}
