package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.bean.Kabuoji3ResultBean;
import github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.bean.NikkeiResultBean;
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
public class StockPrice {

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
    public static StockPrice ofNikkeiResultBean(
            final String code, final NikkeiResultBean nikkei, final LocalDateTime createdAt) {
        return new StockPrice(
                null,
                code,
                LocalDate.parse(nikkei.getTargetDate(), DateTimeFormatter.ofPattern("yyyy/M/d")),
                Parser.parseDoubleStock(nikkei.getStockPrice()).orElse(null),
                Parser.parseDoubleStock(nikkei.getOpeningPrice()).orElse(null),
                Parser.parseDoubleStock(nikkei.getHighPrice()).orElse(null),
                Parser.parseDoubleStock(nikkei.getLowPrice()).orElse(null),
                Parser.parseIntegerStock(nikkei.getVolume()).orElse(null),
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
    public static StockPrice ofKabuoji3ResultBean(
            final String code, final Kabuoji3ResultBean kabuoji3, final LocalDateTime createdAt) {
        return new StockPrice(
                null,
                code,
                LocalDate.parse(kabuoji3.getTargetDate()),
                Parser.parseDoubleStock(kabuoji3.getClosingPrice()).orElse(null),
                Parser.parseDoubleStock(kabuoji3.getOpeningPrice()).orElse(null),
                Parser.parseDoubleStock(kabuoji3.getHighPrice()).orElse(null),
                Parser.parseDoubleStock(kabuoji3.getLowPrice()).orElse(null),
                Parser.parseIntegerStock(kabuoji3.getVolume()).orElse(null),
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
     * データベースに登録されている株価情報から銘柄詳細画面表示するためにマッピングする
     *
     * @param stockPrice データベースに登録されている株価情報から
     * @return StockPrice
     */
    public static StockPrice ofBrandDetail(final StockPrice stockPrice) {
        return new StockPrice(
                null,
                null,
                stockPrice.getTargetDate(),
                stockPrice.getStockPrice(),
                stockPrice.getOpeningPrice(),
                stockPrice.getHighPrice(),
                stockPrice.getLowPrice(),
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
