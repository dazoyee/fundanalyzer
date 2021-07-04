package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.Results;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDateTime;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Entity(immutable = true)
@Table(name = "edinet_document")
public class EdinetDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String docId;

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
    private String parentDocId;

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

    // 登録日
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static EdinetDocumentEntity of(final Results results, final LocalDateTime createdAt) {
        return new EdinetDocumentEntity(
                null,
                results.getDocId(),
                results.getEdinetCode().orElse(null),
                results.getSecCode(),
                results.getJcn(),
                results.getFilerName(),
                results.getFundCode(),
                results.getOrdinanceCode(),
                results.getFormCode(),
                results.getDocTypeCode().orElse(null),
                results.getPeriodStart(),
                results.getPeriodEnd(),
                results.getSubmitDateTime(),
                results.getDocDescription(),
                results.getIssuerEdinetCode(),
                results.getSubjectEdinetCode(),
                results.getSubsidiaryEdinetCode(),
                results.getCurrentReportReason(),
                results.getParentDocID(),
                results.getOpeDateTime(),
                results.getWithdrawalStatus(),
                results.getDocInfoEditStatus(),
                results.getDisclosureStatus(),
                results.getXbrlFlag(),
                results.getPdfFlag(),
                results.getAttachDocFlag(),
                results.getEnglishDocFlag(),
                createdAt
        );
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(final String docId) {
        this.docId = docId;
    }

    public Optional<String> getEdinetCode() {
        return Optional.ofNullable(edinetCode);
    }

    public String getDocTypeCode() {
        return docTypeCode;
    }

    public Optional<String> getPeriodStart() {
        return Optional.ofNullable(periodStart);
    }

    public Optional<String> getPeriodEnd() {
        return Optional.ofNullable(periodEnd);
    }

    public String getSubmitDateTime() {
        return submitDateTime;
    }

    public Optional<String> getDocDescription() {
        return Optional.ofNullable(docDescription);
    }

    public String getParentDocId() {
        return parentDocId;
    }

    // for test
    public void setDocTypeCode(final String docTypeCode) {
        this.docTypeCode = docTypeCode;
    }

    public void setPeriodStart(final String periodStart) {
        this.periodStart = periodStart;
    }

    public void setPeriodEnd(final String periodEnd) {
        this.periodEnd = periodEnd;
    }

    public void setSubmitDateTime(final String submitDateTime) {
        this.submitDateTime = submitDateTime;
    }

    public void setParentDocId(final String parentDocId) {
        this.parentDocId = parentDocId;
    }
}
