package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum DocumentTypeCode {

    DTC_010("010", "有価証券通知書"),
    DTC_020("020", "変更通知書（有価証券通知書）"),
    DTC_030("030", "有価証券届出書"),
    DTC_040("040", "訂正有価証券届出書"),
    DTC_050("050", "届出の取下げ願い"),
    DTC_060("060", "発行登録通知書"),
    DTC_070("070", "変更通知書（発行登録通知書）"),
    DTC_080("080", "発行登録書"),
    DTC_090("090", "訂正発行登録書"),
    DTC_100("100", "発行登録追補書類"),
    DTC_110("110", "発行登録取下届出書"),
    DTC_120("120", "有価証券報告書"),
    DTC_130("130", "訂正有価証券報告書"),
    DTC_135("135", "確認書"),
    DTC_136("136", "訂正確認書"),
    DTC_140("140", "四半期報告書"),
    DTC_150("150", "訂正四半期報告書"),
    DTC_160("160", "半期報告書"),
    DTC_170("170", "訂正半期報告書"),
    DTC_180("180", "臨時報告書"),
    DTC_190("190", "訂正臨時報告書"),
    DTC_200("200", "親会社等状況報告書"),
    DTC_210("210", "訂正親会社等状況報告書"),
    DTC_220("220", "自己株券買付状況報告書"),
    DTC_230("230", "訂正自己株券買付状況報告書"),
    DTC_235("235", "内部統制報告書"),
    DTC_236("236", "訂正内部統制報告書"),
    DTC_240("240", "公開買付届出書"),
    DTC_250("250", "訂正公開買付届出書"),
    DTC_260("260", "公開買付撤回届出書"),
    DTC_270("270", "公開買付報告書"),
    DTC_280("280", "訂正公開買付報告書"),
    DTC_290("290", "意見表明報告書"),
    DTC_300("300", "訂正意見表明報告書"),
    DTC_310("310", "対質問回答報告書"),
    DTC_320("320", "訂正対質問回答報告書"),
    DTC_330("330", "別途買付け禁止の特例を受けるための申出書"),
    DTC_340("340", "訂正別途買付け禁止の特例を受けるための申出書"),
    DTC_350("350", "大量保有報告書"),
    DTC_360("360", "訂正大量保有報告書"),
    DTC_370("370", "基準日の届出書"),
    DTC_380("380", "変更の届出書"),
    DTC_999(null, "定義外エラー"),
    ;

    private final String code;

    private final String name;

    DocumentTypeCode(final String code, final String name) {
        this.code = code;
        this.name = name;
    }

    @JsonCreator
    public static DocumentTypeCode fromValue(final String code) {
        return Arrays.stream(values())
                .filter(v -> v.code.equals(code))
                .findFirst()
                .orElse(DTC_999);
    }

    @JsonValue
    public String toValue() {
        return this.code;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "DocumentTypeCode{" +
               "code='" + code + '\'' +
               ", name='" + name + '\'' +
               '}';
    }
}
