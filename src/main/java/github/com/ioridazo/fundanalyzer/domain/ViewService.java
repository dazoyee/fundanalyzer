package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.bean.CompanyViewBean;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
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
    private final AnalysisResultDao analysisResultDao;

    public ViewService(
            CompanyDao companyDao,
            EdinetDocumentDao edinetDocumentDao,
            AnalysisResultDao analysisResultDao) {
        this.companyDao = companyDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.analysisResultDao = analysisResultDao;
    }

    public static LocalDate mapToPeriod(final String year) {
        return LocalDate.of(Integer.parseInt(year), 1, 1);
    }

    public List<CompanyViewBean> viewCompany() {
        final var companyList = companyDao.selectAll().stream()
                .filter(company -> company.getCode().isPresent())
                .collect(Collectors.toList());
        var viewBeanList = new ArrayList<CompanyViewBean>();

        companyList.forEach(company -> viewBeanList.add(new CompanyViewBean(
                company.getCode().orElseThrow(),
                company.getCompanyName(),
                null,
                null
        )));

        return sortedCode(viewBeanList);
    }

    public List<CompanyViewBean> viewCompany(final String year) {
        final var companyAll = companyDao.selectAll();
        final var resultList = analysisResultDao.selectByPeriod(mapToPeriod(year));
        var presentCompanies = new ArrayList<Company>();
        var viewBeanList = new ArrayList<CompanyViewBean>();

        // ドキュメント取得済の会社のみ画面表示する
        edinetDocumentDao.selectByDocTypeCodeAndPeriodEnd("120", year).stream()
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
                Integer.parseInt(year)
        )));

        return sortedCode(viewBeanList);
    }

    private List<CompanyViewBean> sortedCode(final List<CompanyViewBean> viewBeanList) {
        return viewBeanList.stream()
                .sorted(Comparator.comparing(CompanyViewBean::getCode))
                .collect(Collectors.toList());
    }
}
