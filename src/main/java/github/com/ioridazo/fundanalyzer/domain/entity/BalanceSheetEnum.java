package github.com.ioridazo.fundanalyzer.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum BalanceSheetEnum {
    //    CASH("現金及び預金"),
//    ("信託現金及び信託預金"),
//    ("営業未収入金"),
//    ("リース投資資産"),
//    ("前払費用"),
//    ("その他"),
//    ("貸倒引当金"),
    TOTAL_CURRENT_ASSETS("流動資産合計"),
    //    BUILDINGS("建物"),
//    ("減価償却累計額"),
//    ("建物（純額）"),
//    ("建物附属設備"),
//    ("減価償却累計額"),
//    ("建物附属設備（純額）"),
//    STRUCTURES("構築物"),
//    ("減価償却累計額"),
//    ("構築物（純額）"),
//    ("機械及び装置"),
//    ("減価償却累計額"),
//    ("機械及び装置（純額）"),
//    ("工具、器具及び備品"),
//    ("減価償却累計額                  ,
//    ("工具、器具及び備品（純額）"),
//    LAND("土地"),
//    ("建設仮勘定"),
//    ("信託建物"),
//    ("減価償却累計額"),
//    ("信託建物（純額）"),
//    ("信託建物附属設備"),
//    ("減価償却累計額"),
//    ("信託建物附属設備（純額）"),
//    ("信託構築物"),
//    ("減価償却累計額"),
//    ("信託構築物（純額）"),
//    ("信託機械及び装置"),
//    ("減価償却累計額"),
//    ("信託機械及び装置（純額）"),
//    ("信託工具、器具及び備品"),
//    ("減価償却累計額"),
//    ("信託工具、器具及び備品（純額）"),
//    ("信託土地"),
//    ("信託建設仮勘定"),
//    ("有形固定資産合計"),
//    ("借地権"),
//    ("信託借地権"),
//    ("その他"),
//    ("無形固定資産合計"),
//    ("修繕積立金"),
//    ("敷金及び保証金"),
//    ("信託差入敷金及び保証金"),
//    ("長期前払費用"),
    TOTAL_INVESTMENTS_AND_OTHER_ASSETS("投資その他の資産合計"),
    //    ("固定資産合計"),
//    ("投資法人債発行費"),
//    ("繰延資産合計"),
//    ("資産合計"),
//    ("営業未払金"),
//    ("短期借入金"),
//    ("1年内返済予定の長期借入金"),
//    ("1年内償還予定の投資法人債"),
//    ("未払金"),
//    ("未払費用"),
//    ("未払法人税等"),
//    ("未払消費税等"),
//    ("前受金"),
//    ("その他"),
    TOTAL_CURRENT_LIABILITIES("流動負債合計"),
    //    ("投資法人債"),
//    ("長期借入金"),
//    ("預り敷金及び保証金"),
//    ("資産除去債務"),
    TOTAL_FIXED_LIABILITIES("固定負債合計"),
//    ("負債合計"),
//    ("出資総額"),
//    ("圧縮積立金"),
//    ("任意積立金合計"),
//    ("当期未処分利益又は当期未処理損失（△）"),
//    ("剰余金合計"),
//    ("投資主資本合計"),
//    ("純資産合計"),
//    ("負債純資産合計"),
    ;

    private final String subject;

    BalanceSheetEnum(String subject) {
        this.subject = subject;
    }

    @JsonCreator
    public static BalanceSheetEnum fromValue(String subject) {
        return Arrays.stream(values())
                .filter(v -> v.subject.equals(subject))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.valueOf(subject)));
    }

    @JsonValue
    public String toValue() {
        return this.subject;
    }

    @Override
    public String toString() {
        return String.format("BalanceSheetEnum[code = %s]", this.subject);
    }
}
