package github.com.ioridazo.fundanalyzer.client.jsoup.result;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public record NikkeiResultBean(

        String stockPrice,

        String targetDate,

        String openingPrice,

        String highPrice,

        String lowPrice,

        String volume,

        String per,

        String pbr,

        String roe,

        String numberOfShares,

        String marketCapitalization,

        String dividendYield,

        String shareholderBenefit
) {

    public static NikkeiResultBean ofJsoup(final Document document) {
        return new NikkeiResultBean(
                document.select(".m-stockPriceElm dd").stream()
                        .map(Element::text)
                        .findFirst()
                        .map(NikkeiResultBean::parse).orElse(null),
                document.select(".m-stockInfo_date").stream()
                        .map(Element::text)
                        .findFirst()
                        .map(NikkeiResultBean::parse).orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text)
                        .filter(text -> text.contains("始値"))
                        .findAny()
                        .map(NikkeiResultBean::parse).orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text)
                        .filter(text -> text.contains("高値"))
                        .findAny()
                        .map(NikkeiResultBean::parse).orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text)
                        .filter(text -> text.contains("安値"))
                        .findAny()
                        .map(NikkeiResultBean::parse).orElse(null),
                document.select(".m-stockInfo_detail_right li").stream()
                        .map(Element::text)
                        .filter(text -> text.contains("売買高"))
                        .findAny()
                        .map(NikkeiResultBean::parse).orElse(null),
                document.select(".m-stockInfo_detail_right li").stream()
                        .map(Element::text)
                        .filter(text -> text.contains("PER"))
                        .findAny()
                        .map(NikkeiResultBean::parse).orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text)
                        .filter(text -> text.contains("PBR"))
                        .findAny()
                        .map(NikkeiResultBean::parse).orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text)
                        .filter(text -> text.contains("ROE"))
                        .findAny()
                        .map(NikkeiResultBean::parse).orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text)
                        .filter(text -> text.contains("普通株式数"))
                        .findAny()
                        .map(NikkeiResultBean::parse).orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text)
                        .filter(text -> text.contains("時価総額"))
                        .findAny()
                        .map(NikkeiResultBean::parse).orElse(null),
                document.select(".m-stockInfo_detail_right li").stream()
                        .map(Element::text)
                        .filter(text -> text.contains("予想配当利回り"))
                        .findAny()
                        .map(NikkeiResultBean::parse).orElse(null),
                document.select(".m-stockInfo_detail_left li").stream()
                        .map(Element::text)
                        .filter(text -> text.contains("株主優待"))
                        .findAny()
                        .map(NikkeiResultBean::parse).orElse(null)
        );
    }

    private static String parse(final String value) {
        if ("N/A".equals(value)) {
            return null;
        } else {
            return value;
        }
    }
}
