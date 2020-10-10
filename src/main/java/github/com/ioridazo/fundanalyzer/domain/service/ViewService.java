package github.com.ioridazo.fundanalyzer.domain.service;

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
import github.com.ioridazo.fundanalyzer.domain.util.Converter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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

    public List<CompanyViewBean> viewCompany() {
        final var companyList = companyDao.selectAll();
        final var bank = industryDao.selectByName("銀行業");
        final var insurance = industryDao.selectByName("保険業");

        return companyList.stream()
                .filter(company -> company.getCode().isPresent())
                // 銀行業、保険業は対象外とする
                .filter(company -> !bank.getId().equals(company.getIndustryId()))
                .filter(company -> !insurance.getId().equals(company.getIndustryId()))
                .map(company -> {
                    final var submitDate = latestSubmitDate(company);
                    final var corporateValue = corporateValue(company);
                    final var latestStockPrice = latestStockPrice(company);
                    return new CompanyViewBean(
                            company.getCode().get(),
                            company.getCompanyName(),
                            submitDate.orElse(null),
                            corporateValue.getCorporateValue().orElse(null),
                            corporateValue.getStandardDeviation().orElse(null),
                            submitDate.flatMap(sd -> stockPriceOfSubmitDate(company, sd)).orElse(null),
                            latestStockPrice.getFirst().orElse(null),
                            latestStockPrice.getSecond().orElse(null),
                            discountValue(corporateValue.getCorporateValue().orElse(null), latestStockPrice.getSecond().orElse(null)).getFirst().orElse(null),
                            discountValue(corporateValue.getCorporateValue().orElse(null), latestStockPrice.getSecond().orElse(null)).getSecond().orElse(null),
                            corporateValue.getCountYear().orElse(null)
                    );
                })
                // 提出日が存在したら表示する
                .filter(companyViewBean -> companyViewBean.getSubmitDate() != null)
                .sorted(Comparator
                        .comparing(CompanyViewBean::getSubmitDate).reversed()
                        .thenComparing(CompanyViewBean::getCode)
                ).collect(Collectors.toList());
    }

    Optional<LocalDate> latestSubmitDate(final Company company) {
        return documentDao.selectByDocumentTypeCode("120").stream()
                .filter(d -> company.getEdinetCode().equals(d.getEdinetCode()))
                .max(Comparator.comparing(Document::getSubmitDate))
                .map(Document::getSubmitDate);
    }

    CorporateValue corporateValue(final Company company) {
        final var corporateValueList = analysisResultDao.selectByCompanyCode(company.getCode().orElseThrow()).stream()
                .map(AnalysisResult::getCorporateValue)
                .map(BigDecimal::doubleValue)
                .collect(Collectors.toList());

        if (!corporateValueList.isEmpty()) {
            // 平均値
            final var average = corporateValueList.stream().mapToDouble(Double::doubleValue).average();
            if (average.isPresent()) {
                // 分散
                final var variance = corporateValueList.stream()
                        .mapToDouble(Double::doubleValue)
                        .map(value -> Math.pow(value - average.getAsDouble(), 2.0)) // 2乗
                        .average();
                if (variance.isPresent()) {
                    // 標準偏差
                    return CorporateValue.of(
                            BigDecimal.valueOf(average.getAsDouble()),
                            BigDecimal.valueOf(Math.sqrt(variance.getAsDouble())),
                            BigDecimal.valueOf(corporateValueList.size())
                    );
                }
            }
        }
        return CorporateValue.of();
    }

    Optional<BigDecimal> stockPriceOfSubmitDate(final Company company, final LocalDate submitDate) {
        return stockPriceDao.selectByCode(company.getCode().orElseThrow()).stream()
                .filter(sp -> submitDate.equals(sp.getTargetDate()))
                .map(StockPrice::getStockPrice)
                .map(Optional::get)
                .findAny()
                .map(BigDecimal::valueOf);
    }

    Pair<Optional<LocalDate>, Optional<BigDecimal>> latestStockPrice(final Company company) {
        final var stockPrice = stockPriceDao.selectByCode(company.getCode().orElseThrow()).stream()
                .max(Comparator.comparing(StockPrice::getTargetDate));
        return Pair.of(
                stockPrice.map(StockPrice::getTargetDate),
                stockPrice.flatMap(StockPrice::getStockPrice).map(BigDecimal::valueOf)
        );
    }

    Pair<Optional<BigDecimal>, Optional<BigDecimal>> discountValue(
            final BigDecimal corporateValue, final BigDecimal latestStockPrice) {
        try {
            final var cv = Objects.requireNonNull(corporateValue);
            final var sp = Objects.requireNonNull(latestStockPrice);
            return Pair.of(
                    Optional.of(cv.subtract(sp).setScale(3, RoundingMode.HALF_UP)),
                    Optional.of(cv.divide(sp, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)))
            );
        } catch (NullPointerException e) {
            return Pair.of(Optional.empty(), Optional.empty());
        }
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
                .filter(d -> Converter.toCompanyCode(d.getEdinetCode(), companyAll).isPresent())
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
                        Converter.toCompanyCode(d.getEdinetCode(), companyAll).orElseThrow(),
                        // period
                        d.getPeriod()
                        ).isPresent()
                ).count();

        // 未分析企業コード
        final var notAnalyzedCode = scrapedList.stream()
                // filter analysis is done
                .filter(d -> analysisResultDao.selectByUniqueKey(
                        // companyCode
                        Converter.toCompanyCode(d.getEdinetCode(), companyAll).orElseThrow(),
                        // period
                        d.getPeriod()
                        ).isEmpty()
                )
                .map(Document::getEdinetCode)
                .map(ec -> Converter.toCompanyCode(ec, companyAll).orElseThrow())
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
                .map(ec -> Converter.toCompanyCode(ec, companyAll).orElseThrow())
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
                .filter(ec -> Converter.toCompanyCode(ec, companyAll).isEmpty())
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

    public List<CompanyViewBean> sortedCompanyByDiscountRate() {
        return viewCompany().stream()
                // not null
                .filter(cvb -> cvb.getDiscountRate() != null)
                // 100%以上を表示
                .filter(cvb -> cvb.getDiscountRate().compareTo(BigDecimal.valueOf(100)) > 0)
                .sorted(Comparator.comparing(CompanyViewBean::getDiscountRate).reversed())
                .collect(Collectors.toList());
    }

    private List<EdinetListViewBean> sortedEdinetList(final List<EdinetListViewBean> viewBeanList) {
        return viewBeanList.stream()
                .sorted(Comparator.comparing(EdinetListViewBean::getSubmitDate).reversed())
                .collect(Collectors.toList());
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class CorporateValue {
        // 企業価値
        private BigDecimal corporateValue;
        // 標準偏差
        private BigDecimal standardDeviation;
        // 対象年カウント
        private BigDecimal countYear;

        public static CorporateValue of() {
            return new CorporateValue();
        }

        public static CorporateValue of(
                final BigDecimal corporateValue,
                final BigDecimal standardDeviation,
                final BigDecimal countYear) {
            return new CorporateValue(corporateValue, standardDeviation, countYear);
        }

        public Optional<BigDecimal> getCorporateValue() {
            return Optional.ofNullable(corporateValue);
        }

        public Optional<BigDecimal> getStandardDeviation() {
            return Optional.ofNullable(standardDeviation);
        }

        public Optional<BigDecimal> getCountYear() {
            return Optional.ofNullable(countYear);
        }
    }
}
