package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.ValuationDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.InvestmentIndicatorEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationViewModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class ValuationSpecification {

    private static final String CACHE_KEY_ALL_VALUATION_VIEW = "allValuationView";

    private static final int SECOND_DECIMAL_PLACE = 2;

    private static final Logger log = LogManager.getLogger(ValuationSpecification.class);

    private final ValuationDao valuationDao;
    private final CompanySpecification companySpecification;
    private final StockSpecification stockSpecification;
    private final InvestmentIndicatorSpecification investmentIndicatorSpecification;

    public ValuationSpecification(
            final ValuationDao valuationDao,
            final CompanySpecification companySpecification,
            final StockSpecification stockSpecification,
            final InvestmentIndicatorSpecification investmentIndicatorSpecification) {
        this.valuationDao = valuationDao;
        this.companySpecification = companySpecification;
        this.stockSpecification = stockSpecification;
        this.investmentIndicatorSpecification = investmentIndicatorSpecification;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 最新の評価結果を取得する
     *
     * @param companyCode 企業コード
     * @param submitDate  提出日
     * @return 最新の評価結果
     */
    public Optional<ValuationEntity> findLatestValuation(final String companyCode, final LocalDate submitDate) {
        return valuationDao.selectByCodeAndSubmitDate(companyCode, submitDate).stream()
                .max(Comparator.comparing(ValuationEntity::getTargetDate));
    }

    /**
     * 業種による平均の評価結果を取得する
     *
     * @param industryName 業種名
     * @param companyList  企業リスト
     * @return 業種による平均の評価結果
     */
    public IndustryValuationViewModel averageValuation(final String industryName, final List<Company> companyList) {
        final ArrayList<CompanyValuationViewModel> viewList = new ArrayList<>();
        companyList.forEach(company -> valuationDao.selectByCode(company.getCode()).stream()
                .max(Comparator.comparing(ValuationEntity::getTargetDate).thenComparing(ValuationEntity::getSubmitDate))
                .ifPresent(valuationEntity -> viewList.add(CompanyValuationViewModel.of(valuationEntity, company))));

        return IndustryValuationViewModel.of(
                industryName,
                viewList.stream()
                        .map(CompanyValuationViewModel::getDifferenceFromSubmitDate)
                        .mapToDouble(BigDecimal::doubleValue)
                        .average().orElse(0),
                viewList.stream()
                        .map(CompanyValuationViewModel::getSubmitDateRatio)
                        .mapToDouble(BigDecimal::doubleValue)
                        .average().orElse(0),
                viewList.stream()
                        .map(CompanyValuationViewModel::getGrahamIndex)
                        .filter(Objects::nonNull)
                        .mapToDouble(BigDecimal::doubleValue)
                        .average().orElse(0),
                viewList.size()
        );
    }

    /**
     * 評価結果を取得する
     * <ul>
     *    <li>キャッシュがあるときはキャッシュから取得する<li/>
     *    <li>キャッシュがないときはデータベースから取得する<li/>
     * </>
     *
     * @return 評価結果リスト
     */
    @Cacheable(CACHE_KEY_ALL_VALUATION_VIEW)
    public List<CompanyValuationViewModel> inquiryAllValuationView() {
        return findAllValuationView();
    }

    @CachePut(CACHE_KEY_ALL_VALUATION_VIEW)
    public List<CompanyValuationViewModel> findAllValuationView() {
        final ArrayList<CompanyValuationViewModel> viewList = new ArrayList<>();
        companySpecification.inquiryAllTargetCompanies()
                .forEach(company -> valuationDao.selectByCode(company.getCode()).stream()
                        .max(Comparator.comparing(ValuationEntity::getTargetDate).thenComparing(ValuationEntity::getSubmitDate))
                        .ifPresent(valuationEntity -> viewList.add(CompanyValuationViewModel.of(valuationEntity, company))));
        return viewList;
    }

    /**
     * 評価結果を取得する
     *
     * @param companyCode 企業コード
     * @return 評価結果リスト
     */
    public List<CompanyValuationViewModel> findValuationView(final String companyCode) {
        return companySpecification.findCompanyByCode(companyCode)
                .map(company -> {
                            final List<ValuationEntity> entityList = valuationDao.selectByCode(company.getCode());
                            return entityList.stream()
                                    .map(ValuationEntity::getTargetDate)
                                    .distinct()
                                    // 最新の提出日を取得する
                                    .map(targetDate -> entityList.stream()
                                            .filter(e -> targetDate.equals(e.getTargetDate()))
                                            .max(Comparator.comparing(ValuationEntity::getSubmitDate))
                                            .orElseThrow()
                                    )
                                    .map(e -> CompanyValuationViewModel.of(e, company))
                                    .toList();
                        }
                ).orElseGet(List::of);
    }

    /**
     * 評価結果を登録する
     *
     * @param stock          株価
     * @param analysisResult 分析結果
     */
    public void insert(final StockPriceEntity stock, final AnalysisResultEntity analysisResult) {
        try {
            valuationDao.insert(evaluate(stock, analysisResult));
        } catch (final NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "一意制約違反のため、データベースへの登録をスキップします。" +
                                        "\tテーブル名:{0}\t企業コード:{1}\t対象日付:{2}\t株価:{3}",
                                "valuation",
                                stock.getCompanyCode(),
                                stock.getTargetDate(),
                                stock.getStockPrice().orElse(0.0)
                        ),
                        companySpecification.findCompanyByCode(stock.getCompanyCode()).map(Company::getEdinetCode).orElse("null"),
                        Category.STOCK,
                        Process.EVALUATE
                ), e);
            } else if (e.contains(SQLIntegrityConstraintViolationException.class)) {
                log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "整合性制約 (外部キー、主キー、または一意キー) 違反のため、データベースへの登録をスキップします。" +
                                        "\tテーブル名:{0}\t企業コード:{0}\t対象日付:{1}\t株価:{2}",
                                "valuation",
                                stock.getCompanyCode(),
                                stock.getTargetDate(),
                                stock.getStockPrice().orElse(0.0)
                        ),
                        companySpecification.findCompanyByCode(stock.getCompanyCode()).map(Company::getEdinetCode).orElse("null"),
                        Category.STOCK,
                        Process.EVALUATE
                ), e);
                // TODO
            } else if (e.contains(NumberFormatException.class)) {
                log.warn(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "予想配当利回りを数値に変換できませんでした。\t値:{0}", stock.getDividendYield().orElse(null)
                        ),
                        companySpecification.findCompanyByCode(stock.getCompanyCode()).map(Company::getEdinetCode).orElse("null"),
                        Category.STOCK,
                        Process.EVALUATE
                ), e.getCause());
            } else {
                throw e;
            }
        }
    }

    /**
     * 株価を評価する
     *
     * @param stock          株価
     * @param analysisResult 分析結果
     * @return 評価結果
     */
    ValuationEntity evaluate(final StockPriceEntity stock, final AnalysisResultEntity analysisResult) {
        final String code = stock.getCompanyCode();
        final LocalDate targetDate = stock.getTargetDate();
        final BigDecimal stockPrice = stock.getStockPrice().map(BigDecimal::valueOf)
                .orElseThrow(() -> new FundanalyzerNotExistException("株価終値"));
        final Optional<InvestmentIndicatorEntity> investmentIndicatorEntity = investmentIndicatorSpecification.findEntity(code, targetDate);
        final LocalDate submitDate = analysisResult.getSubmitDate();
        final BigDecimal stockPriceOfSubmitDate = stockSpecification.findStock(code, submitDate)
                .flatMap(StockPriceEntity::getStockPrice)
                .map(BigDecimal::valueOf).orElseThrow(() -> new FundanalyzerNotExistException("提出日株価終値"));

        return ValuationEntity.of(
                code,
                submitDate,
                targetDate,
                stock.getId(),
                stockPrice,
// TODO
//                stock.getDividendYield().map(v -> new BigDecimal(v
//                        .replace("%", "").replace("％", "")
//                        .replace(" ", "").replace("　", "")
//                )).orElse(null),
                investmentIndicatorEntity.map(InvestmentIndicatorEntity::getId).orElse(null),
                investmentIndicatorEntity.flatMap(InvestmentIndicatorEntity::getGrahamIndex).orElse(null),
                ChronoUnit.DAYS.between(submitDate, targetDate),
                stockPrice.subtract(stockPriceOfSubmitDate),
                stockPrice.divide(stockPriceOfSubmitDate, SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP),
                analysisResult.getCorporateValue().subtract(stockPrice),
                analysisResult.getCorporateValue().divide(stockPrice, SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP),
                nowLocalDateTime()
        );
    }
}
