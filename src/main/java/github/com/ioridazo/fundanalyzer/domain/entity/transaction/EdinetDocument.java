package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDateTime;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "edinet_document")
public class EdinetDocument {

    @Id
    private final String docId;

    // 提出者EDINETコード
    private final String edinetCode;

    // 提出者証券コード
    private final String secCode;

    // 提出者法人番号
    private final String jcn;

    // 提出者名
    private final String filerName;

    // ファンドコード
    private final String fundCode;

    // 府令コード
    private final String ordinanceCode;

    // 様式コード
    private final String formCode;

    // 書類種別コード
    private final String docTypeCode;

    // 期間（自）
    private final String periodStart;

    // 期間（至）
    private final String periodEnd;

    // 提出日時
    private final String submitDateTime;

    // 提出書類概要
    private final String docDescription;

    //発行会社EDINETコード
    private final String issuerEdinetCode;

    // 対象EDINETコード
    private final String subjectEdinetCode;

    // 小会社EDINETコード
    private final String subsidiaryEdinetCode;

    // 臨報提出事由
    private final String currentReportReason;

    // 親書類管理番号
    private final String parentDocID;

    // 操作日時
    private final String opeDateTime;

    // 取下区分
    private final String withdrawalStatus;

    // 書類情報修正区分
    private final String docInfoEditStatus;

    // 開示不開示区分
    private final String disclosureStatus;

    // XBRL有無フラグ
    private final String xbrlFlag;

    // PDF有無フラグ
    private final String pdfFlag;

    // 代替書面・添付文書有無フラグ
    private final String attachDocFlag;

    // 英文ファイル有無フラグ
    private final String englishDocFlag;

    //登録日
    @Column(updatable = false)
    private final LocalDateTime insertDate;
}
