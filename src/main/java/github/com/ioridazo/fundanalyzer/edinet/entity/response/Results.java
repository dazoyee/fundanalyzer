package github.com.ioridazo.fundanalyzer.edinet.entity.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Optional;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Results {

    // 連番
    @JsonProperty("seqNumber")
    private String seqNumber;

    // 書類管理番号
    @JsonProperty("docID")
    private String docId;

    // 提出者EDINETコード
    @JsonProperty("edinetCode")
    private String edinetCode;

    // 提出者証券コード
    @JsonProperty("secCode")
    private String secCode;

    // 提出者法人番号
    @JsonProperty("JCN")
    private String jcn;

    // 提出者名
    @JsonProperty("filerName")
    private String filerName;

    // ファンドコード
    @JsonProperty("fundCode")
    private String fundCode;

    // 府令コード
    @JsonProperty("ordinanceCode")
    private String ordinanceCode;

    // 様式コード
    @JsonProperty("formCode")
    private String formCode;

    // 書類種別コード
    @JsonProperty("docTypeCode")
    private String docTypeCode;

    // 期間（自）
    @JsonProperty("periodStart")
    private String periodStart;

    // 期間（至）
    @JsonProperty("periodEnd")
    private String periodEnd;

    // 提出日時
    @JsonProperty("submitDateTime")
    private String submitDateTime;

    // 提出書類概要
    @JsonProperty("docDescription")
    private String docDescription;

    //発行会社EDINETコード
    @JsonProperty("issuerEdinetCode")
    private String issuerEdinetCode;

    // 対象EDINETコード
    @JsonProperty("subjectEdinetCode")
    private String subjectEdinetCode;

    // 小会社EDINETコード
    @JsonProperty("subsidiaryEdinetCode")
    private String subsidiaryEdinetCode;

    // 臨報提出事由
    @JsonProperty("currentReportReason")
    private String currentReportReason;

    // 親書類管理番号
    @JsonProperty("parentDocID")
    private String parentDocID;

    // 操作日時
    @JsonProperty("opeDateTime")
    private String opeDateTime;

    // 取下区分
    @JsonProperty("withdrawalStatus")
    private String withdrawalStatus;

    // 書類情報修正区分
    @JsonProperty("docInfoEditStatus")
    private String docInfoEditStatus;

    // 開示不開示区分
    @JsonProperty("disclosureStatus")
    private String disclosureStatus;

    // XBRL有無フラグ
    @JsonProperty("xbrlFlag")
    private String xbrlFlag;

    // PDF有無フラグ
    @JsonProperty("pdfFlag")
    private String pdfFlag;

    // 代替書面・添付文書有無フラグ
    @JsonProperty("attachDocFlag")
    private String attachDocFlag;

    // 英文ファイル有無フラグ
    @JsonProperty("englishDocFlag")
    private String englishDocFlag;

    public Optional<String> getEdinetCode() {
        return Optional.ofNullable(edinetCode);
    }
}
