package github.com.ioridazo.fundamentalanalysis.domain.entity;

import lombok.Value;

@Value
public class EdinetDocument {

    private String docID;

    // 提出者EDINETコード
    private String edinetCode;

    // 提出者証券コード
    private String secCode;

    // 提出者法人番号
    private String jcn;

    // 提出者名
    private String filerName;

    // ファンドコード
    private String fundCode;

    // 府令コード
    private String ordinanceCode;

    // 様式コード
    private String formCode;

    // 書類種別コード
    private String docTypeCode;

    // 期間（自）
    private String periodStart;

    // 期間（至）
    private String periodEnd;

    // 提出日時
    private String submitDateTime;

    // 提出書類概要
    private String docDescription;

    //発行会社EDINETコード
    private String issuerEdinetCode;

    // 対象EDINETコード
    private String subjectEdinetCode;

    // 小会社EDINETコード
    private String subsidiaryEdinetCode;

    // 臨報提出事由
    private String currentReportReason;

    // 親書類管理番号
    private String parentDocID;

    // 操作日時
    private String opeDateTime;

    // 取下区分
    private String withdrawalStatus;

    // 書類情報修正区分
    private String docInfoEditStatus;

    // 開示不開示区分
    private String disclosureStatus;

    // XBRL有無フラグ
    private String xbrlFlag;

    // PDF有無フラグ
    private String pdfFlag;

    // 代替書面・添付文書有無フラグ
    private String attachDocFlag;

    // 英文ファイル有無フラグ
    private String englishDocFlag;
}
