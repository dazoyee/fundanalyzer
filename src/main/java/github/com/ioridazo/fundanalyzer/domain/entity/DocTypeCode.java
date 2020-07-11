package github.com.ioridazo.fundanalyzer.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum DocTypeCode {

    SECURITIES_NOTICE("010", "有価証券通知書"),
    CHANGE_SECURITIES_NOTICE("020", "変更通知書（有価証券通知書）"),
    SECURITIES_REGISTRATION_STATEMENT("030", "有価証券届出書"),
    AMENDED_SECURITIES_REGISTRATION_STATEMENT("040", "訂正有価証券届出書"),
    REQUEST_FOR_WITHDRAWAL_OF_NOTIFICATION("050", "届出の取下げ願い"),
    REGISTRATION_STATEMENT_OF_ISSUE("060", "発行登録通知書"),
    CHANGE_REGISTRATION_STATEMENT_OF_ISSUE("070", "変更通知書（発行登録通知書）"),
    REGISTRATION_STATEMENT_FOR_ISSUANCE("080", "発行登録書"),
    AMENDED_ISSUANCE_REGISTRATION_STATEMENT("090", "訂正発行登録書"),
    SUPPLEMENTARY_DOCUMENTS_FOR_ISSUANCE_AND_REGISTRATION("100", "発行登録追補書類"),
    REGISTRATION_STATEMENT_OF_WITHDRAWAL_OF_ISSUANCE("110", "発行登録取下届出書"),
    ANNUAL_SECURITIES_REPORT("120", "有価証券報告書"),
    AMENDED_SECURITIES_REPORT("130", "訂正有価証券報告書"),
    WRITTEN_CONFIRMATION("135", "確認書"),
    CONFIRMATION_OF_CORRECTION("136", "訂正確認書"),
    QUARTERLY_REPORT("140", "四半期報告書"),
    CORRECTED_QUARTERLY_REPORT("150", "訂正四半期報告書"),
    SEMIANNUAL_REPORT("160", "半期報告書"),
    CORRECTED_HALF_YEARLY_REPORT("170", "訂正半期報告書"),
    ;

    private final String code;

    private final String name;

    DocTypeCode(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonCreator
    public static DocTypeCode fromValue(String code) {
        return Arrays.stream(values())
                .filter(v -> v.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(code)));
    }

    @JsonValue
    public String toValue() {
        return this.code;
    }

    @Override
    public String toString() {
        return String.format("DocTypeCode[code = %s]", this.code);
    }
}
