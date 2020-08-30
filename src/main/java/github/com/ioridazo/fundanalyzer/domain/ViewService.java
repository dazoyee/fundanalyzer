package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.bean.CompanyViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.EdinetListViewBean;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPrice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ViewService {

    private final IndustryDao industryDao;
    private final CompanyDao companyDao;
    private final DocumentDao documentDao;
    private final AnalysisResultDao analysisResultDao;
    private final StockPriceDao stockPriceDao;

    public ViewService(
            final IndustryDao industryDao,
            final CompanyDao companyDao,
            final DocumentDao documentDao,
            final AnalysisResultDao analysisResultDao,
            final StockPriceDao stockPriceDao) {
        this.industryDao = industryDao;
        this.companyDao = companyDao;
        this.documentDao = documentDao;
        this.analysisResultDao = analysisResultDao;
        this.stockPriceDao = stockPriceDao;
    }

    public static LocalDate mapToPeriod(final int year) {
        return LocalDate.of(year, 1, 1);
    }

    public static Optional<String> codeConverter(final String edinetCode, final List<Company> companyAll) {
        return companyAll.stream()
                .filter(company -> edinetCode.equals(company.getEdinetCode()))
                .map(Company::getCode)
                .findAny()
                .orElseThrow();
    }

    public List<CompanyViewBean> viewCompanyAll() {
        final var companyList = companyDao.selectAll().stream()
                .filter(company -> company.getCode().isPresent())
                .collect(Collectors.toList());
        final var resultList = analysisResultDao.selectByPeriod(mapToPeriod(LocalDate.now().getYear()));
        var viewBeanList = new ArrayList<CompanyViewBean>();

        companyList.forEach(company -> {
            final var corporateValue = resultList.stream()
                    .filter(analysisResult -> company.getCode().orElseThrow().equals(analysisResult.getCompanyCode()))
                    .map(AnalysisResult::getCorporateValue)
                    .findAny()
                    .orElse(null);
            final var stockPriceList = stockPriceDao.selectByCode(company.getCode().orElseThrow());
            final var latestStockPrice = stockPriceList.stream()
                    .max(Comparator.comparing(StockPrice::getTargetDate));
            viewBeanList.add(new CompanyViewBean(
                    company.getCode().orElseThrow(),
                    company.getCompanyName(),
                    null,
                    corporateValue,
                    null,
                    latestStockPrice.map(StockPrice::getTargetDate).orElse(null),
                    latestStockPrice.flatMap(StockPrice::getStockPrice).orElse(null),
                    corporateValue != null ? corporateValue.subtract(BigDecimal.valueOf(latestStockPrice.flatMap(StockPrice::getStockPrice).orElse(0.0))) : null,
                    null
            ));
        });

        return sortedCompanyAll(viewBeanList);
    }

    public List<CompanyViewBean> viewCompany() {
        var viewBeanList = new ArrayList<CompanyViewBean>();

        List.of(
                LocalDate.now().getYear(),
                LocalDate.now().minusYears(1).getYear()
        ).forEach(year -> viewBeanList.addAll((viewCompany(year))));

        return sortedCompany(viewBeanList);
    }

    public List<CompanyViewBean> viewCompany(final int year) {
        final var bank = industryDao.selectByName("銀行業");
        final var insurance = industryDao.selectByName("保険業");
        final var companyAll = companyDao.selectAll();
        final var documentList = documentDao.selectByDocumentTypeCode("120");
        final var resultList = analysisResultDao.selectByPeriod(mapToPeriod(year));
        var presentCompanies = new ArrayList<Company>();
        var viewBeanList = new ArrayList<CompanyViewBean>();

        documentDao.selectByTypeAndPeriod("120", String.valueOf(year)).stream()
                .map(Document::getEdinetCode)
                .forEach(edinetCode -> companyAll.stream()
                        .filter(company -> edinetCode.equals(company.getEdinetCode()))
                        .filter(company -> company.getCode().isPresent())
                        // 銀行業、保険業は対象外とする
                        .filter(company -> !bank.getId().equals(company.getIndustryId()))
                        .filter(company -> !insurance.getId().equals(company.getIndustryId()))
                        .findAny()
                        .ifPresent(presentCompanies::add)
                );

        presentCompanies.forEach(company -> documentList.stream()
                .filter(document -> company.getEdinetCode().equals(document.getEdinetCode()))
                .map(Document::getSubmitDate)
                .filter(submitDate -> year == submitDate.getYear())
                .findAny()
                // 指定対象年に合致する提出日が存在する場合
                .ifPresent(submitDate -> {
                    final var corporateValue = resultList.stream()
                            .filter(ar -> company.getCode().orElseThrow().equals(ar.getCompanyCode()))
                            .map(AnalysisResult::getCorporateValue)
                            .findAny()
                            .orElse(null);
                    final var stockPriceList = stockPriceDao.selectByCode(company.getCode().orElseThrow());
                    final var latestStockPrice = stockPriceList.stream()
                            .max(Comparator.comparing(StockPrice::getTargetDate));
                    viewBeanList.add(new CompanyViewBean(
                            company.getCode().map(c -> c.substring(0, 4)).orElseThrow(),
                            company.getCompanyName(),
                            submitDate,
                            corporateValue,
                            stockPriceList.stream()
                                    .filter(stockPrice -> submitDate.equals(stockPrice.getTargetDate()))
                                    .map(StockPrice::getStockPrice)
                                    .map(Optional::get)
                                    .findAny()
                                    .orElse(null),
                            latestStockPrice.map(StockPrice::getTargetDate).orElse(null),
                            latestStockPrice.flatMap(StockPrice::getStockPrice).orElse(null),
                            corporateValue != null ? corporateValue.subtract(BigDecimal.valueOf(latestStockPrice.flatMap(StockPrice::getStockPrice).orElse(0.0))) : null,
                            year
                    ));
                })
        );

        return sortedCompany(viewBeanList);
    }

    public String companyUpdated() {
        return companyDao.selectAll().stream()
                .map(Company::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .map(dateTime -> dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))
                .orElse("null");
    }

    public List<EdinetListViewBean> edinetList(final String documentTypeCode) {
        final var viewBeanList = edinetListAll(documentTypeCode);

        viewBeanList.removeIf(vb -> vb.getCountTarget().equals(vb.getCountScraped()) &&
                vb.getCountTarget().equals(vb.getCountAnalyzed())
        );

        return sortedEdinetList(viewBeanList);
    }

    public List<EdinetListViewBean> edinetListAll(final String documentTypeCode) {
        final var viewBeanList = new ArrayList<EdinetListViewBean>();
        final var documentList = documentDao.selectByDocumentTypeCode(documentTypeCode);

        final var documentGroupBySubmitDate = documentList.stream()
                .collect(Collectors.groupingBy(Document::getSubmitDate, Collectors.counting()));

        documentGroupBySubmitDate.forEach((submitDate, countAll) ->
                viewBeanList.add(counter(documentList, submitDate, countAll)));

        return sortedEdinetList(viewBeanList);
    }

    EdinetListViewBean counter(final List<Document> documentList, final LocalDate submitDate, final Long countAll) {
        final var bank = industryDao.selectByName("銀行業");
        final var insurance = industryDao.selectByName("保険業");
        final var companyAll = companyDao.selectAll();

        // 処理対象件数
        final var targetList = documentList.stream()
                // filter submitDate
                .filter(document -> submitDate.equals(document.getSubmitDate()))
                // filter removed
                .filter(Document::getNotRemoved)
                // filter companyCode is present
                .filter(d -> codeConverter(d.getEdinetCode(), companyAll).isPresent())
                // filter no bank
                .filter(d -> companyAll.stream()
                        .filter(company -> d.getEdinetCode().equals(company.getEdinetCode()))
                        .noneMatch(company -> bank.getId().equals(company.getIndustryId()))
                )
                // filter no insurance
                .filter(d -> companyAll.stream()
                        .filter(company -> d.getEdinetCode().equals(company.getEdinetCode()))
                        .noneMatch(company -> insurance.getId().equals(company.getIndustryId()))
                )
                .collect(Collectors.toList());

        // 処理済件数
        final var scrapedList = targetList.stream()
                // filter scrapedBs is done
                .filter(d -> documentList.stream()
                        .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                        .anyMatch(document -> DocumentStatus.DONE.toValue().equals(document.getScrapedBs()))
                )
                // filter scrapedPl is done
                .filter(d -> documentList.stream()
                        .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                        .anyMatch(document -> DocumentStatus.DONE.toValue().equals(document.getScrapedPl()))
                )
                // filter scrapedNumberOfShares is done
                .filter(d -> documentList.stream()
                        .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                        .anyMatch(document -> DocumentStatus.DONE.toValue().equals(document.getScrapedNumberOfShares()))
                ).collect(Collectors.toList());

        // 分析済件数
        final var countAnalyzed = scrapedList.stream()
                // filter analysis is done
                .filter(d -> analysisResultDao.selectByUniqueKey(
                        // companyCode
                        codeConverter(d.getEdinetCode(), companyAll).orElseThrow(),
                        // period
                        d.getPeriod()
                        ).isPresent()
                ).count();

        // 未分析企業コード
        final var notAnalyzedCode = scrapedList.stream()
                // filter analysis is done
                .filter(d -> analysisResultDao.selectByUniqueKey(
                        // companyCode
                        codeConverter(d.getEdinetCode(), companyAll).orElseThrow(),
                        // period
                        d.getPeriod()
                        ).isEmpty()
                )
                .map(Document::getEdinetCode)
                .map(ec -> codeConverter(ec, companyAll).orElseThrow())
                .collect(Collectors.joining("\n"));

        // 処理中企業コード
        final var cantScrapedCode = targetList.stream()
                // filter no all done
                .filter(d -> documentList.stream()
                        .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                        .anyMatch(document -> !(DocumentStatus.DONE.toValue().equals(document.getScrapedBs()) &&
                                DocumentStatus.DONE.toValue().equals(document.getScrapedPl()) &&
                                DocumentStatus.DONE.toValue().equals(document.getScrapedNumberOfShares())))
                )
                // filter no all notYet
                .filter(d -> documentList.stream()
                        .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                        .anyMatch(document -> !(DocumentStatus.NOT_YET.toValue().equals(document.getScrapedBs()) &&
                                DocumentStatus.NOT_YET.toValue().equals(document.getScrapedPl()) &&
                                DocumentStatus.NOT_YET.toValue().equals(document.getScrapedNumberOfShares())))
                )
                .map(Document::getEdinetCode)
                .map(ec -> codeConverter(ec, companyAll).orElseThrow())
                .collect(Collectors.joining("\n"));

        // 未処理件数
        final var countNotScraped = targetList.stream()
                // filter scrapedBs is notYet
                .filter(d -> documentList.stream()
                        .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                        .anyMatch(document -> DocumentStatus.NOT_YET.toValue().equals(document.getScrapedBs()))
                )
                // filter scrapedPl is notYet
                .filter(d -> documentList.stream()
                        .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                        .anyMatch(document -> DocumentStatus.NOT_YET.toValue().equals(document.getScrapedPl()))
                )
                // filter scrapedNumberOfShares is notYet
                .filter(d -> documentList.stream()
                        .filter(document -> d.getEdinetCode().equals(document.getEdinetCode()))
                        .anyMatch(document -> DocumentStatus.NOT_YET.toValue().equals(document.getScrapedNumberOfShares()))
                ).count();

        // 対象外件数
        final var countNotTarget = documentList.stream()
                // filter submitDate
                .filter(document -> submitDate.equals(document.getSubmitDate()))
                // list edinetCode
                .map(Document::getEdinetCode)
                // filter companyCode is empty
                .filter(ec -> codeConverter(ec, companyAll).isEmpty())
                .count();

        return new EdinetListViewBean(
                submitDate,
                countAll,
                (long) targetList.size(),
                (long) scrapedList.size(),
                countAnalyzed,
                notAnalyzedCode,
                cantScrapedCode,
                countNotScraped,
                countNotTarget);
    }

    private List<CompanyViewBean> sortedCompany(final List<CompanyViewBean> viewBeanList) {
        return viewBeanList.stream()
                .sorted(Comparator
                        .comparing(CompanyViewBean::getSubmitDate).reversed()
                        .thenComparing(CompanyViewBean::getCode)
                )
                .collect(Collectors.toList());
    }

    private List<CompanyViewBean> sortedCompanyAll(final List<CompanyViewBean> viewBeanList) {
        return viewBeanList.stream()
                .sorted(Comparator.comparing(CompanyViewBean::getCode))
                .collect(Collectors.toList());
    }

    private List<EdinetListViewBean> sortedEdinetList(final List<EdinetListViewBean> viewBeanList) {
        return viewBeanList.stream()
                .sorted(Comparator.comparing(EdinetListViewBean::getSubmitDate).reversed())
                .collect(Collectors.toList());
    }
}
