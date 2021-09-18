package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.MinkabuDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.MinkabuEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean.Kabuoji3ResultBean;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean.MinkabuResultBean;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean.NikkeiResultBean;
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
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class StockSpecification {

    private static final Logger log = LogManager.getLogger(StockSpecification.class);
    private static final int SECOND_DECIMAL_PLACE = 2;

    private final StockPriceDao stockPriceDao;
    private final MinkabuDao minkabuDao;
    private final DocumentSpecification documentSpecification;

    @Value("${app.config.stock.average-stock-price-for-last-days}")
    int daysToAverageStockPrice;
    @Value("${app.config.stock.store-stock-price-for-last-days}")
    int daysToStoreStockPrice;

    public StockSpecification(
            final StockPriceDao stockPriceDao,
            final MinkabuDao minkabuDao,
            final DocumentSpecification documentSpecification) {
        this.stockPriceDao = stockPriceDao;
        this.minkabuDao = minkabuDao;
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
     * @param company 企業情報
     * @return 株価情報
     */
    public Stock findStock(final Company company) {
        final List<StockPriceEntity> stockPriceList = stockPriceDao.selectByCode(company.getCode().orElseThrow(FundanalyzerNotExistException::new));

        final Optional<LocalDate> importDate = stockPriceList.stream()
                .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                .map(StockPriceEntity::getTargetDate);

        final Optional<BigDecimal> latestStock = stockPriceList.stream()
                .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                .map(StockPriceEntity::getStockPrice)
                .map(BigDecimal::valueOf);

        final Optional<BigDecimal> averageStockPrice = averageStockPrice(company, stockPriceList);

        final Optional<BigDecimal> latestForecastStock = minkabuDao.selectByCode(company.getCode().orElseThrow(FundanalyzerNotExistException::new)).stream()
                .max(Comparator.comparing(MinkabuEntity::getTargetDate))
                .map(MinkabuEntity::getGoalsStock)
                .map(BigDecimal::new);

        return Stock.of(
                company,
                averageStockPrice.orElse(null),
                importDate.orElse(null),
                latestStock.orElse(null),
                latestForecastStock.orElse(null),
                stockPriceList,
                minkabuDao.selectByCode(company.getCode().orElseThrow(FundanalyzerNotExistException::new))
        );
    }

    /**
     * 削除対象となる日付を取得する
     *
     * @return 対象日付リスト
     */
    public List<LocalDate> findTargetDateToDelete() {
        return stockPriceDao.selectDistinctTargetDate().stream()
                .filter(targetDate -> targetDate.isBefore(nowLocalDate().minusDays(daysToStoreStockPrice)))
                .collect(Collectors.toList());
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
     * @param code         企業コード
     * @param kabuoji3List kabuoji3から取得した株価情報
     */
    public void insert(final String code, final List<Kabuoji3ResultBean> kabuoji3List) {
        kabuoji3List.stream()
                // 保存する株価を絞る
                .filter(kabuoji3 -> LocalDate.parse(kabuoji3.getTargetDate())
                        .isAfter(nowLocalDate().minusDays(daysToStoreStockPrice)))
                .forEach(kabuoji3 -> {
                    if (isEmptyStockPrice(code, kabuoji3.getTargetDate())) {
                        try {
                            stockPriceDao.insert(StockPriceEntity.ofKabuoji3ResultBean(code, kabuoji3, nowLocalDateTime()));
                        } catch (NestedRuntimeException e) {
                            if (e.contains(UniqueConstraintException.class)) {
                                log.debug("一意制約違反のため、株価情報のデータベース登録をスキップします。" +
                                        "\t企業コード:{}\t対象日:{}", code, kabuoji3.getTargetDate());
                            } else {
                                throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
                            }
                        }
                    }
                });
    }

    /**
     * みんかぶから取得した株価情報を登録する
     *
     * @param code    企業コード
     * @param minkabu みんかぶから取得した株価情報
     */
    public void insert(final String code, final MinkabuResultBean minkabu) {
        if (!isPresentMinkabu(code, minkabu.getTargetDate())) {
            minkabuDao.insert(MinkabuEntity.ofMinkabuResultBean(code, minkabu, nowLocalDateTime()));
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
     * @param company        企業情報
     * @param stockPriceList 株価情報リスト
     * @return 平均の株価
     */
    private Optional<BigDecimal> averageStockPrice(final Company company, final List<StockPriceEntity> stockPriceList) {
        final Optional<LocalDate> submitDate = documentSpecification.latestDocument(company).map(Document::getSubmitDate);

        if (submitDate.isEmpty()) {
            return Optional.empty();
        }

        final List<Double> certainPeriodList = stockPriceList.stream()
                .filter(stockPrice -> submitDate.get().minusDays(daysToAverageStockPrice).isBefore(stockPrice.getTargetDate()))
                .filter(stockPrice -> submitDate.get().isAfter(stockPrice.getTargetDate()))
                .map(StockPriceEntity::getStockPrice)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

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
            } else {
                targetDate = LocalDate.parse(targetDateAsString);
            }
            return stockPriceDao.selectByCodeAndDate(code, targetDate).isEmpty();
        } catch (final NullPointerException e) {
            log.info(FundanalyzerLogClient.toSpecificationLogObject(
                    MessageFormat.format(
                            "{} の株価取得スクレイピング処理にて日付を取得できなかったため、本日の日付に置き換えて後続処理を実行します。", code),
                    Category.STOCK,
                    Process.REGISTER
            ), e);
            return false;
        }
    }

    /**
     * みんかぶ情報がデータベースに存在するか
     *
     * @param code               企業コード
     * @param targetDateAsString 対象日
     * @return boolean
     */
    private boolean isPresentMinkabu(final String code, final String targetDateAsString) {
        LocalDate targetDate = LocalDate.now();
        try {
            targetDate = MonthDay.parse(targetDateAsString, DateTimeFormatter.ofPattern("MM/dd")).atYear(targetDate.getYear());
        } catch (final DateTimeException e) {
            log.info("みんかぶのスクレイピング処理で期待の対象日が得られませんでした。本日日付で処理を継続します。" +
                    "\t企業コード:{}\tスクレイピング結果:{}\t登録対象日:{}", code, targetDateAsString, targetDate, e);
        }
        return minkabuDao.selectByCodeAndDate(code, targetDate).isPresent();
    }
}
