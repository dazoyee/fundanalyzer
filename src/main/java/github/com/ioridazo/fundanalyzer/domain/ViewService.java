package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.bean.CompanyViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.EdinetListViewBean;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocument;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ViewService {

    private final CompanyDao companyDao;
    private final EdinetDocumentDao edinetDocumentDao;
    private final DocumentDao documentDao;
    private final AnalysisResultDao analysisResultDao;

    public ViewService(
            CompanyDao companyDao,
            EdinetDocumentDao edinetDocumentDao,
            DocumentDao documentDao,
            AnalysisResultDao analysisResultDao) {
        this.companyDao = companyDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.documentDao = documentDao;
        this.analysisResultDao = analysisResultDao;
    }

    public static LocalDate mapToPeriod(final int year) {
        return LocalDate.of(year, 1, 1);
    }

    public List<CompanyViewBean> viewCompany() {
        final var companyList = companyDao.selectAll().stream()
                .filter(company -> company.getCode().isPresent())
                .collect(Collectors.toList());
        final var resultList = analysisResultDao.selectByPeriod(mapToPeriod(LocalDate.now().getYear()));
        var viewBeanList = new ArrayList<CompanyViewBean>();

        companyList.forEach(company -> viewBeanList.add(new CompanyViewBean(
                company.getCode().orElseThrow(),
                company.getCompanyName(),
                resultList.stream()
                        .filter(analysisResult -> company.getCode().orElseThrow().equals(analysisResult.getCompanyCode()))
                        .map(AnalysisResult::getCorporateValue)
                        .findAny()
                        .orElse(null),
                LocalDate.now().getYear()
        )));

        return sortedCode(viewBeanList);
    }

    public List<CompanyViewBean> viewCompany(final int year) {
        final var companyAll = companyDao.selectAll();
        final var resultList = analysisResultDao.selectByPeriod(mapToPeriod(year));
        var presentCompanies = new ArrayList<Company>();
        var viewBeanList = new ArrayList<CompanyViewBean>();

        // ドキュメント取得済の会社のみ画面表示する
        edinetDocumentDao.selectByDocTypeCodeAndPeriodEnd("120", String.valueOf(year)).stream()
                .map(EdinetDocument::getEdinetCode)
                .map(Optional::get)
                .forEach(edinetCode -> companyAll.stream()
                        .filter(company -> edinetCode.equals(company.getEdinetCode()))
                        .filter(company -> company.getCode().isPresent())
                        .findAny()
                        .ifPresent(presentCompanies::add));

        presentCompanies.forEach(company -> viewBeanList.add(new CompanyViewBean(
                company.getCode().orElseThrow(),
                company.getCompanyName(),
                resultList.stream()
                        .filter(analysisResult -> company.getCode().orElseThrow().equals(analysisResult.getCompanyCode()))
                        .map(AnalysisResult::getCorporateValue)
                        .findAny()
                        .orElse(null),
                year
        )));

        return sortedCode(viewBeanList);
    }

    public List<EdinetListViewBean> edinetList(final String documentTypeCode) {
        final var viewBeanList = new ArrayList<EdinetListViewBean>();
        final var documentList = documentDao.selectByDocumentTypeCode(documentTypeCode);

        final var documentGroupBySubmitDate = documentList.stream()
                .collect(Collectors.groupingBy(Document::getSubmitDate, Collectors.counting()));

        documentGroupBySubmitDate.forEach((submitDate, countAll) ->
                viewBeanList.add(counter(documentList, submitDate, countAll)));

        return viewBeanList;
    }

    EdinetListViewBean counter(final List<Document> documentList, final LocalDate submitDate, final Long countAll) {
        final var companyAll = companyDao.selectAll();

        // 処理対象件数
        final var targetList = documentList.stream()
                // filter submitDate
                .filter(document -> submitDate.equals(document.getSubmitDate()))
                // list edinetCode
                .map(Document::getEdinetCode)
                // filter companyCode is present
                .filter(ec -> companyAll.stream()
                        .filter(company -> ec.equals(company.getEdinetCode()))
                        .map(Company::getCode)
                        .findAny()
                        .orElseThrow()
                        .isPresent()
                ).collect(Collectors.toList());

        // 処理済件数
        final var scrapedList = targetList.stream()
                // filter scrapedBs is done
                .filter(ec -> documentList.stream()
                        .filter(document -> ec.equals(document.getEdinetCode()))
                        .anyMatch(document -> DocumentStatus.DONE.toValue().equals(document.getScrapedBs()))
                )
                // filter scrapedPl is done
                .filter(ec -> documentList.stream()
                        .filter(document -> ec.equals(document.getEdinetCode()))
                        .anyMatch(document -> DocumentStatus.DONE.toValue().equals(document.getScrapedPl()))
                )
                // filter scrapedNumberOfShares is done
                .filter(ec -> documentList.stream()
                        .filter(document -> ec.equals(document.getEdinetCode()))
                        .anyMatch(document -> DocumentStatus.DONE.toValue().equals(document.getScrapedNumberOfShares()))
                ).collect(Collectors.toList());

        // 分析済件数
        final var countAnalyzed = scrapedList.stream()
                // filter analysis is done
                .filter(ec -> analysisResultDao.selectByUniqueKey(
                        // companyCode
                        companyAll.stream()
                                .filter(company -> ec.equals(company.getEdinetCode()))
                                .map(Company::getCode)
                                .findAny()
                                .orElseThrow()
                                .orElseThrow(),
                        // period
                        LocalDate.of(submitDate.getYear(), 1, 1)
                        ).isPresent()
                ).count();

        // 未分析件数
        final var countNotAnalyzed = scrapedList.stream()
                // filter analysis is done
                .filter(ec -> analysisResultDao.selectByUniqueKey(
                        // companyCode
                        companyAll.stream()
                                .filter(company -> ec.equals(company.getEdinetCode()))
                                .map(Company::getCode)
                                .findAny()
                                .orElseThrow()
                                .orElseThrow(),
                        // period
                        LocalDate.of(submitDate.getYear(), 1, 1)
                        ).isEmpty()
                ).count();

        // 対象外件数
        final var countNotTarget = documentList.stream()
                // filter submitDate
                .filter(document -> submitDate.equals(document.getSubmitDate()))
                // list edinetCode
                .map(Document::getEdinetCode)
                // filter companyCode is empty
                .filter(ec -> companyAll.stream()
                        .filter(company -> ec.equals(company.getEdinetCode()))
                        .map(Company::getCode)
                        .findAny()
                        .orElseThrow()
                        .isEmpty()
                ).count();

        return new EdinetListViewBean(
                submitDate,
                countAll,
                (long) targetList.size(),
                (long) scrapedList.size(),
                countAnalyzed,
                countNotAnalyzed,
                (long) targetList.size() - scrapedList.size(),
                countNotTarget);
    }

    private List<CompanyViewBean> sortedCode(final List<CompanyViewBean> viewBeanList) {
        return viewBeanList.stream()
                .sorted(Comparator.comparing(CompanyViewBean::getCode))
                .collect(Collectors.toList());
    }
}
