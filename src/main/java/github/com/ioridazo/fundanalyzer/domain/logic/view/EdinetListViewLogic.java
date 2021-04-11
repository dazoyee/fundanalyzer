package github.com.ioridazo.fundanalyzer.domain.logic.view;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.logic.view.bean.EdinetListViewBean;
import github.com.ioridazo.fundanalyzer.domain.util.Converter;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EdinetListViewLogic {

    private static final String MESSAGE_FOR_A_LOT_OF_EDINET_CODE = "A lot of edinet_code.";

    private final AnalysisResultDao analysisResultDao;

    public EdinetListViewLogic(final AnalysisResultDao analysisResultDao) {
        this.analysisResultDao = analysisResultDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 提出日ごとに処理状況をカウントする
     *
     * @param submitDate         提出日
     * @param countAll           提出日における総数
     * @param documentList       ドキュメントステータスリスト
     * @param allTargetCompanies 処理対象となるすべての会社
     * @return EdinetListViewBean
     */
    @NewSpan("EdinetListViewLogic.counter")
    public EdinetListViewBean counter(
            final LocalDate submitDate,
            final Long countAll,
            final List<Document> documentList,
            final List<Company> allTargetCompanies) {
        // 処理対象件数
        final var targetList = documentList.stream()
                // filter companyCode is present
                .filter(d -> Converter.toCompanyCode(d.getEdinetCode(), allTargetCompanies).isPresent())
                // filter submitDate
                .filter(document -> submitDate.equals(document.getSubmitDate()))
                // filter removed
                .filter(Document::getNotRemoved)
                .collect(Collectors.toList());

        // 処理済件数
        final var scrapedList = targetList.stream()
                // filter scrapedBs is done
                .filter(d -> documentList.stream()
                        .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                        .anyMatch(document -> DocumentStatus.DONE.equals(DocumentStatus.fromValue(document.getScrapedBs())))
                )
                // filter scrapedPl is done
                .filter(d -> documentList.stream()
                        .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                        .anyMatch(document -> DocumentStatus.DONE.equals(DocumentStatus.fromValue(document.getScrapedPl())))
                )
                // filter scrapedNumberOfShares is done
                .filter(d -> documentList.stream()
                        .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                        .anyMatch(document -> DocumentStatus.DONE.equals(DocumentStatus.fromValue(document.getScrapedNumberOfShares())))
                ).collect(Collectors.toList());

        return new EdinetListViewBean(
                submitDate,
                countAll,
                (long) targetList.size(),
                (long) scrapedList.size(),

                // 分析済件数
                scrapedList.stream()
                        // filter analysis is done
                        .filter(d -> analysisResultDao.selectByUniqueKey(
                                Converter.toCompanyCode(d.getEdinetCode(), allTargetCompanies).orElseThrow(),
                                d.getDocumentPeriod(),
                                d.getDocumentTypeCode(),
                                d.getSubmitDate()
                                ).isPresent()
                        ).count(),

                // 未分析企業コード
                Optional.of(scrapedList.stream()
                        // filter analysis is done
                        .filter(d -> analysisResultDao.selectByUniqueKey(
                                Converter.toCompanyCode(d.getEdinetCode(), allTargetCompanies).orElseThrow(),
                                d.getDocumentPeriod(),
                                d.getDocumentTypeCode(),
                                d.getSubmitDate()
                                ).isEmpty()
                        )
                        .map(Document::getEdinetCode)
                        .collect(Collectors.joining(",")))
                        .map(notAnalyzedCode -> {
                            if (notAnalyzedCode.length() > 100) {
                                return MESSAGE_FOR_A_LOT_OF_EDINET_CODE;
                            } else {
                                return notAnalyzedCode;
                            }
                        })
                        .orElse(""),

                // 処理中企業コード
                Optional.of(targetList.stream()
                        .map(Document::getEdinetCode)
                        // filter no all done
                        .filter(edinetCode -> documentList.stream()
                                .filter(document -> edinetCode.equals(document.getEdinetCode()))
                                .anyMatch(document -> !(DocumentStatus.DONE.equals(DocumentStatus.fromValue(document.getScrapedBs()))
                                        && DocumentStatus.DONE.equals(DocumentStatus.fromValue(document.getScrapedPl()))
                                        && DocumentStatus.DONE.equals(DocumentStatus.fromValue(document.getScrapedNumberOfShares()))))
                        )
                        // filter no all notYet
                        .filter(edinetCode -> documentList.stream()
                                .filter(document -> edinetCode.equals(document.getEdinetCode()))
                                .anyMatch(document -> !(DocumentStatus.NOT_YET.equals(DocumentStatus.fromValue(document.getScrapedBs()))
                                        && DocumentStatus.NOT_YET.equals(DocumentStatus.fromValue(document.getScrapedPl()))
                                        && DocumentStatus.NOT_YET.equals(DocumentStatus.fromValue(document.getScrapedNumberOfShares()))))
                        )
                        .collect(Collectors.joining(",")))
                        .map(cantScrapedCode -> {
                            if (cantScrapedCode.length() > 100) {
                                return MESSAGE_FOR_A_LOT_OF_EDINET_CODE;
                            } else {
                                return cantScrapedCode;
                            }
                        })
                        .orElse(""),

                // 未処理件数
                targetList.stream()
                        // filter scrapedBs is notYet
                        .filter(d -> documentList.stream()
                                .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                                .anyMatch(document -> DocumentStatus.NOT_YET.equals(DocumentStatus.fromValue(document.getScrapedBs())))
                        )
                        // filter scrapedPl is notYet
                        .filter(d -> documentList.stream()
                                .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                                .anyMatch(document -> DocumentStatus.NOT_YET.equals(DocumentStatus.fromValue(document.getScrapedPl())))
                        )
                        // filter scrapedNumberOfShares is notYet
                        .filter(d -> documentList.stream()
                                .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                                .anyMatch(document -> DocumentStatus.NOT_YET.equals(DocumentStatus.fromValue(document.getScrapedNumberOfShares())))
                        ).count(),

                // 対象外件数
                countAll - targetList.size(),

                nowLocalDateTime(),
                nowLocalDateTime()
        );
    }
}
