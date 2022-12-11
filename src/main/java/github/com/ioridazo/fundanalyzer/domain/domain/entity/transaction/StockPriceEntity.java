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
    public static StockPriceEntity ofKabuoji3(
            final String code, final StockPriceResultBean kabuoji3, final LocalDateTime createdAt) {
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
                "2",
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
     */
    public static StockPriceEntity ofMinkabu(
            final String code, final StockPriceResultBean minkabu, final LocalDateTime createdAt) {
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
                "4",
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
            final String code, final StockPriceResultBean yahooFinance, final LocalDateTime createdAt) {
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
                "3",
                createdAt
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

    @SuppressWarnings("unused")
    public Optional<String> getSourceOf() {
        return Optional.ofNullable(sourceOf);
    }
}
