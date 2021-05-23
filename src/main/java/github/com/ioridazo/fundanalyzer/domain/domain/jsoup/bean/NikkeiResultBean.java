package github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean;

import lombok.Value;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class NikkeiResultBean {

    private final String stockPrice;

    private final String targetDate;

    private final String openingPrice;

    private final String highPrice;

    private final String lowPrice;

    private final String volume;

    private final String per;

    private final String pbr;

    private final String roe;

    private final String numberOfShares;

    private final String marketCapitalization;

    private final String dividendYield;

    private final String shareholderBenefit;

    public static NikkeiResultBean ofJsoup(final Document document) {
        return new NikkeiResultBean(
                document.select(".m-stockPriceElm dd").first().text(),
                document.select(".m-stockInfo_date").first().text(),
                document.select(".m-stockInfo_detail_left li").stream()
                        .filter(e -> e.text().contains("始値")).map(Element::text).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .filter(e -> e.text().contains("高値")).map(Element::text).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .filter(e -> e.text().contains("安値")).map(Element::text).findAny().orElse(null),
                document.select(".m-stockInfo_detail_right li").stream()
                        .filter(e -> e.text().contains("売買高")).map(Element::text).findAny().orElse(null),
                document.select(".m-stockInfo_detail_right li").stream()
                        .filter(e -> e.text().contains("PER")).map(Element::text).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .filter(e -> e.text().contains("PBR")).map(Element::text).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .filter(e -> e.text().contains("ROE")).map(Element::text).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .filter(e -> e.text().contains("普通株式数")).map(Element::text).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .filter(e -> e.text().contains("時価総額")).map(Element::text).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .filter(e -> e.text().contains("株式益回り")).map(Element::text).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .filter(e -> e.text().contains("株主優待")).map(Element::text).findAny().orElse(null)
        );
    }
}
