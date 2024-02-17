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
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SourceOfStockPrice;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.util.Parser;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.Stock;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerScrapingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLIntegrityConstraintViolationException;
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
        return stockPriceDao.selectByCodeAndDate(companyCode, targetDate).stream().findAny();
    }

    /**
     * 最新の株価情報を取得する
     *
     * @param companyCode 企業コード
     * @return 株価情報
     */
    public Optional<StockPriceEntity> findLatestStock(final String companyCode) {
        return stockPriceDao.selectByCode(companyCode).stream()
                // latest
                .max(Comparator.comparing(StockPriceEntity::getTargetDate));
    }

    /**
     * 株価情報を取得する
     *
     * @param companyCode 企業コード
     * @return 株価情報
     */
    public List<StockPriceEntity> findEntityList(final String companyCode) {
        return stockPriceDao.selectByCode(companyCode);
    }

    /**
     * 株価情報を取得する
     *
     * @param company 企業情報
     * @return 株価情報
     */
    public Stock findStock(final Company company) {
        final List<StockPriceEntity> stockPriceList = stockPriceDao.selectByCode(company.code());

        final Optional<LocalDate> importDate = stockPriceList.stream()
                .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                .map(StockPriceEntity::getTargetDate);

        final Optional<BigDecimal> latestStock = stockPriceList.stream()
                .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                .map(StockPriceEntity::getStockPrice)
                .map(BigDecimal::valueOf);

        final Optional<BigDecimal> averageStockPrice = documentSpecification.findLatestDocument(company)
                .map(Document::getSubmitDate)
                .flatMap(sd -> getAverageStockPriceOfLatestSubmitDate(sd, stockPriceList));

        final List<MinkabuEntity> minkabuList = minkabuDao.selectByCode(company.code());
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
                .map(Company::code)
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
     * 日経から取得した株価情報を登録・更新する
     *
     * @param code   企業コード
     * @param nikkei 日経から取得した株価情報
     * @throws DateTimeParseException 対象日をパースできなかったとき
     */
    public void upsert(final String code, final NikkeiResultBean nikkei) throws DateTimeParseException {
        if (nikkei.targetDate() == null) {
            throw new FundanalyzerScrapingException("株価取得スクレイピング処理において対象日を取得できませんでした");
        }

        StockPriceEntity entity;
        if (Parser.parseDoubleNikkei(nikkei.stockPrice()).isEmpty()) {
            final LocalDate targetDate = LocalDate.parse(nikkei.targetDate(), DateTimeFormatter.ofPattern("yyyy/M/d"));
            final Double stockPrice = findStock(code, targetDate)
                    .map(StockPriceEntity::getStockPrice)
                    .orElseThrow(() -> new FundanalyzerScrapingException("株価取得スクレイピング処理において株価終値を取得できませんでした"));
            entity = StockPriceEntity.ofNikkeiResultBean(code, stockPrice, nikkei, nowLocalDateTime());
        } else {
            entity = StockPriceEntity.ofNikkeiResultBean(code, nikkei, nowLocalDateTime());
        }

        if (stockPriceDao.selectByUniqueKey(entity.getCompanyCode(), entity.getTargetDate(), SourceOfStockPrice.NIKKEI.toValue()).isEmpty()) {
            insert(entity, SourceOfStockPrice.NIKKEI);
        } else {
            stockPriceDao.update(entity);
        }
    }

    /**
     * 取得した株価情報を登録・更新する
     *
     * @param code       企業コード
     * @param resultBean 取得した株価情報
     * @param price      取得先
     * @throws DateTimeParseException 対象日をパースできなかったとき
     */
    public void upsert(
            final String code,
            final StockPriceResultBean resultBean,
            final SourceOfStockPrice price) throws DateTimeParseException {
        if (resultBean.targetDate() == null) {
            throw new FundanalyzerScrapingException("株価取得スクレイピング処理において対象日を取得できませんでした");
        }
        if (resultBean.closingPrice() == null) {
            throw new FundanalyzerScrapingException("株価取得スクレイピング処理において株価終値を取得できませんでした");
        }

        final StockPriceEntity entity = switch (price) {
            case KABUOJI3 -> StockPriceEntity.ofKabuoji3(code, resultBean, nowLocalDateTime());
            case MINKABU -> StockPriceEntity.ofMinkabu(code, resultBean, nowLocalDateTime());
            case YAHOO_FINANCE -> StockPriceEntity.ofYahooFinanceResultBean(code, resultBean, nowLocalDateTime());
            default -> throw new FundanalyzerRuntimeException();
        };

        if (stockPriceDao.selectByCodeAndDate(entity.getCompanyCode(), entity.getTargetDate()).isEmpty()) {
            insert(entity, price);
        } else {
            stockPriceDao.update(entity);
        }
    }

    void insert(final StockPriceEntity entity, final SourceOfStockPrice price) {
        if (entity.getTargetDate().isAfter(nowLocalDate())) {
            throw new FundanalyzerScrapingException("株価取得スクレイピング処理において対象日を正しく取得できませんでした");
        }

        try {
            stockPriceDao.insert(entity);
        } catch (final NestedRuntimeException e) {
            handleDaoError(
                    e,
                    "stock_price",
                    entity.getCompanyCode(),
                    entity.getTargetDate(),
                    entity.getStockPrice(),
                    price.getMemo()
            );
        }
    }

    /**
     * みんかぶから取得した予想株価を登録・更新する
     *
     * @param code    企業コード
     * @param minkabu みんかぶから取得した予想株価
     */
    public void insert(final String code, final MinkabuResultBean minkabu) {
        LocalDate targetDate;

        if (Pattern.compile("^([0-1]\\d|2[0-3]):[0-5]\\d$").matcher(minkabu.targetDate()).find()) {
            targetDate = nowLocalDate();
        } else if ("--:--".equals(minkabu.targetDate())) {
            log.trace(FundanalyzerLogClient.toSpecificationLogObject(
                    MessageFormat.format(
                            "みんかぶの予想株価スクレイピング処理で期待の対象日が得られませんでした。登録をスキップします。" +
                            "\t企業コード:{0}\tスクレイピング結果:{1}",
                            code,
                            minkabu.targetDate()
                    ),
                    companySpecification.findCompanyByCode(code).map(Company::edinetCode).orElse("null"),
                    Category.STOCK,
                    Process.REGISTER
            ));

            return;
        } else {
            try {
                targetDate = MonthDay.parse(minkabu.targetDate(), DateTimeFormatter.ofPattern("MM/dd")).atYear(nowLocalDate().getYear());
            } catch (final DateTimeParseException e) {
                log.warn(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "みんかぶの予想株価スクレイピング処理で期待の対象日が得られませんでした。登録をスキップします。" +
                                "\t企業コード:{0}\tスクレイピング結果:{1}",
                                code,
                                minkabu.targetDate()
                        ),
                        companySpecification.findCompanyByCode(code).map(Company::edinetCode).orElse("null"),
                        Category.STOCK,
                        Process.REGISTER
                ), e);

                return;
            }
        }

        if (targetDate.isAfter(nowLocalDate())) {
            throw new FundanalyzerScrapingException("株価取得スクレイピング処理において対象日を正しく取得できませんでした");
        }

        if (!isPresentMinkabu(code, targetDate)) {
            try {
                minkabuDao.insert(MinkabuEntity.ofMinkabuResultBean(code, targetDate, minkabu, nowLocalDateTime()));
            } catch (NestedRuntimeException e) {
                handleDaoError(
                        e,
                        "minkabu",
                        code,
                        targetDate,
                        0.0,
                        SourceOfStockPrice.MINKABU.getMemo()
                );
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
     * 対象日から前の特定期間における平均の株価を取得する
     *
     * @param companyCode 企業コード
     * @param targetDate  対象日
     * @return 平均の株価
     */
    public Optional<BigDecimal> getAverageStockPrice(final String companyCode, final LocalDate targetDate) {
        final Company company = companySpecification.findCompanyByCode(companyCode).orElseThrow(() -> new FundanalyzerNotExistException("企業"));
        return getAverageStockPriceOfLatestSubmitDate(
                targetDate,
                stockPriceDao.selectByCode(company.code())
        );
    }

    /**
     * 特定期間における平均の株価を取得する
     *
     * @param targetDate     対象日
     * @param stockPriceList 株価情報リスト
     * @return 平均の株価
     */
    private Optional<BigDecimal> getAverageStockPriceOfLatestSubmitDate(
            final LocalDate targetDate, final List<StockPriceEntity> stockPriceList) {
        final List<Double> certainPeriodList = stockPriceList.stream()
                .filter(stockPrice -> targetDate.minusDays(daysToAverageStockPrice).isBefore(stockPrice.getTargetDate()))
                .filter(stockPrice -> targetDate.isAfter(stockPrice.getTargetDate()))
                .map(StockPriceEntity::getStockPrice)
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
     * みんかぶ予想株価がデータベースに存在するか
     *
     * @param code       企業コード
     * @param targetDate 対象日
     * @return boolean
     */
    private boolean isPresentMinkabu(final String code, final LocalDate targetDate) {
        return minkabuDao.selectByCodeAndDate(code, targetDate).isPresent();
    }

    private void handleDaoError(
            final NestedRuntimeException e,
            final String tableName,
            final String code,
            final LocalDate targetDate,
            final Double stockPrice,
            final String placeMemo) {
        if (e.contains(UniqueConstraintException.class)) {
            log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                    MessageFormat.format(
                            "一意制約違反のため、データベースへの登録をスキップします。" +
                            "\tテーブル名:{0}\t企業コード:{1}\t対象日:{2}\t取得先:{3}",
                            tableName,
                            code,
                            targetDate,
                            placeMemo
                    ),
                    companySpecification.findCompanyByCode(code).map(Company::edinetCode).orElse("null"),
                    Category.STOCK,
                    Process.REGISTER
            ));
        } else if (e.contains(SQLIntegrityConstraintViolationException.class)) {
            log.warn(FundanalyzerLogClient.toSpecificationLogObject(
                    MessageFormat.format(
                            "整合性制約 (外部キー、主キー、または一意キー) 違反のため、データベースへの登録をスキップします。" +
                            "\tテーブル名:{0}\t企業コード:{1}\t対象日:{2}\t株価終値:{3}\t取得先:{4}",
                            tableName,
                            code,
                            targetDate,
                            stockPrice,
                            placeMemo
                    ),
                    companySpecification.findCompanyByCode(code).map(Company::edinetCode).orElse("null"),
                    Category.STOCK,
                    Process.REGISTER
            ));
        } else {
            throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
        }
    }
}
