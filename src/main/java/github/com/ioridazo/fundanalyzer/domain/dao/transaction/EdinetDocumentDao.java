package github.com.ioridazo.fundanalyzer.domain.dao.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocument;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class EdinetDocumentDao {

    private final JdbcTemplate jdbc;

    public EdinetDocumentDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EdinetDocument> findByDocTypeCode(String docTypeCode) {
        return jdbc.query(
                "SELECT * FROM edinet_document WHERE doc_type_code = ?",
                new BeanPropertyRowMapper<>(EdinetDocument.class),
                docTypeCode
        );
    }

    public void insert(EdinetDocument document) {
        jdbc.update(
                "INSERT INTO edinet_document VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                document.getDocId(),
                document.getEdinetCode(),
                document.getSecCode(),
                document.getJcn(),
                document.getFilerName(),
                document.getFundCode(),
                document.getOrdinanceCode(),
                document.getFormCode(),
                document.getDocTypeCode(),
                document.getPeriodStart(),
                document.getPeriodEnd(),
                document.getSubmitDateTime(),
                document.getDocDescription(),
                document.getIssuerEdinetCode(),
                document.getSubjectEdinetCode(),
                document.getSubsidiaryEdinetCode(),
                document.getCurrentReportReason(),
                document.getParentDocID(),
                document.getOpeDateTime(),
                document.getWithdrawalStatus(),
                document.getDocInfoEditStatus(),
                document.getDisclosureStatus(),
                document.getXbrlFlag(),
                document.getPdfFlag(),
                document.getAttachDocFlag(),
                document.getEnglishDocFlag(),
                LocalDateTime.now()
        );
    }
}
