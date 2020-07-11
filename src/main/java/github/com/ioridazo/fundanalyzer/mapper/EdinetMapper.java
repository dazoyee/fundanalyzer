package github.com.ioridazo.fundanalyzer.mapper;

import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocument;
import github.com.ioridazo.fundanalyzer.edinet.entity.response.Results;

import java.time.LocalDateTime;

public class EdinetMapper {

    public static EdinetDocument map(final Results results) {
        return new EdinetDocument(
                null,
                results.getDocId(),
                results.getEdinetCode(),
                results.getSecCode(),
                results.getJcn(),
                results.getFilerName(),
                results.getFundCode(),
                results.getOrdinanceCode(),
                results.getFormCode(),
                results.getDocTypeCode(),
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
                LocalDateTime.now()
        );
    }
}
