package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.client.jsoup.result.NikkeiResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.StockPriceResultBean;
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
import java.time.format.DateTimeParseException;
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
     * @throws DateTimeParseException 対象日をパースできなかったとき
     */
    public static StockPriceEntity ofNikkeiResultBean(
            final String code, final NikkeiResultBean nikkei, final LocalDateTime createdAt) throws DateTimeParseException {
        return ofNikkeiResultBean(
                code,
                Parser.parseDoubleNikkei(nikkei.stockPrice()).orElse(null),
                nikkei,
                createdAt
        );
    }

    /**
     * 日経のスクレイピング結果からデータベース登録するためにマッピングする
     *
     * @param code       会社コード
     * @param stockPrice 株価終値
     * @param nikkei     日経スクレイピング結果
     * @param createdAt  登録日
     * @return StockPrice
     * @throws DateTimeParseException 対象日をパースできなかったとき
     */
    public static StockPriceEntity ofNikkeiResultBean(
            final String code,
            final Double stockPrice,
            final NikkeiResultBean nikkei,
            final LocalDateTime createdAt) throws DateTimeParseException {
        return new StockPriceEntity(
                null,
                code,
                LocalDate.parse(nikkei.targetDate(), DateTimeFormatter.ofPattern("yyyy/M/d")),
                stockPrice,
                Parser.parseDoubleNikkei(nikkei.openingPrice()).orElse(null),
                Parser.parseDoubleNikkei(nikkei.highPrice()).orElse(null),
                Parser.parseDoubleNikkei(nikkei.lowPrice()).orElse(null),
                Parser.parseIntegerVolume(nikkei.volume()).orElse(null),
                Parser.parseStringNikkei(nikkei.per()),
                Parser.parseStringNikkei(nikkei.pbr()),
                Parser.parseStringNikkei(nikkei.roe()),
                Parser.parseStringNikkei(nikkei.numberOfShares()),
                Parser.parseStringNikkei(nikkei.marketCapitalization()),
                Parser.parseStringNikkei(nikkei.dividendYield()),
                Optional.ofNullable(nikkei.shareholderBenefit()).map(v -> v.replace("株主優待 ", "")).orElse(null),
                SourceOfStockPrice.NIKKEI.toValue(),
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
     * @throws DateTimeParseException 対象日をパースできなかったとき
     */
    public static StockPriceEntity ofKabuoji3(
            final String code,
            final StockPriceResultBean kabuoji3,
            final LocalDateTime createdAt) throws DateTimeParseException {
        return new StockPriceEntity(
                null,
                code,
                LocalDate.parse(kabuoji3.targetDate()),
                Parser.parseDoubleKabuoji3(kabuoji3.closingPrice()).orElse(null),
                Parser.parseDoubleKabuoji3(kabuoji3.openingPrice()).orElse(null),
                Parser.parseDoubleKabuoji3(kabuoji3.highPrice()).orElse(null),
                Parser.parseDoubleKabuoji3(kabuoji3.lowPrice()).orElse(null),
                Parser.parseIntegerVolume(kabuoji3.volume()).orElse(null),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SourceOfStockPrice.KABUOJI3.toValue(),
                createdAt
        );
    }

    /**
     * みんかぶのスクレイピング結果からデータベース登録するためにマッピングする
     *
     * @param code      会社コード
     * @param minkabu   minkabuのスクレイピング結果
     * @param createdAt 登録日
     * @return StockPrice
     * @throws DateTimeParseException 対象日をパースできなかったとき
     */
    public static StockPriceEntity ofMinkabu(
            final String code,
            final StockPriceResultBean minkabu,
            final LocalDateTime createdAt) throws DateTimeParseException {
        return new StockPriceEntity(
                null,
                code,
                LocalDate.parse(minkabu.targetDate(), DateTimeFormatter.ofPattern("uuuu/MM/dd")),
                Parser.parseDoubleYahooFinance(minkabu.closingPrice()).orElse(null),
                Parser.parseDoubleYahooFinance(minkabu.openingPrice()).orElse(null),
                Parser.parseDoubleYahooFinance(minkabu.highPrice()).orElse(null),
                Parser.parseDoubleYahooFinance(minkabu.lowPrice()).orElse(null),
                Parser.parseIntegerVolume(minkabu.volume()).orElse(null),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SourceOfStockPrice.MINKABU.toValue(),
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
     * @throws DateTimeParseException 対象日をパースできなかったとき
     */
    public static StockPriceEntity ofYahooFinanceResultBean(
            final String code,
            final StockPriceResultBean yahooFinance,
            final LocalDateTime createdAt) throws DateTimeParseException {
        return new StockPriceEntity(
                null,
                code,
                LocalDate.parse(yahooFinance.targetDate(), DateTimeFormatter.ofPattern("yyyy年M月d日")),
                Parser.parseDoubleYahooFinance(yahooFinance.closingPrice()).orElse(null),
                Parser.parseDoubleYahooFinance(yahooFinance.openingPrice()).orElse(null),
                Parser.parseDoubleYahooFinance(yahooFinance.highPrice()).orElse(null),
                Parser.parseDoubleYahooFinance(yahooFinance.lowPrice()).orElse(null),
                Parser.parseIntegerVolume(yahooFinance.volume()).orElse(null),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SourceOfStockPrice.YAHOO_FINANCE.toValue(),
                createdAt
        );
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
}
