package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.client.jsoup.result.Kabuoji3ResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.NikkeiResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.YahooFinanceResultBean;
import github.com.ioridazo.fundanalyzer.domain.util.Parser;
import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "stock_price")
public class StockPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Integer id;

    private final String companyCode;

    private final LocalDate targetDate;

    private final Double stockPrice;

    private final Double openingPrice;

    private final Double highPrice;

    private final Double lowPrice;

    private final Integer volume;

    private final String per;

    private final String pbr;

    private final String roe;

    private final String numberOfShares;

    private final String marketCapitalization;

    private final String dividendYield;

    private final String shareholderBenefit;

    private final String sourceOf;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    /**
     * 日経のスクレイピング結果からデータベース登録するためにマッピングする
     *
     * @param code      会社コード
     * @param nikkei    日経スクレイピング結果
     * @param createdAt 登録日
     * @return StockPrice
     */
    public static StockPriceEntity ofNikkeiResultBean(
            final String code, final NikkeiResultBean nikkei, final LocalDateTime createdAt) {
        return new StockPriceEntity(
                null,
                code,
                LocalDate.parse(nikkei.getTargetDate(), DateTimeFormatter.ofPattern("yyyy/M/d")),
                Parser.parseDoubleNikkei(nikkei.getStockPrice()).orElse(null),
                Parser.parseDoubleNikkei(nikkei.getOpeningPrice()).orElse(null),
                Parser.parseDoubleNikkei(nikkei.getHighPrice()).orElse(null),
                Parser.parseDoubleNikkei(nikkei.getLowPrice()).orElse(null),
                Parser.parseIntegerVolume(nikkei.getVolume()).orElse(null),
                Parser.parseStringNikkei(nikkei.getPer()),
                Parser.parseStringNikkei(nikkei.getPbr()),
                Parser.parseStringNikkei(nikkei.getRoe()),
                Parser.parseStringNikkei(nikkei.getNumberOfShares()),
                Parser.parseStringNikkei(nikkei.getMarketCapitalization()),
                Parser.parseStringNikkei(nikkei.getDividendYield()),
                nikkei.getShareholderBenefit().replace("株主優待 ", ""),
                "1",
                createdAt
        );
    }

    /**
     * kabuoji3のスクレイピング結果からデータベース登録するためにマッピングする
     *
     * @param code      会社コード
     * @param kabuoji3  kabuoji3のスクレイピング結果
     * @param createdAt 登録日
     * @return StockPrice
     */
    public static StockPriceEntity ofKabuoji3ResultBean(
            final String code, final Kabuoji3ResultBean kabuoji3, final LocalDateTime createdAt) {
        return new StockPriceEntity(
                null,
                code,
                LocalDate.parse(kabuoji3.getTargetDate()),
                Parser.parseDoubleKabuoji3(kabuoji3.getClosingPrice()).orElse(null),
                Parser.parseDoubleKabuoji3(kabuoji3.getOpeningPrice()).orElse(null),
                Parser.parseDoubleKabuoji3(kabuoji3.getHighPrice()).orElse(null),
                Parser.parseDoubleKabuoji3(kabuoji3.getLowPrice()).orElse(null),
                Parser.parseIntegerVolume(kabuoji3.getVolume()).orElse(null),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "2",
                createdAt
        );
    }

    /**
     * yahoo-financeのスクレイピング結果からデータベース登録するためにマッピングする
     *
     * @param code         会社コード
     * @param yahooFinance yahoo-financeのスクレイピング結果
     * @param createdAt    登録日
     * @return StockPrice
     */
    public static StockPriceEntity ofYahooFinanceResultBean(
            final String code, final YahooFinanceResultBean yahooFinance, final LocalDateTime createdAt) {
        return new StockPriceEntity(
                null,
                code,
                LocalDate.parse(yahooFinance.getTargetDate(), DateTimeFormatter.ofPattern("yyyy年M月d日")),
                Parser.parseDoubleYahooFinance(yahooFinance.getClosingPrice()).orElse(null),
                Parser.parseDoubleYahooFinance(yahooFinance.getOpeningPrice()).orElse(null),
                Parser.parseDoubleYahooFinance(yahooFinance.getHighPrice()).orElse(null),
                Parser.parseDoubleYahooFinance(yahooFinance.getLowPrice()).orElse(null),
                Parser.parseIntegerVolume(yahooFinance.getVolume()).orElse(null),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "3",
                createdAt
        );
    }

    /**
     * データベースに登録されている株価情報から銘柄詳細画面表示するためにマッピングする
     *
     * @param stockPriceEntity データベースに登録されている株価情報から
     * @return StockPrice
     */
    public static StockPriceEntity ofBrandDetail(final StockPriceEntity stockPriceEntity) {
        return new StockPriceEntity(
                null,
                null,
                stockPriceEntity.getTargetDate(),
                stockPriceEntity.getStockPrice().orElse(null),
                stockPriceEntity.getOpeningPrice().orElse(null),
                stockPriceEntity.getHighPrice().orElse(null),
                stockPriceEntity.getLowPrice().orElse(null),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public Optional<Double> getStockPrice() {
        return Optional.ofNullable(stockPrice);
    }

    public Optional<Double> getOpeningPrice() {
        return Optional.ofNullable(openingPrice);
    }

    public Optional<Double> getHighPrice() {
        return Optional.ofNullable(highPrice);
    }

    public Optional<Double> getLowPrice() {
        return Optional.ofNullable(lowPrice);
    }

    public Optional<Integer> getVolume() {
        return Optional.ofNullable(volume);
    }

    public Optional<String> getPer() {
        return Optional.ofNullable(per);
    }

    public Optional<String> getPbr() {
        return Optional.ofNullable(pbr);
    }

    public Optional<String> getRoe() {
        return Optional.ofNullable(roe);
    }

    public Optional<String> getNumberOfShares() {
        return Optional.ofNullable(numberOfShares);
    }

    public Optional<String> getMarketCapitalization() {
        return Optional.ofNullable(marketCapitalization);
    }

    public Optional<String> getDividendYield() {
        return Optional.ofNullable(dividendYield);
    }

    public Optional<String> getShareholderBenefit() {
        return Optional.ofNullable(shareholderBenefit);
    }

    public Optional<String> getSourceOf() {
        return Optional.ofNullable(sourceOf);
    }
}
