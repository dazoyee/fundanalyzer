package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDateTime;

@Value
@Entity(immutable = true)
@Table(name = "edinet_document")
public class EdinetDocument {

    @Id
    String docId;

    // 提出者EDINETコード
    String edinetCode;

    // 提出者証券コード
    String secCode;

    // 提出者法人番号
    String jcn;

    // 提出者名
    String filerName;

    // ファンドコード
    String fundCode;

    // 府令コード
    String ordinanceCode;

    // 様式コード
    String formCode;

    // 書類種別コード
    String docTypeCode;

    // 期間（自）
    String periodStart;

    // 期間（至）
    String periodEnd;

    // 提出日時
    String submitDateTime;

    // 提出書類概要
    String docDescription;

    //発行会社EDINETコード
    String issuerEdinetCode;

    // 対象EDINETコード
    String subjectEdinetCode;

    // 小会社EDINETコード
    String subsidiaryEdinetCode;

    // 臨報提出事由
    String currentReportReason;

    // 親書類管理番号
    String parentDocID;

    // 操作日時
    String opeDateTime;

    // 取下区分
    String withdrawalStatus;

    // 書類情報修正区分
    String docInfoEditStatus;

    // 開示不開示区分
    String disclosureStatus;

    // XBRL有無フラグ
    String xbrlFlag;

    // PDF有無フラグ
    String pdfFlag;

    // 代替書面・添付文書有無フラグ
    String attachDocFlag;

    // 英文ファイル有無フラグ
    String englishDocFlag;

    //登録日
    @Column(updatable = false)
    LocalDateTime insertDate;
}
