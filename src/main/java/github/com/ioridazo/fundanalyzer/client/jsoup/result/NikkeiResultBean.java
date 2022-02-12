package github.com.ioridazo.fundanalyzer.client.jsoup.result;

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
                document.select(".m-stockPriceElm dd").stream().map(Element::text).findFirst().orElse(null),
                document.select(".m-stockInfo_date").stream().map(Element::text).findFirst().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text).filter(text -> text.contains("始値")).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text).filter(text -> text.contains("高値")).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text).filter(text -> text.contains("安値")).findAny().orElse(null),
                document.select(".m-stockInfo_detail_right li").stream()
                        .map(Element::text).filter(text -> text.contains("売買高")).findAny().orElse(null),
                document.select(".m-stockInfo_detail_right li").stream()
                        .map(Element::text).filter(text -> text.contains("PER")).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text).filter(text -> text.contains("PBR")).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text).filter(text -> text.contains("ROE")).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text).filter(text -> text.contains("普通株式数")).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text).filter(text -> text.contains("時価総額")).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text).filter(text -> text.contains("株式益回り")).findAny().orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text).filter(text -> text.contains("株主優待")).findAny().orElse(null)
        );
    }
}
