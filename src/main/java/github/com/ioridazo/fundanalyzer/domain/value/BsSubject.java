package github.com.ioridazo.fundanalyzer.domain.value;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.BsSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.Subject;
import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public class BsSubject extends Subject {

    private final String id;

    private final String outlineSubjectId;

    private final String detailSubjectId;

    private final String name;

    public static BsSubject of(final BsSubjectEntity entity) {
        return new BsSubject(
                entity.getId(),
                entity.getOutlineSubjectId(),
                entity.getDetailSubjectId().orElse(null),
                entity.getName()
        );
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getOutlineSubjectId() {
        return outlineSubjectId;
    }

    @Override
    public String getDetailSubjectId() {
        return detailSubjectId;
    }

    @Override
    public String getName() {
        return name;
    }

    public enum BsEnum {
        //    CASH("現金及び預金"),
//    ("信託現金及び信託預金"),
//    ("営業未収入金"),
//    ("リース投資資産"),
//    ("前払費用"),
//    ("その他"),
//    ("貸倒引当金"),
        TOTAL_CURRENT_ASSETS("1", "流動資産合計"),
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
        TOTAL_INVESTMENTS_AND_OTHER_ASSETS("4", "投資その他の資産合計"),
        //    ("固定資産合計"),
//    ("投資法人債発行費"),
//    ("繰延資産合計"),
        TOTAL_ASSETS("7", "資産合計"),
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
        TOTAL_CURRENT_LIABILITIES("8", "流動負債合計"),
        //    ("投資法人債"),
//    ("長期借入金"),
//    ("預り敷金及び保証金"),
//    ("資産除去債務"),
        TOTAL_FIXED_LIABILITIES("9", "固定負債合計"),
        TOTAL_LIABILITIES("10", "負債合計"),
        //    ("出資総額"),
//    ("圧縮積立金"),
//    ("任意積立金合計"),
//    ("当期未処分利益又は当期未処理損失（△）"),
//    ("剰余金合計"),
//    ("投資主資本合計"),
        SUBSCRIPTION_WARRANT("16", "新株予約権"),
        TOTAL_NET_ASSETS("14", "純資産合計"),
//    ("負債純資産合計"),
        ;

        private final String outlineSubjectId;
        private final String subject;

        BsEnum(final String outlineSubjectId, final String subject) {
            this.outlineSubjectId = outlineSubjectId;
            this.subject = subject;
        }

        public static BsEnum fromValue(final String subject) {
            return Arrays.stream(values())
                    .filter(v -> v.subject.equals(subject))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.valueOf(subject)));
        }

        public String getOutlineSubjectId() {
            return this.outlineSubjectId;
        }

        public String getSubject() {
            return this.subject;
        }
    }
}
