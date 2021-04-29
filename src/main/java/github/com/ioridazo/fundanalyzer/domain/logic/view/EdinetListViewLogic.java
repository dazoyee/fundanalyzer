package github.com.ioridazo.fundanalyzer.domain.logic.view;

import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.logic.view.bean.EdinetListViewBean;
import github.com.ioridazo.fundanalyzer.domain.util.Converter;
import github.com.ioridazo.fundanalyzer.domain.util.Target;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EdinetListViewLogic {

    private final IndustryDao industryDao;
    private final CompanyDao companyDao;
    private final DocumentDao documentDao;
    private final AnalysisResultDao analysisResultDao;

    public EdinetListViewLogic(
            final IndustryDao industryDao,
            final CompanyDao companyDao,
            final DocumentDao documentDao,
            final AnalysisResultDao analysisResultDao) {
        this.industryDao = industryDao;
        this.companyDao = companyDao;
        this.documentDao = documentDao;
        this.analysisResultDao = analysisResultDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 提出日に対する処理状況をカウントする
     *
     * @param submitDate  対象提出日
     * @param targetTypes 書類種別コード
     * @return EdinetListViewBean
     */
    @NewSpan("EdinetListViewLogic.counter")
    public EdinetListViewBean counter(final LocalDate submitDate, final List<DocumentTypeCode> targetTypes) {
        final List<String> docTypeCode = targetTypes.stream().map(DocumentTypeCode::toValue).collect(Collectors.toList());
        final List<Document> documentList = documentDao.selectByTypeAndSubmitDate(docTypeCode, submitDate);
        final List<Company> allTargetCompanies = Target.allCompanies(
                companyDao.selectAll(),
                List.of(industryDao.selectByName("銀行業"), industryDao.selectByName("保険業")));

        // 処理対象書類
        final List<Document> targetList = extractTargetList(documentList, allTargetCompanies);
        // 処理済書類
        final List<Document> scrapedList = extractScrapedList(targetList).getFirst();
        // 未処理書類
        final List<Document> notScrapedList = extractScrapedList(targetList).getSecond();
        // 分析済書類
        final List<Document> analyzedList = extractAnalyzedList(scrapedList).getFirst();
        // 未分析書類
        final List<Document> notAnalyzedList = extractAnalyzedList(scrapedList).getSecond();

        return EdinetListViewBean.of(
                submitDate,
                documentList.size(),
                targetList.size(),
                scrapedList.size(),
                analyzedList.size(),
                notAnalyzedList.stream().map(Document::getDocumentId).collect(Collectors.joining(",<br>")),
                notScrapedList.stream().map(Document::getDocumentId).collect(Collectors.joining(",<br>")),
                notScrapedList.size(),
                nowLocalDateTime()
        );
    }

    /**
     * 処理対象書類リストを抽出する
     *
     * @param documentList       総書類リスト
     * @param allTargetCompanies 処理対象となるすべての会社
     * @return 処理対象書類リスト
     */
    List<Document> extractTargetList(final List<Document> documentList, final List<Company> allTargetCompanies) {
        return documentList.stream()
                // filter companyCode is present
                .filter(d -> Converter.toCompanyCode(d.getEdinetCode(), allTargetCompanies).isPresent())
                // filter removed
                .filter(Document::getNotRemoved)
                .collect(Collectors.toList());
    }

    /**
     * 処理済,未処理書類リストを抽出する
     *
     * @param targetList 処理対象書類リスト
     * @return 処理済書類リスト, 未処理書類リスト
     */
    Pair<List<Document>, List<Document>> extractScrapedList(final List<Document> targetList) {
        final List<Document> scrapedList = new ArrayList<>();
        final List<Document> notScrapedList = new ArrayList<>();

        targetList.forEach(document -> {
            final boolean isStatusDone = DocumentStatus.DONE.equals(DocumentStatus.fromValue(document.getScrapedBs()))
                    && DocumentStatus.DONE.equals(DocumentStatus.fromValue(document.getScrapedPl()))
                    && DocumentStatus.DONE.equals(DocumentStatus.fromValue(document.getScrapedNumberOfShares()));

            if (isStatusDone) {
                scrapedList.add(document);
            } else {
                notScrapedList.add(document);
            }
        });

        return Pair.of(scrapedList, notScrapedList);
    }

    /**
     * 分析済,未分析書類リストを抽出する
     *
     * @param scrapedList 処理済書類リスト
     * @return 分析済書類リスト, 未分析書類リスト
     */
    Pair<List<Document>, List<Document>> extractAnalyzedList(final List<Document> scrapedList) {
        final List<Document> analyzedList = new ArrayList<>();
        final List<Document> notAnalyzedList = new ArrayList<>();

        scrapedList.forEach(document -> {
            final Optional<Company> company = companyDao.selectByEdinetCode(document.getEdinetCode());

            // document period is present && company code is present
            if (document.getDocumentPeriod().isPresent() && company.flatMap(Company::getCode).isPresent()) {
                final Optional<AnalysisResult> analysisResult = analysisResultDao.selectByUniqueKey(
                        company.flatMap(Company::getCode).get(),
                        document.getDocumentPeriod().get(),
                        document.getDocumentTypeCode(),
                        document.getSubmitDate()
                );

                // analysis is done
                if (analysisResult.isPresent()) {
                    analyzedList.add(document);
                } else {
                    notAnalyzedList.add(document);
                }
            } else {
                notAnalyzedList.add(document);
            }
        });

        return Pair.of(analyzedList, notAnalyzedList);
    }
}
