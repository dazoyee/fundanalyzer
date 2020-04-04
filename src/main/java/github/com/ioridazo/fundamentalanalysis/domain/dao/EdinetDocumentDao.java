package github.com.ioridazo.fundamentalanalysis.domain.dao;

import github.com.ioridazo.fundamentalanalysis.domain.entity.EdinetDocument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class EdinetDocumentDao {

    private final JdbcTemplate jdbc;

    public EdinetDocumentDao(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(EdinetDocument document) {
        jdbc.update(
                "INSERT INTO edinet_document VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                document.getDocID(),
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
