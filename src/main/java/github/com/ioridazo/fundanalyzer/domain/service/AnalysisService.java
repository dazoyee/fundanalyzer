package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.logic.analysis.AnalysisLogic;
import github.com.ioridazo.fundanalyzer.domain.util.Converter;
import github.com.ioridazo.fundanalyzer.domain.util.Target;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    private final AnalysisLogic analysisLogic;
    private final IndustryDao industryDao;
    private final CompanyDao companyDao;
    private final DocumentDao documentDao;
    private final AnalysisResultDao analysisResultDao;

    public AnalysisService(
            final AnalysisLogic analysisLogic,
            final IndustryDao industryDao,
            final CompanyDao companyDao,
            final DocumentDao documentDao,
            final AnalysisResultDao analysisResultDao) {
        this.analysisLogic = analysisLogic;
        this.industryDao = industryDao;
        this.companyDao = companyDao;
        this.documentDao = documentDao;
        this.analysisResultDao = analysisResultDao;
    }

    /**
     * 対象書類の分析結果をデータベースに登録する
     *
     * @param documentId 書類ID
     */
    public void analyze(final String documentId) {
        analysisLogic.analyze(documentId);

        FundanalyzerLogClient.logService(
                MessageFormat.format("書類ID[{0}]の分析が正常に終了しました。", documentId),
                Category.DOCUMENT,
                Process.ANALYSIS
        );
    }

    /**
     * 対象書類の分析結果をデータベースに登録する
     *
     * @param submitDate   提出日
     * @param docTypeCodes 書類種別コード
     * @return Void
     */
    @NewSpan("AnalysisService.analyze.submitDate")
    public CompletableFuture<Void> analyze(final LocalDate submitDate, final List<DocTypeCode> docTypeCodes) {
        try {
            final List<String> docTypeCode = docTypeCodes.stream().map(DocTypeCode::toValue).collect(Collectors.toList());
            final var companyAll = companyDao.selectAll();
            final var bank = industryDao.selectByName("銀行業");
            final var insurance = industryDao.selectByName("保険業");

            final List<Document> documentList = documentDao.selectByTypeAndSubmitDate(docTypeCode, submitDate);
            if (documentList.isEmpty()) {
                FundanalyzerLogClient.logService(
                        MessageFormat.format("次の企業はデータベースに存在しませんでした。\t対象提出日:{0}", submitDate),
                        Category.DOCUMENT,
                        Process.ANALYSIS
                );
            } else {
                documentList.stream()
                        // target company code
                        .filter(document -> Target.containsEdinetCode(
                                document.getEdinetCode(), companyAll, List.of(bank, insurance)))
                        // only not analyze
                        .filter(document -> analysisResultDao.selectByUniqueKey(
                                Converter.toCompanyCode(document.getEdinetCode(), companyAll).orElseThrow(),
                                document.getDocumentPeriod(),
                                document.getDocumentTypeCode(),
                                submitDate
                                ).isEmpty()
                        )
                        .forEach(document -> analysisLogic.analyze(document.getDocumentId()));

                FundanalyzerLogClient.logService(
                        MessageFormat.format("次の企業に対して分析を正常に終了しました。\t対象提出日:{0}", submitDate),
                        Category.DOCUMENT,
                        Process.ANALYSIS
                );
            }
            return null;
        } catch (Throwable t) {
            FundanalyzerLogClient.logError(t);
            throw new FundanalyzerRuntimeException(t);
        }
    }
}
