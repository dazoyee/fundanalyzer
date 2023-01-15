package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.jsoup.result.MinkabuResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.NikkeiResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.StockPriceResultBean;
import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.MinkabuDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.MinkabuEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.Stock;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class StockSpecification {

    private static final Logger log = LogManager.getLogger(StockSpecification.class);
    private static final int SECOND_DECIMAL_PLACE = 2;

    private final StockPriceDao stockPriceDao;
    private final MinkabuDao minkabuDao;
    private final CompanySpecification companySpecification;
    private final DocumentSpecification documentSpecification;

    @Value("${app.config.stock.average-stock-price-for-last-days}")
    int daysToAverageStockPrice;
    @Value("${app.scheduler.stock.target-company.number}")
    int targetCompanyNumber;
    @Value("${app.config.stock.store-stock-price-for-last-days}")
    int daysToStoreStockPrice;

    public StockSpecification(
            final StockPriceDao stockPriceDao,
            final MinkabuDao minkabuDao,
            final CompanySpecification companySpecification,
            final DocumentSpecification documentSpecification) {
        this.stockPriceDao = stockPriceDao;
        this.minkabuDao = minkabuDao;
        this.companySpecification = companySpecification;
        this.documentSpecification = documentSpecification;
    }

    LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 株価情報を取得する
     *
     * @param companyCode 企業コード
     * @param targetDate  対象日付
     * @return 株価情報
     */
    public Optional<StockPriceEntity> findStock(final String companyCode, final LocalDate targetDate) {
        return stockPriceDao.selectByCodeAndDate(companyCode, targetDate).stream()
                .filter(entity -> entity.getStockPrice().isPresent())
                .findAny();
    }

    /**
     * 最新の株価情報を取得する
     *
     * @param companyCode 企業コード
     * @return 株価情報
     */
    public Optional<StockPriceEntity> findLatestStock(final String companyCode) {
        return stockPriceDao.selectByCode(companyCode).stream()
                .filter(entity -> entity.getStockPrice().isPresent())
                // latest
                .max(Comparator.comparing(StockPriceEntity::getTargetDate));
    }

    /**
     * 株価情報を取得する
     *
     * @param company 企業情報
     * @return 株価情報
     */
    public Stock findStock(final Company company) {
        final List<StockPriceEntity> stockPriceList = stockPriceDao.selectByCode(company.getCode());

        final Optional<LocalDate> importDate = stockPriceList.stream()
                .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                .map(StockPriceEntity::getTargetDate);

        final Optional<BigDecimal> latestStock = stockPriceList.stream()
                .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                .flatMap(StockPriceEntity::getStockPrice)
                .map(BigDecimal::valueOf);

        final Optional<BigDecimal> averageStockPrice = getAverageStockPriceOfLatestSubmitDate(company, stockPriceList);

        final List<MinkabuEntity> minkabuList = minkabuDao.selectByCode(company.getCode());
        final Optional<BigDecimal> latestForecastStock = minkabuList.stream()
                .max(Comparator.comparing(MinkabuEntity::getTargetDate))
                .flatMap(MinkabuEntity::getGoalsStock)
                .map(BigDecimal::new);

        return Stock.of(
                company,
                averageStockPrice.orElse(null),
                importDate.orElse(null),
                latestStock.orElse(null),
                latestForecastStock.orElse(null),
                stockPriceList,
                minkabuList
        );
    }

    /**
     * スケジューラで株価取得するときの対象会社リストを取得する
     *
     * @return 会社コードリスト
     */
    public List<String> findTargetCodeForStockScheduler() {
        return companySpecification.inquiryAllTargetCompanies().stream()
                .map(Company::getCode)
                // 会社毎に最新の登録日を抽出する
                .map(code -> stockPriceDao.selectByCode(code).stream()
                        .max(Comparator.comparing(StockPriceEntity::getCreatedAt)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(StockPriceEntity::getCreatedAt))
                .limit(targetCompanyNumber)
                .map(StockPriceEntity::getCompanyCode)
                .toList();
    }

    /**
     * 削除対象となる日付を取得する
     *
     * @return 対象日付リスト
     */
    public List<LocalDate> findTargetDateToDelete() {
        return stockPriceDao.selectDistinctTargetDate().stream()
                .filter(targetDate -> targetDate.isBefore(nowLocalDate().minusDays(daysToStoreStockPrice)))
                .toList();
    }

    /**
     * 日経から取得した株価情報を登録する
     *
     * @param code   企業コード
     * @param nikkei 日経から取得した株価情報
     */
    public void insert(final String code, final NikkeiResultBean nikkei) {
        if (isEmptyStockPrice(code, nikkei.getTargetDate())) {
            stockPriceDao.insert(StockPriceEntity.ofNikkeiResultBean(code, nikkei, nowLocalDateTime()));
        }
    }

    /**
     * kabuoji3から取得した株価情報を登録する
     *
     * @param code     企業コード
     * @param kabuoji3 kabuoji3から取得した株価情報
     */
    public void insertOfKabuoji3(final String code, final StockPriceResultBean kabuoji3) {
        if (isEmptyStockPrice(code, kabuoji3.targetDate())) {
            try {
                stockPriceDao.insert(StockPriceEntity.ofKabuoji3(code, kabuoji3, nowLocalDateTime()));
            } catch (NestedRuntimeException e) {
                if (e.contains(UniqueConstraintException.class)) {
                    log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                            MessageFormat.format(
                                    "一意制約違反のため、株価情報のデータベース登録をスキップします。" +
                                            "\t企業コード:{0}\t対象日:{1}", code, kabuoji3.targetDate()
                            ),
                            companySpecification.findCompanyByCode(code).map(Company::getEdinetCode).orElse("null"),
                            Category.STOCK,
                            Process.REGISTER
                    ));
                } else {
                    throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
                }
            }
        }
    }

    /**
     * みんかぶから取得した予想株価を登録する
     *
     * @param code    企業コード
     * @param minkabu みんかぶから取得した予想株価
     */
    public void insert(final String code, final MinkabuResultBean minkabu) {
        LocalDate targetDate;
        try {
            targetDate = MonthDay.parse(minkabu.getTargetDate(), DateTimeFormatter.ofPattern("MM/dd")).atYear(nowLocalDate().getYear());
        } catch (DateTimeParseException e) {
            if (Pattern.compile("^([0-1]\\d|2[0-3]):[0-5]\\d$").matcher(minkabu.getTargetDate()).find()) {
                targetDate = nowLocalDate();
            } else if ("--:--".equals(minkabu.getTargetDate())) {
                log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "みんかぶの予想株価スクレイピング処理で期待の対象日が得られませんでした。登録をスキップします。" +
                                        "\t企業コード:{0}\tスクレイピング結果:{1}",
                                code,
                                minkabu.getTargetDate()
                        ),
                        companySpecification.findCompanyByCode(code).map(Company::getEdinetCode).orElse("null"),
                        Category.STOCK,
                        Process.REGISTER
                ), e);

                return;
            } else {
                log.warn(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "みんかぶの予想株価スクレイピング処理で期待の対象日が得られませんでした。登録をスキップします。" +
                                        "\t企業コード:{0}\tスクレイピング結果:{1}",
                                code,
                                minkabu.getTargetDate()
                        ),
                        companySpecification.findCompanyByCode(code).map(Company::getEdinetCode).orElse("null"),
                        Category.STOCK,
                        Process.REGISTER
                ), e);

                return;
            }
        }

        if (!isPresentMinkabu(code, targetDate)) {
            minkabuDao.insert(MinkabuEntity.ofMinkabuResultBean(code, targetDate, minkabu, nowLocalDateTime()));
        }
    }

    /**
     * みんかぶから取得した株価情報を登録する
     *
     * @param code    企業コード
     * @param minkabu minkabuから取得した株価情報
     */
    public void insertOfMinkabu(final String code, final StockPriceResultBean minkabu) {
        if (isEmptyStockPrice(code, minkabu.targetDate())) {
            try {
                stockPriceDao.insert(StockPriceEntity.ofMinkabu(code, minkabu, nowLocalDateTime()));
            } catch (NestedRuntimeException e) {
                if (e.contains(UniqueConstraintException.class)) {
                    log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                            MessageFormat.format(
                                    "一意制約違反のため、株価情報のデータベース登録をスキップします。" +
                                            "\t企業コード:{0}\t対象日:{1}", code, minkabu.targetDate()
                            ),
                            companySpecification.findCompanyByCode(code).map(Company::getEdinetCode).orElse("null"),
                            Category.STOCK,
                            Process.REGISTER
                    ));
                } else {
                    throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
                }
            }
        }
    }

    /**
     * yahoo-financeから取得した株価情報を登録する
     *
     * @param code         企業コード
     * @param yahooFinance yahoo-financeから取得した株価情報
     */
    public void insertOfYahooFinance(final String code, final StockPriceResultBean yahooFinance) {
        if (isEmptyStockPrice(code, yahooFinance.targetDate())) {
            try {
                stockPriceDao.insert(StockPriceEntity.ofYahooFinanceResultBean(code, yahooFinance, nowLocalDateTime()));
            } catch (NestedRuntimeException e) {
                if (e.contains(UniqueConstraintException.class)) {
                    log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                            MessageFormat.format(
                                    "一意制約違反のため、株価情報のデータベース登録をスキップします。" +
                                            "\t企業コード:{0}\t対象日:{1}", code, yahooFinance.targetDate()
                            ),
                            companySpecification.findCompanyByCode(code).map(Company::getEdinetCode).orElse("null"),
                            Category.STOCK,
                            Process.REGISTER
                    ));
                } else {
                    throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
                }
            }
        }
    }

    /**
     * 対象日付の株価を削除する
     *
     * @param targetDate 対象日付
     */
    public int delete(final LocalDate targetDate) {
        return stockPriceDao.delete(targetDate);
    }

    /**
     * 特定期間における平均の株価を取得する
     *
     * @param companyCode 企業コード
     * @return 平均の株価
     */
    public Optional<BigDecimal> getAverageStockPriceOfLatestSubmitDate(final String companyCode) {
        final Company company = companySpecification.findCompanyByCode(companyCode).orElseThrow(() -> new FundanalyzerNotExistException("企業"));
        return getAverageStockPriceOfLatestSubmitDate(
                company,
                stockPriceDao.selectByCode(company.getCode())
        );
    }

    /**
     * 予想株価を取得する
     *
     * @param companyCode 企業コード
     * @param targetDate  対象日付
     * @return 予想株価
     */
    public Optional<Double> findForecastStock(final String companyCode, final LocalDate targetDate) {
        return minkabuDao.selectByCodeAndDate(companyCode, targetDate).flatMap(MinkabuEntity::getGoalsStock);
    }

    /**
     * 特定期間における平均の株価を取得する
     *
     * @param company        企業情報
     * @param stockPriceList 株価情報リスト
     * @return 平均の株価
     */
    private Optional<BigDecimal> getAverageStockPriceOfLatestSubmitDate(
            final Company company, final List<StockPriceEntity> stockPriceList) {
        final Optional<LocalDate> submitDate = documentSpecification.findLatestDocument(company).map(Document::getSubmitDate);

        if (submitDate.isEmpty()) {
            return Optional.empty();
        }

        final List<Double> certainPeriodList = stockPriceList.stream()
                .filter(stockPrice -> submitDate.get().minusDays(daysToAverageStockPrice).isBefore(stockPrice.getTargetDate()))
                .filter(stockPrice -> submitDate.get().isAfter(stockPrice.getTargetDate()))
                .map(StockPriceEntity::getStockPrice)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (certainPeriodList.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(certainPeriodList.stream()
                    .map(BigDecimal::valueOf)
                    // sum
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    // average
                    .divide(BigDecimal.valueOf(certainPeriodList.size()), SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP));
        }
    }

    /**
     * 株価情報がデータベースに存在するか
     *
     * @param code               企業コード
     * @param targetDateAsString 対象日
     * @return boolean
     */
    private boolean isEmptyStockPrice(final String code, final String targetDateAsString) {
        try {
            final LocalDate targetDate;
            if (targetDateAsString.contains("/")) {
                targetDate = LocalDate.parse(targetDateAsString, DateTimeFormatter.ofPattern("yyyy/M/d"));
            } else if (targetDateAsString.contains("月") && targetDateAsString.contains("日")) {
                targetDate = LocalDate.parse(targetDateAsString, DateTimeFormatter.ofPattern("yyyy年M月d日"));
            } else {
                targetDate = LocalDate.parse(targetDateAsString);
            }
            return stockPriceDao.selectByCodeAndDate(code, targetDate).isEmpty();
        } catch (final NullPointerException e) {
            log.info(FundanalyzerLogClient.toSpecificationLogObject(
                    MessageFormat.format(
                            "{0} の株価取得スクレイピング処理にて日付を取得できなかったため、本日の日付に置き換えて後続処理を実行します。"
                            , code
                    ),
                    companySpecification.findCompanyByCode(code).map(Company::getEdinetCode).orElse("null"),
                    Category.STOCK,
                    Process.REGISTER
            ), e);
            return false;
        }
    }

    /**
     * みんかぶ予想株価がデータベースに存在するか
     *
     * @param code       企業コード
     * @param targetDate 対象日
     * @return boolean
     */
    private boolean isPresentMinkabu(final String code, final LocalDate targetDate) {
        return minkabuDao.selectByCodeAndDate(code, targetDate).isPresent();
    }
}
