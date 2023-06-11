package github.com.ioridazo.fundanalyzer.domain.value;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.PlSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.Subject;
import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public class PlSubject extends Subject {

    private final String id;

    private final String outlineSubjectId;

    private final String detailSubjectId;

    private final String name;

    public static PlSubject of(final PlSubjectEntity entity) {
        return new PlSubject(
                entity.id(),
                entity.outlineSubjectId(),
                entity.detailSubjectId(),
                entity.name()
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

    public enum PlEnum {
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
        NET_INCOME("11", "当期純利益"),
//    ("前期繰越利益"),
//    ("当期未処分利益又は当期未処理損失（△）"),
        ;

        private final String outlineSubjectId;
        private final String subject;

        PlEnum(final String outlineSubjectId, final String subject) {
            this.outlineSubjectId = outlineSubjectId;
            this.subject = subject;
        }

        public static PlEnum fromValue(final String subject) {
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
