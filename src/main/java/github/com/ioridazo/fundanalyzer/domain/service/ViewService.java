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
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    /**
     * 企業価値等を算出して一定以上を表示する
     *
     * @return 会社一覧
     */
    public List<CompanyViewBean> viewCompany() {
        return sortedCompanyList(getCompanyViewBean().stream()
                // not null
                .filter(cvb -> cvb.getDiscountRate() != null)
                // 100%以上を表示
                .filter(cvb -> cvb.getDiscountRate().compareTo(BigDecimal.valueOf(100)) > 0)
                .collect(Collectors.toList()));
    }

    /**
     * 企業価値等を算出してすべてを表示する
     *
     * @return 会社一覧
     */
    public List<CompanyViewBean> viewCompanyAll() {
        return sortedCompanyList(getCompanyViewBean());
    }

    /**
     * 企業価値等を割安度でソートする
     *
     * @return ソート後のリスト
     */
    public List<CompanyViewBean> sortedCompanyByDiscountRate() {
        return getCompanyViewBean().stream()
                // not null
                .filter(cvb -> cvb.getDiscountRate() != null)
                // 100%以上を表示
                .filter(cvb -> cvb.getDiscountRate().compareTo(BigDecimal.valueOf(100)) > 0)
                .sorted(Comparator.comparing(CompanyViewBean::getDiscountRate).reversed())
                .collect(Collectors.toList());
    }

    private List<CompanyViewBean> getCompanyViewBean() {
        return companyAllTargeted().stream()
                .map(company -> {
                    final var submitDate = latestSubmitDate(company);
                    final var corporateValue = corporateValue(company);
                    final var stockPrice = submitDate.map(sd -> stockPrice(company, sd)).orElse(StockPriceValue.of());
                    final var discountValue = discountValue(corporateValue.getCorporateValue().orElse(null), stockPrice.getLatestStockPrice().orElse(null));
                    return new CompanyViewBean(
                            company.getCode().orElseThrow(),
                            company.getCompanyName(),
                            submitDate.orElse(null),
                            corporateValue.getCorporateValue().orElse(null),
                            corporateValue.getStandardDeviation().orElse(null),
                            stockPrice.getStockPriceOfSubmitDate().orElse(null),
                            stockPrice.getImportDate().orElse(null),
                            stockPrice.getLatestStockPrice().orElse(null),
                            discountValue.getFirst().orElse(null),
                            discountValue.getSecond().orElse(null),
                            corporateValue.getCountYear().orElse(null)
                    );
                })
                // 提出日が存在したら表示する
                .filter(companyViewBean -> companyViewBean.getSubmitDate() != null)
                .collect(Collectors.toList());
    }

    /**
     * 直近の財務諸表提出日を取得する
     *
     * @param company 会社情報
     * @return 提出日
     */
    Optional<LocalDate> latestSubmitDate(final Company company) {
        return documentDao.selectByDocumentTypeCode("120").stream()
                .filter(d -> company.getEdinetCode().equals(d.getEdinetCode()))
                .max(Comparator.comparing(Document::getSubmitDate))
                .map(Document::getSubmitDate);
    }

    /**
     * 企業価値を算出する
     *
     * @param company 会社情報
     * @return <li>平均の企業価値</li><li>標準偏差</li><li>対象年数</li>
     */
    CorporateValue corporateValue(final Company company) {
        final var corporateValueList = analysisResultDao.selectByCompanyCode(company.getCode().orElseThrow()).stream()
                .map(AnalysisResult::getCorporateValue)
                .collect(Collectors.toList());

        if (!corporateValueList.isEmpty()) {
            // 平均値
            final var average = corporateValueList.stream()
                    // sum
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    // average
                    .divide(BigDecimal.valueOf(corporateValueList.size()), 2, RoundingMode.HALF_UP);
            // 標準偏差
            final var standardDeviation = corporateValueList.stream()
                    // (value - average) ^2
                    .map(value -> value.subtract(average).pow(2))
                    // sum
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    // average
                    .divide(BigDecimal.valueOf(corporateValueList.size()), 3, RoundingMode.HALF_UP)
                    // sqrt
                    .sqrt(new MathContext(5, RoundingMode.HALF_UP));
            return CorporateValue.of(average, standardDeviation, BigDecimal.valueOf(corporateValueList.size()));
        } else {
            return CorporateValue.of();
        }
    }

    /**
     * 株価を取得する
     *
     * @param company    会社情報
     * @param submitDate 提出日
     * @return <li>提出日の株価</li><li>最近株価日付</li><li>最新株価</li>
     */
    StockPriceValue stockPrice(final Company company, final LocalDate submitDate) {
        final var stockPriceList = stockPriceDao.selectByCode(company.getCode().orElseThrow());
        return StockPriceValue.of(
                // stockPriceOfSubmitDate
                stockPriceList.stream()
                        .filter(sp -> submitDate.equals(sp.getTargetDate()))
                        .map(StockPrice::getStockPrice)
                        .map(Optional::get)
                        .findAny()
                        .map(BigDecimal::valueOf).orElse(null),
                // importDate
                stockPriceList.stream()
                        .max(Comparator.comparing(StockPrice::getTargetDate))
                        .map(StockPrice::getTargetDate).orElse(null),
                // latestStockPrice
                stockPriceList.stream()
                        .max(Comparator.comparing(StockPrice::getTargetDate))
                        .flatMap(StockPrice::getStockPrice)
                        .map(BigDecimal::valueOf).orElse(null)
        );
    }

    /**
     * 割安値
     *
     * @param corporateValue   企業価値
     * @param latestStockPrice 最新株価
     * @return <li>割安値</li><li>割安度</li>
     */
    Pair<Optional<BigDecimal>, Optional<BigDecimal>> discountValue(
            final BigDecimal corporateValue, final BigDecimal latestStockPrice) {
        try {
            final var cv = Objects.requireNonNull(corporateValue);
            final var sp = Objects.requireNonNull(latestStockPrice);
            return Pair.of(
                    Optional.of(cv.subtract(sp).abs(new MathContext(6))),
                    Optional.of(cv.divide(sp, 5, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(3, RoundingMode.HALF_UP))
            );
        } catch (NullPointerException e) {
            return Pair.of(Optional.empty(), Optional.empty());
        }
    }

    /**
     * 会社情報の更新日を取得する
     *
     * @return 最新更新日
     */
    public String companyUpdated() {
        return companyDao.selectAll().stream()
                .map(Company::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .map(dateTime -> dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))
                .orElse("null");
    }

    /**
     * 処理状況を表示するためのリストを取得する
     *
     * @param documentTypeCode 書類種別コード
     * @return 処理状況リスト
     */
    public List<EdinetListViewBean> edinetList(final String documentTypeCode) {
        final var viewBeanList = getEdinetList(documentTypeCode);
        viewBeanList.removeIf(
                el -> el.getCountTarget().equals(el.getCountScraped()) &&
                        el.getCountTarget().equals(el.getCountAnalyzed())
        );
        return sortedEdinetList(viewBeanList);
    }

    /**
     * すべての処理状況を表示するためのリストを取得する
     *
     * @param documentTypeCode 書類種別コード
     * @return 処理状況リスト
     */
    public List<EdinetListViewBean> edinetListAll(final String documentTypeCode) {
        return sortedEdinetList(getEdinetList(documentTypeCode));
    }

    private List<EdinetListViewBean> getEdinetList(final String documentTypeCode) {
        final var documentList = documentDao.selectByDocumentTypeCode(documentTypeCode);
        return documentList.stream()
                // 提出日ごとに件数をカウントする
                .collect(Collectors.groupingBy(Document::getSubmitDate, Collectors.counting()))
                // map -> stream
                .entrySet()
                .stream().map(localDateLongEntry -> counter(localDateLongEntry.getKey(),
                        localDateLongEntry.getValue(),
                        documentList,
                        companyAllTargeted()
                ))
                .collect(Collectors.toList());
    }

    private EdinetListViewBean counter(
            final LocalDate submitDate,
            final Long countAll,
            final List<Document> documentList,
            final List<Company> companyAllTargeted) {
        // 処理対象件数
        final var targetList = documentList.stream()
                // filter companyCode is present
                .filter(d -> Converter.toCompanyCode(d.getEdinetCode(), companyAllTargeted).isPresent())
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

        return new EdinetListViewBean(
                submitDate,
                countAll,
                (long) targetList.size(),
                (long) scrapedList.size(),

                // 分析済件数
                scrapedList.stream()
                        // filter analysis is done
                        .filter(d -> analysisResultDao.selectByUniqueKey(
                                // companyCode
                                Converter.toCompanyCode(d.getEdinetCode(), companyAllTargeted).orElseThrow(),
                                // period
                                d.getPeriod()
                                ).isPresent()
                        ).count(),

                // 未分析企業コード
                scrapedList.stream()
                        // filter analysis is done
                        .filter(d -> analysisResultDao.selectByUniqueKey(
                                // companyCode
                                Converter.toCompanyCode(d.getEdinetCode(), companyAllTargeted).orElseThrow(),
                                // period
                                d.getPeriod()
                                ).isEmpty()
                        )
                        .map(Document::getEdinetCode)
                        .collect(Collectors.joining("\n")),

                // 処理中企業コード
                targetList.stream()
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
                        .collect(Collectors.joining("\n")),

                // 未処理件数
                targetList.stream()
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
                        ).count(),

                // 対象外件数
                countAll - targetList.size()
        );
    }

    private List<Company> companyAllTargeted() {
        final var companyList = companyDao.selectAll();
        final var bank = industryDao.selectByName("銀行業");
        final var insurance = industryDao.selectByName("保険業");

        return companyList.stream()
                .filter(company -> company.getCode().isPresent())
                // 銀行業、保険業は対象外とする
                .filter(company -> !bank.getId().equals(company.getIndustryId()))
                .filter(company -> !insurance.getId().equals(company.getIndustryId()))
                .collect(Collectors.toList());
    }

    private List<CompanyViewBean> sortedCompanyList(final List<CompanyViewBean> viewBeanList) {
        return viewBeanList.stream()
                .sorted(Comparator
                        .comparing(CompanyViewBean::getSubmitDate).reversed()
                        .thenComparing(CompanyViewBean::getCode))
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

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class StockPriceValue {
        // 提出日株価
        private BigDecimal stockPriceOfSubmitDate;
        // 株価取得日
        private LocalDate importDate;
        // 最新株価
        private BigDecimal latestStockPrice;

        public static StockPriceValue of() {
            return new StockPriceValue();
        }

        public static StockPriceValue of(
                final BigDecimal stockPriceOfSubmitDate,
                final LocalDate importDate,
                BigDecimal latestStockPrice) {
            return new StockPriceValue(stockPriceOfSubmitDate, importDate, latestStockPrice);
        }

        public Optional<BigDecimal> getStockPriceOfSubmitDate() {
            return Optional.ofNullable(stockPriceOfSubmitDate);
        }

        public Optional<LocalDate> getImportDate() {
            return Optional.ofNullable(importDate);
        }

        public Optional<BigDecimal> getLatestStockPrice() {
            return Optional.ofNullable(latestStockPrice);
        }
    }
}
